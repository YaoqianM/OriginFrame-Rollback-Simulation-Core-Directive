package prototype.simulationcore.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import prototype.simulationcore.dto.SimulationCreateRequest;
import prototype.simulationcore.dto.SimulationRunDto;
import prototype.simulationcore.dto.WorldStateDto;
import prototype.simulationcore.service.SimulationWorldService;
import prototype.simulationcore.world.SimulationRun;
import prototype.simulationcore.world.WorldState;

@RestController
@RequestMapping("/simulation")
public class SimulationWorldController {

    private final SimulationWorldService simulationWorldService;

    public SimulationWorldController(SimulationWorldService simulationWorldService) {
        this.simulationWorldService = simulationWorldService;
    }

    @PostMapping("/create")
    public SimulationRunDto createWorld(@RequestBody(required = false) SimulationCreateRequest request) {
        SimulationRun run = simulationWorldService.createWorld(request);
        return SimulationRunDto.from(run);
    }

    @GetMapping("/{runId}/state")
    public WorldStateDto worldState(@PathVariable("runId") UUID runId) {
        try {
            WorldState state = simulationWorldService.snapshot(runId);
            return WorldStateDto.from(state);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }
}


