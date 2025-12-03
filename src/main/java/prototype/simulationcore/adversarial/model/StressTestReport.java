package prototype.simulationcore.adversarial.model;

import java.time.Instant;
import java.util.List;

/**
 * Aggregated output from running adversarial stress testing.
 */
public record StressTestReport(String simulationId,
                               int executedIterations,
                               double survivalRate,
                               double averageEnergyDelta,
                               double averageResourceDelta,
                               double averageFitnessDelta,
                               Instant generatedAt,
                               List<ScenarioRunSummary> scenarioRuns,
                               List<EnvironmentPerturbationRecord> perturbations) {

    public StressTestReport {
        scenarioRuns = scenarioRuns == null ? List.of() : List.copyOf(scenarioRuns);
        perturbations = perturbations == null ? List.of() : List.copyOf(perturbations);
    }
}


