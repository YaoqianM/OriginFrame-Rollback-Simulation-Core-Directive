package prototype.simulationcore.safety.constraints;

import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.ConstraintContext;
import prototype.simulationcore.safety.ConstraintPhase;
import prototype.simulationcore.safety.SafetyConstraint;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.ValidationResult;

@Component
public class RateConstraint implements SafetyConstraint {

    private static final String TYPE = "RATE_CONSTRAINT";

    private final Clock clock;
    private final Map<Action, SafetyProperties.RateLimitRule> limitsByAction;
    private final Map<UUID, Map<Action, Deque<Instant>>> agentHistory = new ConcurrentHashMap<>();

    public RateConstraint(SafetyProperties properties) {
        this(properties, Clock.systemUTC());
    }

    RateConstraint(SafetyProperties properties, Clock clock) {
        this.clock = clock;
        this.limitsByAction = properties.getRate().getLimits().stream()
                .collect(Collectors.toMap(SafetyProperties.RateLimitRule::getAction, rule -> rule,
                        (a, b) -> b, () -> new ConcurrentHashMap<>()));
    }

    @Override
    public ValidationResult validate(Agent agent, Action action, Environment environment) {
        SafetyProperties.RateLimitRule rule = limitsByAction.get(action);
        if (rule == null) {
            return ValidationResult.passed(getConstraintType());
        }

        ConstraintPhase phase = ConstraintContext.getPhase();
        if (phase == ConstraintPhase.POST_ACTION) {
            return ValidationResult.passed(getConstraintType());
        }

        UUID agentId = agent.getAgentId();
        if (agentId == null) {
            // bootstrap agents share the same rate limiter bucket
            agentId = UUID.nameUUIDFromBytes(("bootstrap-" + agent.hashCode()).getBytes());
        }

        Map<Action, Deque<Instant>> perAction = agentHistory.computeIfAbsent(agentId, ignored -> new ConcurrentHashMap<>());
        Deque<Instant> attempts = perAction.computeIfAbsent(action, ignored -> new ConcurrentLinkedDeque<>());

        Instant now = clock.instant();
        Instant lowerBound = now.minus(rule.getWindow());
        while (!attempts.isEmpty() && attempts.peekFirst().isBefore(lowerBound)) {
            attempts.pollFirst();
        }

        if (attempts.size() >= rule.getMaxAttempts()) {
            return ValidationResult.failed(
                    getConstraintType(),
                    rule.getSeverity(),
                    "Action rate limit exceeded",
                    Map.of(
                            "action", action.name(),
                            "attempts", attempts.size(),
                            "windowSeconds", rule.getWindow().toSeconds()
                    )
            );
        }

        attempts.addLast(now);
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
}

