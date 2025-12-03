package prototype.lineageruntime.checkpoint;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.repository.AgentRepository;
import prototype.simulationcore.service.SimulationService;

@Component
public class SimulationStateAdapter implements ServiceStateAdapter {

    private static final Logger log = LoggerFactory.getLogger(SimulationStateAdapter.class);

    private final SimulationService simulationService;
    private final AgentRepository agentRepository;
    private final ObjectMapper objectMapper;

    public SimulationStateAdapter(SimulationService simulationService,
                                  AgentRepository agentRepository,
                                  ObjectMapper objectMapper) {
        this.simulationService = simulationService;
        this.agentRepository = agentRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public String serviceId() {
        return RuntimeServiceIds.SIMULATION_CORE;
    }

    @Override
    public String captureSnapshot() {
        try {
            return objectMapper.writeValueAsString(simulationService.currentAgent().snapshotState());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize simulation state", e);
        }
    }

    @Override
    public void restoreFromSnapshot(String snapshot) {
        try {
            AgentState state = objectMapper.readValue(snapshot, AgentState.class);
            var agent = simulationService.currentAgent();
            agent.replaceState(state);
            agentRepository.save(agent);
            log.info("Simulation state restored to {}", state);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize simulation snapshot", e);
        }
    }
}

