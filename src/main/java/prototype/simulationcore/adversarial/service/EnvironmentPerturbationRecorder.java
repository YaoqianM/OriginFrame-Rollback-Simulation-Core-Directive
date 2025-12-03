package prototype.simulationcore.adversarial.service;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.adversarial.model.EnvironmentPerturbationRecord;
import prototype.simulationcore.adversarial.model.PerformanceImpact;
import prototype.simulationcore.domain.AgentState;

/**
 * In-memory recorder to correlate perturbations with performance changes.
 */
@Component
public class EnvironmentPerturbationRecorder {

    private final ConcurrentHashMap<UUID, EnvironmentPerturbationRecord> recordIndex = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<UUID>> simulationTimeline = new ConcurrentHashMap<>();

    public UUID recordPerturbation(String simulationId,
                                   AdversarialScenario scenario,
                                   long tick,
                                   Map<String, Double> baselineSensors,
                                   Map<String, Double> perturbedSensors) {
        UUID recordId = UUID.randomUUID();
        Map<String, Double> delta = computeDelta(baselineSensors, perturbedSensors);
        EnvironmentPerturbationRecord record = new EnvironmentPerturbationRecord(
                recordId,
                simulationId,
                scenario == null ? "unknown" : scenario.getScenarioType(),
                scenario == null ? 0.0 : scenario.getSeverity(),
                tick,
                Instant.now(),
                baselineSensors,
                perturbedSensors,
                delta,
                null
        );
        recordIndex.put(recordId, record);
        simulationTimeline.computeIfAbsent(simulationId, key -> new CopyOnWriteArrayList<>()).add(recordId);
        return recordId;
    }

    public void recordPerformanceImpact(String simulationId,
                                        Collection<UUID> perturbationIds,
                                        AgentState previous,
                                        AgentState updated,
                                        double fitnessDelta) {
        if (perturbationIds == null || perturbationIds.isEmpty()) {
            return;
        }
        PerformanceImpact impact = new PerformanceImpact(
                updated.energy() - previous.energy(),
                updated.resources() - previous.resources(),
                fitnessDelta,
                updated.energy() > 0.0
        );

        for (UUID id : perturbationIds) {
            recordIndex.computeIfPresent(id, (key, record) -> record.withImpact(impact));
        }
    }

    public List<EnvironmentPerturbationRecord> export(String simulationId) {
        List<UUID> ids = simulationTimeline.getOrDefault(simulationId, new CopyOnWriteArrayList<>());
        return ids.stream()
                .map(recordIndex::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private Map<String, Double> computeDelta(Map<String, Double> baseline, Map<String, Double> perturbed) {
        Map<String, Double> delta = new HashMap<>();
        HashSet<String> keys = new HashSet<>();
        if (baseline != null) {
            keys.addAll(baseline.keySet());
        }
        if (perturbed != null) {
            keys.addAll(perturbed.keySet());
        }
        for (String key : keys) {
            double before = baseline == null ? 0.0 : baseline.getOrDefault(key, 0.0);
            double after = perturbed == null ? 0.0 : perturbed.getOrDefault(key, 0.0);
            double diff = after - before;
            if (Math.abs(diff) > 1e-9) {
                delta.put(key, diff);
            }
        }
        return delta;
    }
}


