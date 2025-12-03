package prototype.simulationcore.safety.constraints;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.SafetyConstraint;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.ValidationResult;

@Component
public class CustomRuleConstraint implements SafetyConstraint {

    private static final Logger log = LoggerFactory.getLogger(CustomRuleConstraint.class);
    private static final String TYPE = "CUSTOM_RULE_CONSTRAINT";

    private final List<RuleDefinition> rules;

    public CustomRuleConstraint(SafetyProperties properties, ObjectMapper objectMapper) {
        this.rules = buildRules(properties.getCustomRules(), objectMapper);
    }

    @Override
    public ValidationResult validate(Agent agent, Action action, Environment environment) {
        if (rules.isEmpty()) {
            return ValidationResult.passed(getConstraintType());
        }

        Environment resolved = environment == null
                ? new DefaultEnvironment(agent.getState())
                : environment;

        for (RuleDefinition rule : rules) {
            double metricValue = resolveMetric(rule.metric(), agent, resolved);
            if (!rule.evaluate(metricValue)) {
                return ValidationResult.failed(
                        getConstraintType(),
                        rule.severity(),
                        "Custom rule violated: " + rule.id(),
                        Map.of(
                                "metric", rule.metric(),
                                "value", metricValue,
                                "threshold", rule.threshold(),
                                "operator", rule.operator()
                        )
                );
            }
        }

        return ValidationResult.passed(getConstraintType());
    }

    @Override
    public String getConstraintType() {
        return TYPE;
    }

    @Override
    public Severity getSeverity() {
        return Severity.WARNING;
    }

    private List<RuleDefinition> buildRules(List<SafetyProperties.CustomRule> definitions,
                                           ObjectMapper objectMapper) {
        List<RuleDefinition> parsed = new ArrayList<>();
        for (SafetyProperties.CustomRule definition : definitions) {
            if (definition.getRule() == null || definition.getRule().isBlank()) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(definition.getRule());
                String metric = node.path("metric").asText(null);
                String operator = node.path("operator").asText(">");
                double threshold = node.path("threshold").asDouble(0.0);
                if (metric == null) {
                    log.warn("Skipping custom rule {} - missing metric", definition.getId());
                    continue;
                }
                parsed.add(new RuleDefinition(
                        definition.getId() == null ? metric : definition.getId(),
                        metric,
                        operator,
                        threshold,
                        definition.getSeverity() == null ? Severity.WARNING : definition.getSeverity()
                ));
            } catch (IOException e) {
                log.warn("Failed to parse custom safety rule {}: {}", definition.getId(), e.getMessage());
            }
        }
        return parsed;
    }

    private double resolveMetric(String metric, Agent agent, Environment environment) {
        String normalized = metric.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "energy" -> agent.getState().energy();
            case "resources" -> agent.getState().resources();
            case "generation" -> agent.getGeneration();
            case "position.x" -> agent.getState().position().x();
            case "position.y" -> agent.getState().position().y();
            case "position.z" -> agent.getState().position().z();
            default -> resolveDynamicMetric(normalized, agent, environment);
        };
    }

    private double resolveDynamicMetric(String metric, Agent agent, Environment environment) {
        if (metric.startsWith("sensor.")) {
            String key = metric.substring("sensor.".length());
            return environment.readSignal(key);
        }
        if (metric.startsWith("internal.")) {
            String key = metric.substring("internal.".length());
            return agent.getState().internalState().getOrDefault(key, 0.0);
        }
        if (metric.startsWith("env.")) {
            String key = metric.substring("env.".length());
            return environment.snapshotSensors().getOrDefault(key, 0.0);
        }
        return 0.0;
    }

    private record RuleDefinition(String id, String metric, String operator, double threshold, Severity severity) {

        boolean evaluate(double value) {
            return switch (operator) {
                case ">=", "gte" -> value >= threshold;
                case ">", "gt" -> value > threshold;
                case "<=", "lte" -> value <= threshold;
                case "<", "lt" -> value < threshold;
                case "==", "eq" -> Double.compare(value, threshold) == 0;
                case "!=", "ne" -> Double.compare(value, threshold) != 0;
                default -> true;
            };
        }
    }
}

