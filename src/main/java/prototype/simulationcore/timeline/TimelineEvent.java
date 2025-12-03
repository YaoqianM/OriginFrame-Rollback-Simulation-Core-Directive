package prototype.simulationcore.timeline;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * General purpose timeline event for visualization and auditing.
 */
public final class TimelineEvent {

    private final String simulationId;
    private final int tick;
    private final String eventType;
    private final String description;
    private final Instant timestamp;
    private final Map<String, Object> metadata;

    private TimelineEvent(Builder builder) {
        this.simulationId = builder.simulationId;
        this.tick = builder.tick;
        this.eventType = builder.eventType;
        this.description = builder.description;
        this.timestamp = builder.timestamp == null ? Instant.now() : builder.timestamp;
        this.metadata = Collections.unmodifiableMap(new HashMap<>(builder.metadata));
    }

    public String simulationId() {
        return simulationId;
    }

    public int tick() {
        return tick;
    }

    public String eventType() {
        return eventType;
    }

    public String description() {
        return description;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String simulationId;
        private int tick;
        private String eventType;
        private String description;
        private Instant timestamp;
        private Map<String, Object> metadata = new HashMap<>();

        private Builder() {
        }

        public Builder simulationId(String simulationId) {
            this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
            return this;
        }

        public Builder tick(int tick) {
            this.tick = tick;
            return this;
        }

        public Builder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                this.metadata.putAll(metadata);
            }
            return this;
        }

        public Builder metadataEntry(String key, Object value) {
            if (key != null && value != null) {
                this.metadata.put(key, value);
            }
            return this;
        }

        public TimelineEvent build() {
            if (simulationId == null) {
                throw new IllegalStateException("simulationId is required");
            }
            return new TimelineEvent(this);
        }
    }
}

