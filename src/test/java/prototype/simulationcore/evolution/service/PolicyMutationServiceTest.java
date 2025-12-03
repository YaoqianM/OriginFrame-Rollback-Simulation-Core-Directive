package prototype.simulationcore.evolution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.WeightedPolicy;
import prototype.simulationcore.repository.AgentPolicyRepository;

@ExtendWith(MockitoExtension.class)
class PolicyMutationServiceTest {

    @Mock
    private AgentPolicyRepository policyRepository;

    private PolicyMutationService mutationService;

    @BeforeEach
    void setUp() {
        mutationService = new PolicyMutationService(policyRepository);
    }

    @Test
    void replicateCopiesParametersAndWeights() {
        WeightedPolicy template = new WeightedPolicy();
        template.setParameters(Map.of("alpha", 0.5, "beta", -1.0));
        template.setActionWeights(Map.of(Action.MOVE, 2.0, Action.REST, 0.25));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AbstractAgentPolicy copy = mutationService.replicate(template);

        assertThat(copy)
                .isInstanceOf(WeightedPolicy.class)
                .isNotSameAs(template);
        WeightedPolicy weightedCopy = (WeightedPolicy) copy;
        assertThat(weightedCopy.getParameters()).isEqualTo(template.getParameters());
        assertThat(weightedCopy.getActionWeights()).isEqualTo(template.getActionWeights());
        verify(policyRepository).save(weightedCopy);
    }

    @Test
    void mutateRespectsMutationBoundsForWeightedPolicies() {
        WeightedPolicy parent = new WeightedPolicy();
        parent.setParameters(Map.of("alpha", 1.0, "bias", -0.4));
        parent.setActionWeights(Map.of(
                Action.MOVE, 3.5,
                Action.REST, 0.5,
                Action.REPLICATE, -2.0
        ));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AbstractAgentPolicy mutatedPolicy = mutationService.mutate(parent, 0.2);

        assertThat(mutatedPolicy).isInstanceOf(WeightedPolicy.class);
        WeightedPolicy mutated = (WeightedPolicy) mutatedPolicy;
        mutated.getParameters().forEach((key, value) -> {
            double delta = Math.abs(value - parent.getParameters().get(key));
            assertThat(delta).isLessThanOrEqualTo(0.2 + 1e-9);
        });
        mutated.getActionWeights().forEach((action, value) -> {
            double delta = Math.abs(value - parent.getActionWeights().get(action));
            assertThat(delta).isLessThanOrEqualTo(0.2 + 1e-9);
        });
        verify(policyRepository).save(mutated);
    }

    @Test
    void mutateWithZeroRateProducesClonedPolicy() {
        WeightedPolicy parent = new WeightedPolicy();
        parent.setParameters(Map.of("alpha", 1.0));
        parent.setActionWeights(Map.of(Action.MOVE, 2.0));
        when(policyRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AbstractAgentPolicy mutated = mutationService.mutate(parent, 0.0);

        assertThat(mutated.getParameters()).isEqualTo(parent.getParameters());
        assertThat(((WeightedPolicy) mutated).getActionWeights()).isEqualTo(parent.getActionWeights());
    }

    @Test
    void replicateRejectsNullTemplate() {
        assertThatThrownBy(() -> mutationService.replicate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template policy");
    }

    @Test
    void mutateRejectsNullParent() {
        assertThatThrownBy(() -> mutationService.mutate(null, 0.1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent policy");
    }
}

