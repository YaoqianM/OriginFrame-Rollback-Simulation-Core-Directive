package prototype.simulationcore.adversarial.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.adversarial.model.EnvironmentPerturbationRecord;
import prototype.simulationcore.adversarial.model.ScenarioRunSummary;
import prototype.simulationcore.adversarial.model.StressTestReport;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.service.SimulationService;

/**
 * Drives adversarial stress testing workflows.
 */
@Service
public class StressTestingService {

    private final SimulationService simulationService;
    private final ScenarioInjector scenarioInjector;
    private final EnvironmentPerturbationRecorder perturbationRecorder;
    private final AdversarialScenarioFactory scenarioFactory;
    private final ConcurrentHashMap<String, StressTestReport> latestReports = new ConcurrentHashMap<>();

    public StressTestingService(SimulationService simulationService,
                                ScenarioInjector scenarioInjector,
                                EnvironmentPerturbationRecorder perturbationRecorder,
                                AdversarialScenarioFactory scenarioFactory) {
        this.simulationService = simulationService;
        this.scenarioInjector = scenarioInjector;
        this.perturbationRecorder = perturbationRecorder;
        this.scenarioFactory = scenarioFactory;
    }

    public StressTestReport runStressTest(String simulationId, int iterations, List<AdversarialScenario> scenarioPool) {
        String resolvedId = resolveId(simulationId);
        List<AdversarialScenario> pool = (scenarioPool == null || scenarioPool.isEmpty())
                ? scenarioFactory.defaultScenarioPool()
                : scenarioPool;

        if (iterations <= 0) {
            return snapshot(resolvedId, List.of());
        }

        List<ScenarioRunSummary> runs = new ArrayList<>();
        for (int i = 0; i < iterations; i++) {
            AdversarialScenario scenario = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            Agent agentBefore = simulationService.currentAgent();
            AgentState previous = agentBefore.snapshotState();
            double fitnessBefore = agentBefore.getFitness();

            UUID scenarioId = scenarioInjector.injectScenario(resolvedId, scenario);
            Agent agentAfter = simulationService.step();
            AgentState updated = agentAfter.snapshotState();
            double fitnessAfter = agentAfter.getFitness();

            runs.add(new ScenarioRunSummary(
                    scenarioId,
                    scenario.getScenarioType(),
                    scenario.getSeverity(),
                    updated.energy() - previous.energy(),
                    updated.resources() - previous.resources(),
                    fitnessAfter - fitnessBefore,
                    updated.energy() > 0.0
            ));
            scenarioInjector.removeScenario(resolvedId, scenarioId);
        }

        StressTestReport report = snapshot(resolvedId, runs);
        latestReports.put(resolvedId, report);
        return report;
    }

    public StressTestReport latestReport(String simulationId) {
        String resolvedId = resolveId(simulationId);
        return latestReports.getOrDefault(resolvedId, snapshot(resolvedId, List.of()));
    }

    private StressTestReport snapshot(String simulationId, List<ScenarioRunSummary> runs) {
        int count = runs.size();
        double survivalRate = count == 0 ? 1.0 :
                runs.stream().filter(ScenarioRunSummary::survived).count() / (double) count;
        double avgEnergy = count == 0 ? 0.0 :
                runs.stream().mapToDouble(ScenarioRunSummary::energyDelta).average().orElse(0.0);
        double avgResources = count == 0 ? 0.0 :
                runs.stream().mapToDouble(ScenarioRunSummary::resourceDelta).average().orElse(0.0);
        double avgFitness = count == 0 ? 0.0 :
                runs.stream().mapToDouble(ScenarioRunSummary::fitnessDelta).average().orElse(0.0);
        List<EnvironmentPerturbationRecord> perturbations = perturbationRecorder.export(simulationId);
        return new StressTestReport(
                simulationId,
                count,
                survivalRate,
                avgEnergy,
                avgResources,
                avgFitness,
                Instant.now(),
                runs,
                perturbations
        );
    }

    private String resolveId(String simulationId) {
        if (simulationId == null || simulationId.isBlank()) {
            return ScenarioInjector.DEFAULT_SIMULATION_ID;
        }
        return simulationId;
    }
}


