package prototype.lineageruntime.checkpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AdaptiveCheckpointManager {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveCheckpointManager.class);

    private final CheckpointService checkpointService;
    private final CheckpointProperties properties;
    private final Map<String, AtomicInteger> operationCounters = new ConcurrentHashMap<>();

    public AdaptiveCheckpointManager(CheckpointService checkpointService, CheckpointProperties properties) {
        this.checkpointService = checkpointService;
        this.properties = properties;
    }

    @EventListener
    public void onServiceOperation(ServiceOperationEvent event) {
        Set<String> criticalServices = properties.getCritical().getServices();
        int threshold = properties.getCritical().getOperationInterval();
        if (criticalServices.isEmpty() || threshold <= 0) {
            return;
        }
        if (!criticalServices.contains(event.getServiceId())) {
            return;
        }
        int updated = operationCounters
                .computeIfAbsent(event.getServiceId(), key -> new AtomicInteger(0))
                .incrementAndGet();
        if (updated >= threshold) {
            operationCounters.get(event.getServiceId()).set(0);
            log.debug("Critical service {} reached {} operations; capturing checkpoint", event.getServiceId(), threshold);
            try {
                checkpointService.createCheckpoint(event.getServiceId(), CheckpointType.PERIODIC);
            } catch (IllegalArgumentException ex) {
                log.warn("Unable to capture checkpoint for service {}: {}", event.getServiceId(), ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "#{T(java.lang.Long).toString(@checkpointProperties.nonCritical.pollInterval.toMillis())}")
    public void enforceNonCriticalSchedule() {
        Set<String> services = properties.getNonCritical().getServices();
        Duration interval = properties.getNonCritical().getInterval();
        if (services.isEmpty() || interval.isZero() || interval.isNegative()) {
            return;
        }
        Instant cutoff = Instant.now().minus(interval);
        for (String serviceId : services) {
            try {
                boolean needsCheckpoint = checkpointService.getLatestCheckpoint(serviceId)
                        .map(cp -> cp.getTimestamp().isBefore(cutoff))
                        .orElse(true);
                if (needsCheckpoint) {
                    log.debug("Non-critical service {} exceeded interval {}; capturing checkpoint", serviceId, interval);
                    checkpointService.createCheckpoint(serviceId, CheckpointType.PERIODIC);
                }
            } catch (IllegalArgumentException ex) {
                log.warn("Skipping checkpoint for {} due to missing adapter: {}", serviceId, ex.getMessage());
            }
        }
    }

    @Scheduled(fixedDelayString = "#{T(java.lang.Long).toString(@checkpointProperties.retentionSweepInterval.toMillis())}")
    public void pruneExpiredCheckpoints() {
        checkpointService.pruneOldCheckpoints(properties.getRetentionDays());
    }
}

