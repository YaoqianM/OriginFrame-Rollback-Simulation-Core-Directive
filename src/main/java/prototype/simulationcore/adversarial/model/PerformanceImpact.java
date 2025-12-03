package prototype.simulationcore.adversarial.model;

/**
 * Captures how an adversarial perturbation influenced the agent's performance.
 */
public record PerformanceImpact(double energyDelta,
                                double resourceDelta,
                                double fitnessDelta,
                                boolean survived) {
}


