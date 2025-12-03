package prototype.lineageruntime.recovery;

import java.time.Instant;
import java.util.Objects;

public record ServiceSnapshot(
        String serviceId,
        String version,
        String instanceId,
        ServiceStatus status,
        Instant lastUpdated,
        boolean fallbackActive
) {

    public ServiceSnapshot {
        Objects.requireNonNull(serviceId, "serviceId");
        if (status == null) {
            status = ServiceStatus.UNKNOWN;
        }
        if (lastUpdated == null) {
            lastUpdated = Instant.now();
        }
    }

    public ServiceSnapshot withStatus(ServiceStatus newStatus) {
        return new ServiceSnapshot(serviceId, version, instanceId, newStatus, Instant.now(), fallbackActive);
    }

    public ServiceSnapshot withInstance(String newInstanceId, String newVersion, ServiceStatus newStatus) {
        String resolvedVersion = newVersion != null ? newVersion : version;
        return new ServiceSnapshot(serviceId, resolvedVersion, newInstanceId, newStatus, Instant.now(), fallbackActive);
    }

    public ServiceSnapshot withFallbackActive(boolean active) {
        return new ServiceSnapshot(serviceId, version, instanceId, status, Instant.now(), active);
    }
}


