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
import prototype.lineageruntime.resilience.CircuitBreakerGuard;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.repository.AgentRepository;

@Service
public class RollbackService {

    private static final Logger log = LoggerFactory.getLogger(RollbackService.class);

    private final SimulationService simulationService;
    private final EventConsumer eventConsumer;
    private final AgentRepository agentRepository;
    private final LineageTrackerService lineageTrackerService;

    public RollbackService(SimulationService simulationService,
                           EventConsumer eventConsumer,
                           AgentRepository agentRepository,
                           LineageTrackerService lineageTrackerService) {
        this.simulationService = simulationService;
        this.eventConsumer = eventConsumer;
        this.agentRepository = agentRepository;
        this.lineageTrackerService = lineageTrackerService;
    }

    @CircuitBreakerGuard(serviceId = "rollback-service")
    @Transactional
    public Agent rollback() {
        Agent agent = simulationService.currentAgent();
        AgentState before = agent.snapshotState();

        List<LineageEvent> history = eventConsumer.getHistory();
        if (history.isEmpty()) {
            log.info("Rollback requested but no lineage history. Agent remains at {}", before);
            return agent;
        }

        LineageEvent latest = history.get(history.size() - 1);
        agent.replaceState(latest.getPreviousState());
        Agent persisted = agentRepository.save(agent);
        log.info("Rolled back via event {} from {} to {}", latest.getEventId(), before, persisted.getState());
        lineageTrackerService.recordPerformanceUpdate(persisted, LineageMetrics.fromAgent(
                persisted,
                Map.of(
                        "operation", "rollback",
                        "eventId", latest.getEventId(),
                        "timestamp", Instant.now().toString()
                )));
        return persisted;
    }
}


