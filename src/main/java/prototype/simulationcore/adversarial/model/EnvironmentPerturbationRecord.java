package prototype.simulationcore.adversarial.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit of a single environment perturbation.
 */
public record EnvironmentPerturbationRecord(UUID perturbationId,
                                            String simulationId,
                                            String scenarioType,
                                            double severity,
                                            long tickApplied,
                                            Instant appliedAt,
                                            Map<String, Double> baselineSensors,
                                            Map<String, Double> perturbedSensors,
                                            Map<String, Double> deltas,
                                            PerformanceImpact performanceImpact) {

    public EnvironmentPerturbationRecord {
        baselineSensors = baselineSensors == null ? Map.of() : Map.copyOf(baselineSensors);
        perturbedSensors = perturbedSensors == null ? Map.of() : Map.copyOf(perturbedSensors);
        deltas = deltas == null ? Map.of() : Map.copyOf(deltas);
    }

    @JsonIgnore
    public boolean hasPerformanceImpact() {
        return performanceImpact != null;
    }

    public EnvironmentPerturbationRecord withImpact(PerformanceImpact impact) {
        return new EnvironmentPerturbationRecord(perturbationId, simulationId, scenarioType, severity,
                tickApplied, appliedAt, baselineSensors, perturbedSensors, deltas, impact);
    }
}


