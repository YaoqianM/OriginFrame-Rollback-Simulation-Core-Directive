package prototype.simulationcore.safety.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.ValidationResult;

class BoundaryConstraintTest {

    private BoundaryConstraint constraint;

    @BeforeEach
    void setUp() {
        SafetyProperties properties = new SafetyProperties();
        SafetyProperties.Boundary boundary = new SafetyProperties.Boundary();
        boundary.setMinX(-10);
        boundary.setMaxX(10);
        boundary.setMinY(-10);
        boundary.setMaxY(10);
        boundary.setMinZ(-5);
        boundary.setMaxZ(5);
        properties.setBoundary(boundary);
        constraint = new BoundaryConstraint(properties);
    }

    @Test
    void passesWhenAgentWithinConfiguredBounds() {
        Agent agent = new Agent();
        agent.setState(new AgentState(new Position(0, 0, 0), 50, 20, null, null));

        ValidationResult result = constraint.validate(agent, null, null);

        assertThat(result.valid()).isTrue();
    }

    @Test
    void failsWhenAgentLeavesBoundary() {
        Agent agent = new Agent();
        agent.setState(new AgentState(new Position(15, 0, 0), 50, 20, null, null));

        ValidationResult result = constraint.validate(agent, null, null);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("outside");
        assertThat(result.context()).containsKeys("x", "bounds");
    }
}

