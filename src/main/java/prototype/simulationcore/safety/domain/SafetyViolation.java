package prototype.simulationcore.safety.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.Violation;

@Entity
@Table(name = "safety_violations")
public class SafetyViolation {

    @Id
    @GeneratedValue
    @Column(name = "violation_id", updatable = false, nullable = false)
    private UUID violationId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "constraint_type", nullable = false)
    private String constraintType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Column(name = "action_attempted", nullable = false)
    private String actionAttempted;

    @Lob
    @Column(name = "environment_state")
    private String environmentState;

    @Column(name = "message")
    private String message;

    @Column(name = "generation")
    private int generation;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected SafetyViolation() {
    }

    public SafetyViolation(UUID violationId,
                           UUID agentId,
                           String constraintType,
                           Severity severity,
                           String actionAttempted,
                           String environmentState,
                           String message,
                           int generation,
                           Instant timestamp) {
        this.violationId = violationId;
        this.agentId = agentId;
        this.constraintType = constraintType;
        this.severity = severity;
        this.actionAttempted = actionAttempted;
        this.environmentState = environmentState;
        this.message = message;
        this.generation = generation;
        this.timestamp = timestamp;
    }

    public static SafetyViolation from(Agent agent, Violation violation, String environmentState) {
        Action attempted = violation.actionAttempted() == null ? Action.WAIT : violation.actionAttempted();
        return new SafetyViolation(
                null,
                agent.getAgentId(),
                violation.constraintType(),
                violation.severity(),
                attempted.name(),
                environmentState,
                violation.message(),
                agent.getGeneration(),
                violation.occurredAt()
        );
    }

    public UUID getViolationId() {
        return violationId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public String getConstraintType() {
        return constraintType;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getActionAttempted() {
        return actionAttempted;
    }

    public String getEnvironmentState() {
        return environmentState;
    }

    public String getMessage() {
        return message;
    }

    public int getGeneration() {
        return generation;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

