package prototype.simulationcore.safety;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.repository.AgentRepository;
import prototype.simulationcore.repository.SafetyViolationRepository;
import prototype.simulationcore.safety.domain.SafetyViolation;
import prototype.simulationcore.safety.events.CriticalSafetyViolationEvent;
import prototype.simulationcore.safety.monitor.SafetyMonitor;

@Component
public class ViolationHandler {

    private static final Logger log = LoggerFactory.getLogger(ViolationHandler.class);

    private final SafetyViolationRepository violationRepository;
    private final AgentRepository agentRepository;
    private final SafetyMonitor safetyMonitor;
    private final SafetyProperties properties;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ViolationHandler(SafetyViolationRepository violationRepository,
                            AgentRepository agentRepository,
                            SafetyMonitor safetyMonitor,
                            SafetyProperties properties,
                            ObjectMapper objectMapper,
                            ApplicationEventPublisher eventPublisher) {
        this.violationRepository = violationRepository;
        this.agentRepository = agentRepository;
        this.safetyMonitor = safetyMonitor;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    public void handleViolation(Agent agent, Violation violation, Environment environment) {
        Environment resolved = environment == null ? new DefaultEnvironment(agent.getState()) : environment;
        String serializedEnvironment = serializeEnvironment(resolved);
        SafetyViolation entity = SafetyViolation.from(agent, violation, serializedEnvironment);
        violationRepository.save(entity);

        agent.recordSafetyViolation();
        agentRepository.save(agent);
        safetyMonitor.recordViolation(agent, entity);

        if (agent.getSafetyViolations() >= properties.getEliminationThreshold()) {
            safetyMonitor.markForElimination(agent.getAgentId());
        }

        if (violation.severity() == Severity.CRITICAL) {
            eventPublisher.publishEvent(new CriticalSafetyViolationEvent(agent.getAgentId(), violation));
        }

        log.warn("Safety violation [{}] detected for agent {}: {}", violation.constraintType(),
                agent.getAgentId(), violation.message());
    }

    private String serializeEnvironment(Environment environment) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "targetPosition", Map.of(
                            "x", environment.getTargetPosition().x(),
                            "y", environment.getTargetPosition().y(),
                            "z", environment.getTargetPosition().z()
                    ),
                    "sensors", environment.snapshotSensors()
            ));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize environment snapshot: {}", e.getMessage());
            return "{}";
        }
    }
}

