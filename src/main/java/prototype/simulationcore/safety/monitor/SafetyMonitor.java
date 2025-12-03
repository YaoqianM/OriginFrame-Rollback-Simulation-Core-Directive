package prototype.simulationcore.safety.monitor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.domain.SafetyViolation;

@Component
public class SafetyMonitor {

    private static final Logger log = LoggerFactory.getLogger(SafetyMonitor.class);
    private static final String TOPIC = "safety-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ConcurrentMap<UUID, AgentViolationSummary> violationSummaries = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Boolean> eliminationCandidates = new ConcurrentHashMap<>();

    public SafetyMonitor(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void recordViolation(Agent agent, SafetyViolation violation) {
        if (agent == null || violation == null) {
            return;
        }

        violationSummaries
                .computeIfAbsent(agent.getAgentId(), ignored -> new AgentViolationSummary())
                .increment(violation.getSeverity(), agent.getGeneration());

        publishEvent(violation);
    }

    public Map<Severity, Integer> getSeverityCounts(UUID agentId) {
        AgentViolationSummary summary = violationSummaries.get(agentId);
        return summary == null ? Map.of() : summary.copySeverityCounts();
    }

    public Map<Integer, Integer> getGenerationCounts(UUID agentId) {
        AgentViolationSummary summary = violationSummaries.get(agentId);
        return summary == null ? Map.of() : summary.copyGenerationCounts();
    }

    public void markForElimination(UUID agentId) {
        if (agentId != null) {
            eliminationCandidates.put(agentId, true);
            log.warn("Agent {} marked for elimination after repeated safety violations", agentId);
        }
    }

    public boolean isEliminationCandidate(UUID agentId) {
        return agentId != null && eliminationCandidates.containsKey(agentId);
    }

    private void publishEvent(SafetyViolation violation) {
        try {
            SafetyViolationEvent event = new SafetyViolationEvent(
                    violation.getAgentId(),
                    violation.getConstraintType(),
                    violation.getSeverity(),
                    violation.getActionAttempted(),
                    violation.getMessage(),
                    violation.getGeneration(),
                    violation.getTimestamp()
            );
            kafkaTemplate.send(TOPIC, violation.getAgentId().toString(), event);
        } catch (Exception ex) {
            log.warn("Failed to publish safety violation event: {}", ex.getMessage());
        }
    }

    private static final class AgentViolationSummary {
        private final ConcurrentMap<Severity, AtomicInteger> severityCounts = new ConcurrentHashMap<>();
        private final ConcurrentMap<Integer, AtomicInteger> generationCounts = new ConcurrentHashMap<>();

        private void increment(Severity severity, int generation) {
            severityCounts.computeIfAbsent(severity, ignored -> new AtomicInteger()).incrementAndGet();
            generationCounts.computeIfAbsent(generation, ignored -> new AtomicInteger()).incrementAndGet();
        }

        private Map<Severity, Integer> copySeverityCounts() {
            return severityCounts.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        }

        private Map<Integer, Integer> copyGenerationCounts() {
            return generationCounts.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> entry.getValue().get()));
        }
    }
}

