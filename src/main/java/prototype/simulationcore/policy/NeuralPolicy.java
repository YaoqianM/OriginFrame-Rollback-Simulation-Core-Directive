package prototype.simulationcore.policy;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.concurrent.ThreadLocalRandom;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.Environment;

/**
 * Placeholder policy intended for future RL integration.
 */
@Entity
@DiscriminatorValue("NEURAL")
public class NeuralPolicy extends AbstractAgentPolicy {

    @Column(name = "model_ref")
    private String modelReference;

    @Override
    public Action decide(AgentState state, Environment environment) {
        // Placeholder: randomly explore while the RL backend is integrated.
        Action[] options = Action.values();
        return options[ThreadLocalRandom.current().nextInt(options.length)];
    }

    public String getModelReference() {
        return modelReference;
    }

    public void setModelReference(String modelReference) {
        this.modelReference = modelReference;
    }
}


