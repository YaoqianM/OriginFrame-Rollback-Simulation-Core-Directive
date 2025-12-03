package prototype.simulationcore.safety.constraints;

import java.util.Map;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.SafetyConstraint;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.ValidationResult;

@Component
public class InteractionConstraint implements SafetyConstraint {

    private static final String TYPE = "INTERACTION_CONSTRAINT";

    private final SafetyProperties.Interaction interaction;

    public InteractionConstraint(SafetyProperties properties) {
        this.interaction = properties.getInteraction();
    }

    @Override
    public ValidationResult validate(Agent agent, Action action, Environment environment) {
        if (action != Action.INTERACT) {
            return ValidationResult.passed(getConstraintType());
        }

        Environment resolved = environment == null
                ? new DefaultEnvironment(agent.getState())
                : environment;

        double signal = resolved.readSignal(interaction.getRequiredSignalKey());
        if (signal < interaction.getMinSignalValue()) {
            return ValidationResult.failed(
                    getConstraintType(),
                    getSeverity(),
                    "Interaction attempted without sufficient proximity/handshake",
                    Map.of("signal", signal, "expected", interaction.getMinSignalValue())
            );
        }

        if (agent.getGeneration() < interaction.getMinimumGeneration()) {
            return ValidationResult.failed(
                    getConstraintType(),
                    getSeverity(),
                    "Agent generation too early for interactions",
                    Map.of("generation", agent.getGeneration(), "required", interaction.getMinimumGeneration())
            );
        }

        return ValidationResult.passed(getConstraintType());
    }

    @Override
    public String getConstraintType() {
        return TYPE;
    }

    @Override
    public Severity getSeverity() {
        return interaction.getSeverity();
    }
}

