package prototype.simulationcore.adversarial.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.adversarial.ScenarioApplicationResult;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.Environment;

/**
 * Coordinates scenario injection, scheduling, and recording.
 */
@Service
public class ScenarioInjector {

    public static final String DEFAULT_SIMULATION_ID = "default-simulation";

    private final EnvironmentPerturbationRecorder recorder;
    private final ConcurrentHashMap<String, SimulationScenarioContext> contexts = new ConcurrentHashMap<>();

    public ScenarioInjector(EnvironmentPerturbationRecorder recorder) {
        this.recorder = recorder;
    }

    public UUID injectScenario(String simulationId, AdversarialScenario scenario) {
        SimulationScenarioContext context = resolveContext(simulationId);
        ActiveScenario activeScenario = new ActiveScenario(UUID.randomUUID(), scenario);
        context.activeScenarios.put(activeScenario.scenarioId(), activeScenario);
        return activeScenario.scenarioId();
    }

    public UUID scheduleScenario(String simulationId, AdversarialScenario scenario, long tickNumber) {
        SimulationScenarioContext context = resolveContext(simulationId);
        ActiveScenario upcoming = new ActiveScenario(UUID.randomUUID(), scenario);
        if (tickNumber <= context.currentTick.get()) {
            context.activeScenarios.put(upcoming.scenarioId(), upcoming);
        } else {
            context.scheduledScenarios.computeIfAbsent(tickNumber, key -> new CopyOnWriteArrayList<>()).add(upcoming);
        }
        return upcoming.scenarioId();
    }

    public Optional<UUID> randomScenarioInjection(double probability, List<AdversarialScenario> scenarioPool) {
        return randomScenarioInjection(DEFAULT_SIMULATION_ID, probability, scenarioPool);
    }

    public Optional<UUID> randomScenarioInjection(String simulationId,
                                                  double probability,
                                                  List<AdversarialScenario> scenarioPool) {
        if (scenarioPool == null || scenarioPool.isEmpty()) {
            return Optional.empty();
        }
        double normalizedProbability = Math.max(0.0, Math.min(1.0, probability));
        if (ThreadLocalRandom.current().nextDouble() > normalizedProbability) {
            return Optional.empty();
        }
        AdversarialScenario scenario = scenarioPool.get(ThreadLocalRandom.current().nextInt(scenarioPool.size()));
        return Optional.of(injectScenario(simulationId, scenario));
    }

    public boolean removeScenario(String simulationId, UUID scenarioId) {
        SimulationScenarioContext context = contexts.get(resolveId(simulationId));
        if (context == null || scenarioId == null) {
            return false;
        }
        boolean removed = context.activeScenarios.remove(scenarioId) != null;
        if (!context.scheduledScenarios.isEmpty()) {
            for (Map.Entry<Long, List<ActiveScenario>> entry : context.scheduledScenarios.entrySet()) {
                List<ActiveScenario> scenarios = entry.getValue();
                if (scenarios.removeIf(s -> s.scenarioId().equals(scenarioId))) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    public ScenarioApplicationResult applyActiveScenarios(String simulationId,
                                                          Environment baseEnvironment) {
        SimulationScenarioContext context = contexts.get(resolveId(simulationId));
        if (context == null || baseEnvironment == null) {
            return ScenarioApplicationResult.noop(baseEnvironment);
        }

        long tick = context.currentTick.incrementAndGet();
        context.activateScheduled(tick);

        if (context.activeScenarios.isEmpty()) {
            return ScenarioApplicationResult.noop(baseEnvironment);
        }

        Environment environment = baseEnvironment;
        List<UUID> recordIds = new ArrayList<>();

        for (ActiveScenario activeScenario : context.activeScenarios.values()) {
            Map<String, Double> baseline = Map.copyOf(environment.snapshotSensors());
            environment = activeScenario.scenario().apply(environment);
            Map<String, Double> perturbed = Map.copyOf(environment.snapshotSensors());
            UUID recordId = recorder.recordPerturbation(resolveId(simulationId), activeScenario.scenario(), tick, baseline, perturbed);
            recordIds.add(recordId);
        }

        return new ScenarioApplicationResult(environment, recordIds);
    }

    public void recordPerformanceImpact(String simulationId,
                                        Collection<UUID> perturbationIds,
                                        AgentState previous,
                                        AgentState updated,
                                        double fitnessDelta) {
        if (previous == null || updated == null) {
            return;
        }
        recorder.recordPerformanceImpact(resolveId(simulationId), perturbationIds, previous, updated, fitnessDelta);
    }

    private SimulationScenarioContext resolveContext(String simulationId) {
        return contexts.computeIfAbsent(resolveId(simulationId), key -> new SimulationScenarioContext());
    }

    private String resolveId(String simulationId) {
        if (simulationId == null || simulationId.isBlank()) {
            return DEFAULT_SIMULATION_ID;
        }
        return simulationId;
    }

    private static final class SimulationScenarioContext {

        private final AtomicLong currentTick = new AtomicLong();
        private final ConcurrentHashMap<UUID, ActiveScenario> activeScenarios = new ConcurrentHashMap<>();
        private final ConcurrentSkipListMap<Long, List<ActiveScenario>> scheduledScenarios = new ConcurrentSkipListMap<>();

        private void activateScheduled(long tick) {
            if (scheduledScenarios.isEmpty()) {
                return;
            }
            List<Long> dueTicks = scheduledScenarios.headMap(tick + 1, true)
                    .keySet().stream().toList();
            for (Long dueTick : dueTicks) {
                List<ActiveScenario> scheduled = scheduledScenarios.remove(dueTick);
                if (scheduled == null) {
                    continue;
                }
                for (ActiveScenario scenario : scheduled) {
                    activeScenarios.putIfAbsent(scenario.scenarioId(), scenario);
                }
            }
        }
    }

    private record ActiveScenario(UUID scenarioId, AdversarialScenario scenario) {
    }
}


