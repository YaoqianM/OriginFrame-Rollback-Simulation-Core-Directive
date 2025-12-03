package prototype.lineageruntime.checkpoint;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "state_checkpoints", indexes = {
        @Index(name = "idx_checkpoint_service_time", columnList = "service_id, timestamp")
})
public class StateCheckpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkpointId;

    @Column(name = "service_id", nullable = false, length = 128)
    private String serviceId;

    @Lob
    @Column(name = "state_snapshot", nullable = false)
    private String stateSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "checkpoint_type", nullable = false, length = 32)
    private CheckpointType checkpointType;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected StateCheckpoint() {
    }

    public StateCheckpoint(String serviceId, String stateSnapshot, CheckpointType checkpointType, Instant timestamp) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.stateSnapshot = Objects.requireNonNull(stateSnapshot, "stateSnapshot");
        this.checkpointType = Objects.requireNonNull(checkpointType, "checkpointType");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public UUID getCheckpointId() {
        return checkpointId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getStateSnapshot() {
        return stateSnapshot;
    }

    public CheckpointType getCheckpointType() {
        return checkpointType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

