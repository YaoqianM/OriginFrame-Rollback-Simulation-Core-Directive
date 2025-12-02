package prototype.simulationcore.service;

import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.lineageruntime.kafka.EventProducer;

@Service
public class SimulationService {

    private final EventProducer eventProducer;
    private final Agent agent = new Agent("agent-1");

    public SimulationService(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public synchronized Agent step() {
        AgentState previous = agent.snapshotState();
        AgentState updated = previous.next(1);
        agent.replaceState(updated);

        LineageEvent event = LineageEvent.capture(agent.getId(), previous, updated);
        eventProducer.send(event);

        return agent;
    }

    public Agent currentAgent() {
        return agent;
    }
}

