package prototype.lineageruntime.recovery;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.lineageruntime.health.HealthMonitorService;

@Component
public class SlidingWindowHealthProbe implements ServiceHealthProbe {

    private static final Logger log = LoggerFactory.getLogger(SlidingWindowHealthProbe.class);

    private final HealthMonitorService healthMonitorService;
    private final Duration maxStaleness;

    public SlidingWindowHealthProbe(HealthMonitorService healthMonitorService, RecoveryProperties properties) {
        this.healthMonitorService = healthMonitorService;
        this.maxStaleness = properties.getHealth().getMaxStaleness();
    }

    @Override
    public boolean isHealthy(String serviceId) {
        Optional<Instant> latest = healthMonitorService.latestHeartbeat(serviceId);
        if (latest.isEmpty()) {
            log.debug("No heartbeat observed for service {}", serviceId);
            return false;
        }

        Duration age = Duration.between(latest.get(), Instant.now());
        boolean healthy = age.compareTo(maxStaleness) <= 0;
        if (!healthy) {
            log.warn("Service {} heartbeat stale by {} > {}", serviceId, age, maxStaleness);
        }
        return healthy;
    }
}


