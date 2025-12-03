package prototype.simulationcore.safety.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.ValidationResult;

class ResourceConstraintTest {

    private ResourceConstraint constraint;

    @BeforeEach
    void setUp() {
        SafetyProperties properties = new SafetyProperties();
        SafetyProperties.Resource resource = new SafetyProperties.Resource();
        resource.setMinEnergy(10.0);
        resource.setMinResources(5.0);
        resource.setMinimumEnergyForAction(Map.of(Action.REPLICATE, 40.0));
        resource.setMinimumResourcesForAction(Map.of(Action.REPLICATE, 25.0));
        properties.setResource(resource);
        constraint = new ResourceConstraint(properties);
    }

    @Test
    void passesWhenResourcesAreHealthy() {
        Agent agent = new Agent();
        agent.setState(new AgentState(null, 80.0, 50.0, null, null));

        ValidationResult result = constraint.validate(agent, Action.MOVE, null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void failsWhenEnergyBelowFloor() {
        Agent agent = new Agent();
        agent.setState(new AgentState(null, 5.0, 50.0, null, null));

        ValidationResult result = constraint.validate(agent, Action.MOVE, null);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("Energy level");
    }

    @Test
    void failsWhenActionSpecificRequirementNotMet() {
        Agent agent = new Agent();
        agent.setState(new AgentState(null, 30.0, 10.0, null, null));

        ValidationResult result = constraint.validate(agent, Action.REPLICATE, null);

        assertThat(result.valid()).isFalse();
        assertThat(result.context()).containsKeys("required", "action");
    }
}

