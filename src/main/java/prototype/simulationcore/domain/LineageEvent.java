package prototype.simulationcore.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a single lineage entry emitted when the agent transitions between states.
 */
public class LineageEvent {

    private final String eventId;
    private final String agentId;
    private final AgentState previousState;
    private final AgentState resultingState;
    private final Instant timestamp;

    private LineageEvent(String eventId,
                         String agentId,
                         AgentState previousState,
                         AgentState resultingState,
                         Instant timestamp) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.agentId = Objects.requireNonNull(agentId, "agentId");
        this.previousState = Objects.requireNonNull(previousState, "previousState");
        this.resultingState = Objects.requireNonNull(resultingState, "resultingState");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public static LineageEvent capture(String agentId, AgentState previousState, AgentState resultingState) {
        return new LineageEvent(
                UUID.randomUUID().toString(),
                agentId,
                previousState.snapshot(),
                resultingState.snapshot(),
                Instant.now()
        );
    }

    public String getEventId() {
        return eventId;
    }

    public String getAgentId() {
        return agentId;
    }

    public AgentState getPreviousState() {
        return previousState;
    }

    public AgentState getResultingState() {
        return resultingState;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

