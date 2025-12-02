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
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final SimulationService simulationService;
    private final EventConsumer eventConsumer;

    public ReplayService(SimulationService simulationService, EventConsumer eventConsumer) {
        this.simulationService = simulationService;
        this.eventConsumer = eventConsumer;
    }

    public synchronized Agent replay() {
        List<LineageEvent> history = eventConsumer.getHistory();
        Agent agent = simulationService.currentAgent();
        AgentState starting = agent.snapshotState();

        if (history.isEmpty()) {
            agent.replaceState(AgentState.initial());
            log.info("Replay reset agent from {} to {}", starting, agent.getState());
            return agent;
        }

        AgentState resetState = history.get(0).getPreviousState();
        agent.replaceState(resetState);
        log.info("Replay reset agent to {}", resetState);

        for (LineageEvent event : history) {
            agent.replaceState(event.getResultingState());
        }

        log.info("Replay rebuilt agent from {} to {}", starting, agent.getState());
        return agent;
    }
}

