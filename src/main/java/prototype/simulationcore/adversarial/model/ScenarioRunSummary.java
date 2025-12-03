package prototype.simulationcore.adversarial.model;

import java.util.UUID;

/**
 * Describes the outcome of a single adversarial scenario execution during stress testing.
 */
public record ScenarioRunSummary(UUID scenarioId,
                                 String scenarioType,
                                 double severity,
                                 double energyDelta,
                                 double resourceDelta,
                                 double fitnessDelta,
                                 boolean survived) {
}


