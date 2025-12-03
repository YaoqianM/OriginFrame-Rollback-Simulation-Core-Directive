package prototype.simulationcore.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;

@ExtendWith(MockitoExtension.class)
class ConstraintValidatorTest {

    @Mock
    private SafetyConstraint safetyConstraint;

    @Mock
    private ViolationHandler violationHandler;

    private ConstraintValidator constraintValidator;

    @BeforeEach
    void setup() {
        constraintValidator = new ConstraintValidator(List.of(safetyConstraint), violationHandler);
    }

    @Test
    void preActionCheck_blocksCriticalViolations() {
        Agent agent = new Agent();
        agent.setState(AgentState.initial());
        Environment environment = new DefaultEnvironment(agent.getState());

        when(safetyConstraint.validate(any(), any(), any()))
                .thenReturn(ValidationResult.failed("TEST", Severity.CRITICAL, "boom", Map.of()));

        boolean allowed = constraintValidator.preActionCheck(agent, Action.MOVE, environment);

        assertThat(allowed).isFalse();
        verify(violationHandler).handleViolation(eq(agent), any(Violation.class), eq(environment));
    }
}

