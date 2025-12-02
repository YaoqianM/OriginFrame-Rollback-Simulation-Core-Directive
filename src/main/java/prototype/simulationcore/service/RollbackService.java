package prototype.simulationcore.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;

@Service
public class RollbackService {

    private static final Logger log = LoggerFactory.getLogger(RollbackService.class);

    private final SimulationService simulationService;
    private final EventConsumer eventConsumer;

    public RollbackService(SimulationService simulationService, EventConsumer eventConsumer) {
        this.simulationService = simulationService;
        this.eventConsumer = eventConsumer;
    }

    public synchronized Agent rollback() {
        Agent agent = simulationService.currentAgent();
        AgentState before = agent.snapshotState();

        List<LineageEvent> history = eventConsumer.getHistory();
        if (history.isEmpty()) {
            log.info("Rollback requested but no lineage history. Agent remains at {}", before);
            return agent;
        }

        LineageEvent latest = history.get(history.size() - 1);
        agent.replaceState(latest.getPreviousState());
        log.info("Rolled back via event {} from {} to {}", latest.getEventId(), before, agent.getState());
        return agent;
    }
}

