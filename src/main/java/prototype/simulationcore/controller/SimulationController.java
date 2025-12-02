package prototype.simulationcore.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.lineageruntime.model.LineageRecord;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.service.ReplayService;
import prototype.simulationcore.service.RollbackService;
import prototype.simulationcore.service.SimulationService;

@RestController
@RequestMapping("/simulate")
public class SimulationController {

    private final SimulationService simulationService;
    private final RollbackService rollbackService;
    private final ReplayService replayService;
    private final EventConsumer eventConsumer;

    public SimulationController(SimulationService simulationService,
                                RollbackService rollbackService,
                                ReplayService replayService,
                                EventConsumer eventConsumer) {
        this.simulationService = simulationService;
        this.rollbackService = rollbackService;
        this.replayService = replayService;
        this.eventConsumer = eventConsumer;
    }

    @PostMapping("/step")
    public Agent step() {
        return simulationService.step();
    }

    @PostMapping("/rollback")
    public Agent rollback() {
        return rollbackService.rollback();
    }

    @PostMapping("/replay")
    public Agent replay() {
        return replayService.replay();
    }

    @GetMapping("/history")
    public List<LineageRecord> history() {
        return eventConsumer.getHistoryView();
    }
}

