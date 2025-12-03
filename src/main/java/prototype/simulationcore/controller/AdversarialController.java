package prototype.simulationcore.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.adversarial.model.StressTestReport;
import prototype.simulationcore.adversarial.service.AdversarialScenarioFactory;
import prototype.simulationcore.adversarial.service.ScenarioInjector;
import prototype.simulationcore.adversarial.service.StressTestingService;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.dto.AdversarialScenarioRequest;
import prototype.simulationcore.dto.AdversarialScenarioRequest.Operation;
import prototype.simulationcore.dto.AdversarialScenarioResponse;
import prototype.simulationcore.service.SimulationService;

@RestController
@RequestMapping("/evolution/adversarial")
public class AdversarialController {

    private final ScenarioInjector scenarioInjector;
    private final AdversarialScenarioFactory scenarioFactory;
    private final StressTestingService stressTestingService;
    private final SimulationService simulationService;

    public AdversarialController(ScenarioInjector scenarioInjector,
                                 AdversarialScenarioFactory scenarioFactory,
                                 StressTestingService stressTestingService,
                                 SimulationService simulationService) {
        this.scenarioInjector = scenarioInjector;
        this.scenarioFactory = scenarioFactory;
        this.stressTestingService = stressTestingService;
        this.simulationService = simulationService;
    }

    @PostMapping("/inject")
    public AdversarialScenarioResponse inject(@RequestBody AdversarialScenarioRequest request) {
        Operation operation = request.getOperation() == null ? Operation.INJECT : request.getOperation();
        String simulationId = resolveSimulationId(request.getSimulationId());

        return switch (operation) {
            case STRESS_TEST -> handleStressTest(simulationId, request);
            case SCHEDULE -> schedule(simulationId, request);
            case RANDOM -> randomize(simulationId, request);
            case INJECT -> injectDirect(simulationId, request);
        };
    }

    @GetMapping("/report/{simulationId}")
    public StressTestReport report(@PathVariable String simulationId) {
        return stressTestingService.latestReport(resolveSimulationId(simulationId));
    }

    private AdversarialScenarioResponse injectDirect(String simulationId, AdversarialScenarioRequest request) {
        AdversarialScenario scenario = scenarioFactory.build(
                request.getScenarioType(),
                request.getSeverity(),
                request.getParameters()
        );
        UUID scenarioId = scenarioInjector.injectScenario(simulationId, scenario);
        return AdversarialScenarioResponse.injected(scenarioId);
    }

    private AdversarialScenarioResponse schedule(String simulationId, AdversarialScenarioRequest request) {
        AdversarialScenario scenario = scenarioFactory.build(
                request.getScenarioType(),
                request.getSeverity(),
                request.getParameters()
        );
        long tick = request.getTickNumber() == null ? 0L : request.getTickNumber();
        UUID scenarioId = scenarioInjector.scheduleScenario(simulationId, scenario, tick);
        return AdversarialScenarioResponse.scheduled(scenarioId);
    }

    private AdversarialScenarioResponse randomize(String simulationId, AdversarialScenarioRequest request) {
        List<AdversarialScenario> pool = scenarioFactory.fromTypes(
                request.getScenarioPool(),
                request.getSeverity(),
                request.getParameters()
        );
        double probability = request.getProbability() == null ? 0.5 : request.getProbability();
        UUID scenarioId = scenarioInjector.randomScenarioInjection(simulationId, probability, pool).orElse(null);
        return AdversarialScenarioResponse.randomized(scenarioId);
    }

    private AdversarialScenarioResponse handleStressTest(String simulationId, AdversarialScenarioRequest request) {
        int iterations = request.getIterations() == null ? 5 : request.getIterations();
        List<AdversarialScenario> pool = scenarioFactory.fromTypes(
                request.getScenarioPool(),
                request.getSeverity(),
                request.getParameters()
        );
        StressTestReport report = stressTestingService.runStressTest(simulationId, iterations, pool);
        return AdversarialScenarioResponse.stressReport(report);
    }

    private String resolveSimulationId(String explicit) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        Agent agent = simulationService.currentAgent();
        return agent.getAgentId() == null
                ? ScenarioInjector.DEFAULT_SIMULATION_ID
                : agent.getAgentId().toString();
    }
}


