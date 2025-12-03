package prototype.simulationcore.service;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.RuleBasedPolicy;
import prototype.simulationcore.policy.RuleBasedPolicy.DecisionRule;
import prototype.simulationcore.policy.WeightedPolicy;
import prototype.simulationcore.repository.AgentPolicyRepository;

/**
 * Centralized helper that makes sure at least one agent policy exists in the database.
 */
@Component
public class AgentPolicyBootstrapper {

    private final AgentPolicyRepository policyRepository;

    public AgentPolicyBootstrapper(AgentPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public AbstractAgentPolicy resolveDefaultPolicy() {
        return policyRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(() -> policyRepository.save(seedPolicies()));
    }

    private AbstractAgentPolicy seedPolicies() {
        WeightedPolicy weightedPolicy = new WeightedPolicy();
        Map<Action, Double> weights = new HashMap<>();
        weights.put(Action.MOVE, 5.0);
        weights.put(Action.CONSUME, 3.0);
        weights.put(Action.INTERACT, 1.0);
        weights.put(Action.REPLICATE, -2.0);
        weights.put(Action.WAIT, 0.5);
        weights.put(Action.REST, 1.0);
        weightedPolicy.setActionWeights(weights);
        weightedPolicy.setParameters(Map.of(
                "interaction_bias", -1.0,
                "replication_bias", -5.0,
                "rest_bias", 0.5
        ));
        policyRepository.save(weightedPolicy);

        RuleBasedPolicy ruleBasedPolicy = new RuleBasedPolicy();
        ruleBasedPolicy.addRule(new DecisionRule("energy", 20.0, Action.REST, false));
        ruleBasedPolicy.addRule(new DecisionRule("resources", 75.0, Action.REPLICATE, true));
        ruleBasedPolicy.addRule(new DecisionRule("threat", 50.0, Action.MOVE, true));
        return ruleBasedPolicy;
    }
}


