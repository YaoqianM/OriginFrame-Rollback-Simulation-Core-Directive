package prototype.simulationcore.policy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyEnumerated;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.Environment;

/**
 * Scores each action via a weight matrix and chooses the highest score.
 */
@Entity
@DiscriminatorValue("WEIGHTED")
public class WeightedPolicy extends AbstractAgentPolicy {

    @ElementCollection
    @CollectionTable(name = "weighted_policy_matrix", joinColumns = @JoinColumn(name = "policy_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "action_weight")
    private Map<Action, Double> actionWeights = new HashMap<>();

    @Override
    public Action decide(AgentState state, Environment environment) {
        return actionWeights.entrySet().stream()
                .max(Comparator.comparingDouble(entry -> contextualize(entry.getKey(), entry.getValue(), state)))
                .map(Map.Entry::getKey)
                .orElse(Action.WAIT);
    }

    private double contextualize(Action action, double baseWeight, AgentState state) {
        double modifier = switch (action) {
            case MOVE -> state == null ? 0.0 : state.energy() * 0.01;
            case CONSUME -> state == null ? 0.0 : Math.max(0.0, 100.0 - state.resources());
            case INTERACT -> parameterOrDefault("interaction_bias", 0.0);
            case REPLICATE -> parameterOrDefault("replication_bias", -5.0);
            case WAIT, REST -> parameterOrDefault("rest_bias", 0.0);
        };
        return baseWeight + modifier;
    }

    public Map<Action, Double> getActionWeights() {
        return Map.copyOf(actionWeights);
    }

    public void setActionWeights(Map<Action, Double> weights) {
        actionWeights.clear();
        if (weights != null) {
            actionWeights.putAll(weights);
        }
    }
}


