package prototype.simulationcore.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.lineageruntime.lineage.model.LineageMetrics;
import prototype.lineageruntime.lineage.service.LineageTrackerService;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.repository.AgentRepository;

@Service
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final SimulationService simulationService;
    private final EventConsumer eventConsumer;
    private final AgentRepository agentRepository;
    private final LineageTrackerService lineageTrackerService;

    public ReplayService(SimulationService simulationService,
                         EventConsumer eventConsumer,
                         AgentRepository agentRepository,
                         LineageTrackerService lineageTrackerService) {
        this.simulationService = simulationService;
        this.eventConsumer = eventConsumer;
        this.agentRepository = agentRepository;
        this.lineageTrackerService = lineageTrackerService;
    }

    @Transactional
    public Agent replay() {
        List<LineageEvent> history = eventConsumer.getHistory();
        Agent agent = simulationService.currentAgent();
        AgentState starting = agent.snapshotState();

        if (history.isEmpty()) {
            agent.replaceState(AgentState.initial());
            Agent persisted = agentRepository.save(agent);
            log.info("Replay reset agent from {} to {}", starting, persisted.getState());
            return persisted;
        }

        AgentState resetState = history.get(0).getPreviousState();
        agent.replaceState(resetState);
        log.info("Replay reset agent to {}", resetState);

        for (LineageEvent event : history) {
            agent.replaceState(event.getResultingState());
        }

        Agent persisted = agentRepository.save(agent);
        log.info("Replay rebuilt agent from {} to {}", starting, persisted.getState());
        lineageTrackerService.recordPerformanceUpdate(persisted, LineageMetrics.fromAgent(
                persisted,
                Map.of(
                        "operation", "replay",
                        "timestamp", Instant.now().toString()
                )));
        return persisted;
    }
}


