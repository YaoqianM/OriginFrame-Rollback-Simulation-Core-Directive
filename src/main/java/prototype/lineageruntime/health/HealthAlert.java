package prototype.lineageruntime.health;

import java.time.Instant;

public record HealthAlert(
        String serviceId,
        String metricType,
        double value,
        Instant timestamp,
        String description
) {
}

