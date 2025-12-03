package prototype.simulationcore.evolution.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.NeuralPolicy;
import prototype.simulationcore.policy.RuleBasedPolicy;
import prototype.simulationcore.policy.WeightedPolicy;
import prototype.simulationcore.repository.AgentPolicyRepository;

@Service
public class PolicyMutationService {

    private final AgentPolicyRepository policyRepository;

    public PolicyMutationService(AgentPolicyRepository policyRepository) {
        this.policyRepository = policyRepository;
    }

    public AbstractAgentPolicy replicate(AbstractAgentPolicy template) {
        if (template == null) {
            throw new IllegalArgumentException("Template policy cannot be null");
        }
        AbstractAgentPolicy clone = clonePolicy(template);
        clone.setParameters(new HashMap<>(template.getParameters()));
        return policyRepository.save(clone);
    }

    public AbstractAgentPolicy mutate(AbstractAgentPolicy parent, double mutationRate) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent policy cannot be null");
        }
        AbstractAgentPolicy mutated = clonePolicy(parent);
        mutateParameters(mutated, mutationRate);
        if (mutated instanceof WeightedPolicy weighted) {
            mutateWeights(weighted, mutationRate);
        }
        return policyRepository.save(mutated);
    }

    private AbstractAgentPolicy clonePolicy(AbstractAgentPolicy template) {
        if (template instanceof WeightedPolicy weighted) {
            WeightedPolicy copy = new WeightedPolicy();
            copy.setActionWeights(weighted.getActionWeights());
            copy.setParameters(weighted.getParameters());
            return copy;
        }
        if (template instanceof RuleBasedPolicy ruleBased) {
            RuleBasedPolicy copy = new RuleBasedPolicy();
            copy.setParameters(ruleBased.getParameters());
            copy.setRules(List.copyOf(ruleBased.getRules()));
            return copy;
        }
        if (template instanceof NeuralPolicy neural) {
            NeuralPolicy copy = new NeuralPolicy();
            copy.setParameters(neural.getParameters());
            copy.setModelReference(neural.getModelReference());
            return copy;
        }
        throw new IllegalArgumentException("Unsupported policy type: " + template.getClass().getSimpleName());
    }

    private void mutateParameters(AbstractAgentPolicy policy, double mutationRate) {
        Map<String, Double> mutated = new HashMap<>(policy.getParameters());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        mutated.replaceAll((key, value) -> value + randomDelta(random, mutationRate));
        policy.setParameters(mutated);
    }

    private void mutateWeights(WeightedPolicy policy, double mutationRate) {
        Map<Action, Double> mutatedWeights = new HashMap<>(policy.getActionWeights());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        mutatedWeights.replaceAll((action, weight) -> weight + randomDelta(random, mutationRate));
        policy.setActionWeights(mutatedWeights);
    }

    private double randomDelta(ThreadLocalRandom random, double mutationRate) {
        double boundedRate = Math.max(0.0, Math.min(1.0, mutationRate));
        return (random.nextDouble() * 2 - 1) * boundedRate;
    }
}


