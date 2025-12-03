package prototype.simulationcore.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.simulationcore.evolution.dto.EvolutionStartRequest;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.evolution.dto.LeaderboardEntry;
import prototype.simulationcore.evolution.selection.SelectionSettings;
import prototype.simulationcore.evolution.service.EvolutionLoopService;

@RestController
@RequestMapping("/evolution")
public class EvolutionController {

    private final EvolutionLoopService evolutionLoopService;

    public EvolutionController(EvolutionLoopService evolutionLoopService) {
        this.evolutionLoopService = evolutionLoopService;
    }

    @PostMapping("/start")
    public EvolutionStatus start(@RequestBody(required = false) EvolutionStartRequest request) {
        EvolutionStartRequest normalized = request == null
                ? new EvolutionStartRequest(null, null, null, null, null, null, null, null)
                : request;
        int populationSize = normalized.resolvePopulationSize();
        SelectionSettings settings = normalized.toSelectionSettings(populationSize);
        double mutationRate = normalized.resolveMutationRate();
        return evolutionLoopService.initializePopulation(populationSize, normalized.basePolicyId(), settings, mutationRate);
    }

    @PostMapping("/pause")
    public EvolutionStatus pause() {
        return evolutionLoopService.pause();
    }

    @PostMapping("/step")
    public GenerationReport step() {
        return evolutionLoopService.runGeneration();
    }

    @GetMapping("/status")
    public EvolutionStatus status() {
        return evolutionLoopService.status();
    }

    @GetMapping("/leaderboard")
    public List<LeaderboardEntry> leaderboard() {
        return evolutionLoopService.leaderboard();
    }

    @GetMapping("/report/{generation}")
    public GenerationReport report(@PathVariable("generation") int generation) {
        return evolutionLoopService.getGenerationReport(generation);
    }
}


