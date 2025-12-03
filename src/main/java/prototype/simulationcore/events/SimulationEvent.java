package prototype.simulationcore.events;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Canonical event envelope emitted by the simulation orchestrator and
 * published to Kafka for downstream consumers.
 */
public final class SimulationEvent {

    private final String eventId;
    private final SimulationEventType type;
    private final UUID simulationId;
    private final long tick;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    private SimulationEvent(String eventId,
                            SimulationEventType type,
                            UUID simulationId,
                            long tick,
                            Instant timestamp,
                            Map<String, Object> metadata) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.type = Objects.requireNonNull(type, "type");
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.tick = tick;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static SimulationEvent of(SimulationEventType type,
                                     UUID simulationId,
                                     long tick,
                                     Map<String, Object> metadata) {
        return new SimulationEvent(
                UUID.randomUUID().toString(),
                type,
                simulationId,
                tick,
                Instant.now(),
                metadata
        );
    }

    public String getEventId() {
        return eventId;
    }

    public SimulationEventType getType() {
        return type;
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    public long getTick() {
        return tick;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

