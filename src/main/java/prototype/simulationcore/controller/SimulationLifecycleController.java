package prototype.simulationcore.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import prototype.simulationcore.dto.RunUntilRequest;
import prototype.simulationcore.dto.ScenarioLoadRequest;
import prototype.simulationcore.dto.SimulationConfigRequest;
import prototype.simulationcore.dto.SimulationWorldView;
import prototype.simulationcore.orchestrator.SimulationOrchestrator;

@RestController
@RequestMapping("/simulation")
public class SimulationLifecycleController {

    private final SimulationOrchestrator orchestrator;

    public SimulationLifecycleController(SimulationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SimulationWorldView create(@RequestBody SimulationConfigRequest request) {
        return SimulationWorldView.from(orchestrator.createSimulation(request.toConfig()));
    }

    @GetMapping
    public List<SimulationWorldView> list() {
        return orchestrator.listSimulations().stream()
                .map(SimulationWorldView::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SimulationWorldView get(@PathVariable("id") UUID simulationId) {
        return orchestrator.find(simulationId)
                .map(SimulationWorldView::from)
                .orElseThrow(() -> new IllegalArgumentException("Simulation not found: " + simulationId));
    }

    @PostMapping("/{id}/start")
    public SimulationWorldView start(@PathVariable("id") UUID simulationId) {
        return SimulationWorldView.from(orchestrator.start(simulationId));
    }

    @PostMapping("/{id}/pause")
    public SimulationWorldView pause(@PathVariable("id") UUID simulationId) {
        return SimulationWorldView.from(orchestrator.pause(simulationId));
    }

    @PostMapping("/{id}/stop")
    public SimulationWorldView stop(@PathVariable("id") UUID simulationId) {
        return SimulationWorldView.from(orchestrator.stop(simulationId));
    }

    @PostMapping("/{id}/step")
    public SimulationWorldView step(@PathVariable("id") UUID simulationId) {
        return SimulationWorldView.from(orchestrator.step(simulationId));
    }

    @PostMapping("/{id}/run-until")
    public SimulationWorldView runUntil(@PathVariable("id") UUID simulationId,
                                        @RequestBody RunUntilRequest request) {
        return SimulationWorldView.from(orchestrator.runUntil(simulationId, request.targetTick()));
    }

    @PostMapping("/{id}/scenario/load")
    public SimulationWorldView loadScenario(@PathVariable("id") UUID simulationId,
                                            @RequestBody ScenarioLoadRequest request) {
        return SimulationWorldView.from(orchestrator.loadScenario(simulationId, request.scenarioFile()));
    }
}

