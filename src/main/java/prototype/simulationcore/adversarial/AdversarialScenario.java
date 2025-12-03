package prototype.simulationcore.adversarial;

import prototype.simulationcore.environment.Environment;

/**
 * Contract for adversarial perturbations that can be applied to the simulation environment.
 */
public interface AdversarialScenario {

    /**
     * Applies the adversarial transformation to the provided environment snapshot.
     *
     * @param environment immutable snapshot of the world
     * @return new environment representing the perturbed world view
     */
    Environment apply(Environment environment);

    /**
     * @return canonical type identifier for logging/reporting
     */
    String getScenarioType();

    /**
     * @return normalized severity between 0 (no impact) and 1 (max impact)
     */
    double getSeverity();
}


