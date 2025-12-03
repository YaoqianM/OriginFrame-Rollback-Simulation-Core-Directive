package prototype.lineageruntime.checkpoint;

import java.time.Instant;
import java.util.Objects;

/**
 * Application event emitted when a service completes an operation that should count
 * toward adaptive checkpoint thresholds.
 */
public final class ServiceOperationEvent {

    private final String serviceId;
    private final Instant timestamp;

    public ServiceOperationEvent(String serviceId) {
        this(serviceId, Instant.now());
    }

    public ServiceOperationEvent(String serviceId, Instant timestamp) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public String getServiceId() {
        return serviceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

