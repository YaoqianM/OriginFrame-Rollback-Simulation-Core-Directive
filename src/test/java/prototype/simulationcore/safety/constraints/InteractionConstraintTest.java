package prototype.simulationcore.safety.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.ValidationResult;

class InteractionConstraintTest {

    private InteractionConstraint constraint;

    @BeforeEach
    void setUp() {
        SafetyProperties properties = new SafetyProperties();
        SafetyProperties.Interaction interaction = new SafetyProperties.Interaction();
        interaction.setRequiredSignalKey("neighbors");
        interaction.setMinSignalValue(1.0);
        interaction.setMinimumGeneration(2);
        properties.setInteraction(interaction);
        constraint = new InteractionConstraint(properties);
    }

    @Test
    void bypassesChecksForNonInteractionActions() {
        Agent agent = new Agent();
        agent.setGeneration(0);
        agent.setState(AgentState.initial());

        ValidationResult result = constraint.validate(agent, Action.MOVE, null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void failsWhenSignalBelowThreshold() {
        Agent agent = new Agent();
        agent.setGeneration(5);
        AgentState state = AgentState.initial().withSensorReading("neighbors", 0.2);
        agent.setState(state);
        Environment environment = new DefaultEnvironment(state);

        ValidationResult result = constraint.validate(agent, Action.INTERACT, environment);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("proximity");
    }

    @Test
    void failsWhenGenerationTooLow() {
        Agent agent = new Agent();
        agent.setGeneration(0);
        AgentState state = AgentState.initial().withSensorReading("neighbors", 5.0);
        agent.setState(state);
        Environment environment = new DefaultEnvironment(state);

        ValidationResult result = constraint.validate(agent, Action.INTERACT, environment);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("generation");
    }

    @Test
    void passesWhenSignalAndGenerationRequirementsMet() {
        Agent agent = new Agent();
        agent.setGeneration(3);
        AgentState state = AgentState.initial().withSensorReading("neighbors", 3.0);
        agent.setState(state);
        Environment environment = new DefaultEnvironment(
                state.position(),
                Map.of("neighbors", 3.0, "threat", 0.0)
        );

        ValidationResult result = constraint.validate(agent, Action.INTERACT, environment);

        assertThat(result.valid()).isTrue();
    }
}

