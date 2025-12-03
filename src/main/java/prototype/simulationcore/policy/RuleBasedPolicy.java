package prototype.simulationcore.policy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.List;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.Environment;

/**
 * Simple if-then driven policy. The first matching rule wins.
 */
@Entity
@DiscriminatorValue("RULE_BASED")
public class RuleBasedPolicy extends AbstractAgentPolicy {

    @ElementCollection
    @CollectionTable(name = "rule_based_policy_rules", joinColumns = @JoinColumn(name = "policy_id"))
    private List<DecisionRule> rules = new ArrayList<>();

    @Override
    public Action decide(AgentState state, Environment environment) {
        return rules.stream()
                .filter(rule -> rule.matches(state, environment))
                .findFirst()
                .map(DecisionRule::action)
                .orElse(Action.WAIT);
    }

    public List<DecisionRule> getRules() {
        return List.copyOf(rules);
    }

    public void setRules(List<DecisionRule> rules) {
        this.rules = rules == null ? new ArrayList<>() : new ArrayList<>(rules);
    }

    public void addRule(DecisionRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
    }

    @Embeddable
    public static class DecisionRule {
        @Column(name = "sensor_key")
        private String sensorKey;

        @Column(name = "threshold_value")
        private double threshold;

        @Enumerated(EnumType.STRING)
        @Column(name = "action_outcome")
        private Action action = Action.WAIT;

        @Column(name = "greater_than", nullable = false)
        private boolean greaterThan = true;

        public DecisionRule() {
        }

        public DecisionRule(String sensorKey, double threshold, Action action, boolean greaterThan) {
            this.sensorKey = sensorKey;
            this.threshold = threshold;
            this.action = action == null ? Action.WAIT : action;
            this.greaterThan = greaterThan;
        }

        public boolean matches(AgentState state, Environment environment) {
            if (sensorKey == null || sensorKey.isBlank()) {
                return false;
            }
            double value;
            if (state != null && state.sensorReadings().containsKey(sensorKey)) {
                value = state.sensorReadings().getOrDefault(sensorKey, 0.0);
            } else if (environment != null) {
                value = environment.readSignal(sensorKey);
            } else {
                value = 0.0;
            }
            return greaterThan ? value >= threshold : value <= threshold;
        }

        public Action action() {
            return action;
        }

        public String sensorKey() {
            return sensorKey;
        }

        public double threshold() {
            return threshold;
        }

        public boolean greaterThan() {
            return greaterThan;
        }
    }
}


