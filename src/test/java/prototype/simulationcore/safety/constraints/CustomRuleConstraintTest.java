package prototype.simulationcore.safety.constraints;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

class CustomRuleConstraintTest {

    private CustomRuleConstraint constraint;
    private Agent agent;

    @BeforeEach
    void setUp() {
        SafetyProperties properties = new SafetyProperties();
        SafetyProperties.CustomRule energyRule = new SafetyProperties.CustomRule();
        energyRule.setId("energy-floor");
        energyRule.setSeverity(prototype.simulationcore.safety.Severity.VIOLATION);
        energyRule.setRule("{\"metric\":\"energy\",\"operator\":\">=\",\"threshold\":25}");

        SafetyProperties.CustomRule toxicityRule = new SafetyProperties.CustomRule();
        toxicityRule.setId("toxicity-cap");
        toxicityRule.setSeverity(prototype.simulationcore.safety.Severity.CRITICAL);
        toxicityRule.setRule("{\"metric\":\"sensor.toxicity\",\"operator\":\"<\",\"threshold\":75}");

        properties.setCustomRules(List.of(energyRule, toxicityRule));
        constraint = new CustomRuleConstraint(properties, new ObjectMapper());

        agent = new Agent();
        agent.setGeneration(5);
    }

    @Test
    void failsWhenEnergyFallsBelowFloor() {
        AgentState state = new AgentState(null, 10.0, 0.0, Map.of(), Map.of());
        agent.setState(state);
        Environment environment = new DefaultEnvironment(state);

        ValidationResult result = constraint.validate(agent, Action.MOVE, environment);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("energy-floor");
    }

    @Test
    void failsWhenSensorMetricViolatesRule() {
        AgentState state = AgentState.initial().withSensorReading("toxicity", 99.0);
        agent.setState(state);
        Environment environment = new DefaultEnvironment(state);

        ValidationResult result = constraint.validate(agent, Action.MOVE, environment);

        assertThat(result.valid()).isFalse();
        assertThat(result.message()).contains("toxicity-cap");
    }

    @Test
    void passesWhenAllRulesSatisfied() {
        AgentState state = AgentState.initial()
                .adjustEnergy(50.0)
                .withSensorReading("toxicity", 10.0);
        agent.setState(state);
        Environment environment = new DefaultEnvironment(state);

        ValidationResult result = constraint.validate(agent, Action.MOVE, environment);

        assertThat(result.valid()).isTrue();
    }
}

