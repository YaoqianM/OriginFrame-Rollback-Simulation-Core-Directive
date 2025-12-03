package prototype.lineageruntime.recovery;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.health.HealthMonitorService;

@Service
public class ServiceReconstructor {

    private static final Logger log = LoggerFactory.getLogger(ServiceReconstructor.class);

    private final ServiceTopology topology;
    private final ServiceHealthProbe healthProbe;
    private final HealthMonitorService healthMonitorService;
    private final RecoveryProperties.BackoffProperties backoff;

    public ServiceReconstructor(ServiceTopology topology,
                                ServiceHealthProbe healthProbe,
                                HealthMonitorService healthMonitorService,
                                RecoveryProperties properties) {
        this.topology = topology;
        this.healthProbe = healthProbe;
        this.healthMonitorService = healthMonitorService;
        this.backoff = properties.getBackoff();
    }

    public ServiceRecoveryAction restartService(String serviceId) {
        return executeWithBackoff(serviceId, RecoveryActionType.RESTART, () -> {
            ServiceSnapshot snapshot = topology.updateInstance(
                    serviceId,
                    newInstanceId(serviceId),
                    null,
                    ServiceStatus.RESTARTING
            );
            log.info("Restarting service {} with instance {}", serviceId, snapshot.instanceId());
            triggerHealthSample(serviceId);
            return snapshot;
        });
    }

    public ServiceRecoveryAction redeployService(String serviceId, String targetVersion) {
        return executeWithBackoff(serviceId, RecoveryActionType.REDEPLOY, () -> {
            ServiceSnapshot snapshot = topology.updateInstance(
                    serviceId,
                    newInstanceId(serviceId),
                    targetVersion,
                    ServiceStatus.RECOVERING
            );
            log.info("Redeploying service {} to version {}", serviceId, snapshot.version());
            triggerHealthSample(serviceId);
            return snapshot;
        });
    }

    public boolean verifyHealth(String serviceId) {
        return healthProbe.isHealthy(serviceId);
    }

    private ServiceRecoveryAction executeWithBackoff(String serviceId,
                                                     RecoveryActionType action,
                                                     Supplier<ServiceSnapshot> stageSupplier) {
        Duration delay = backoff.getInitial();
        ServiceSnapshot snapshot = null;
        for (int attempt = 1; attempt <= backoff.getAttempts(); attempt++) {
            snapshot = stageSupplier.get();
            if (healthProbe.isHealthy(serviceId)) {
                ServiceSnapshot healthy = topology.updateStatus(serviceId, ServiceStatus.HEALTHY);
                String detail = "Service passed health verification on attempt " + attempt;
                return new ServiceRecoveryAction(serviceId, action, attempt, true, detail, healthy);
            }

            log.warn("Health check failed for service {} on attempt {}. Retrying in {}", serviceId, attempt, delay);
            sleep(delay);
            delay = nextDelay(delay);
        }

        ServiceSnapshot failed = topology.updateStatus(serviceId, ServiceStatus.FAILED);
        String detail = "Service failed health verification after " + backoff.getAttempts() + " attempts";
        return new ServiceRecoveryAction(serviceId, action, backoff.getAttempts(), false, detail, failed);
    }

    private Duration nextDelay(Duration current) {
        Duration candidate = current.multipliedBy(2);
        if (candidate.compareTo(backoff.getMax()) > 0) {
            return backoff.getMax();
        }
        return candidate;
    }

    private void sleep(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Backoff sleep interrupted");
        }
    }

    private void triggerHealthSample(String serviceId) {
        healthMonitorService.recordHeartbeat(serviceId);
    }

    private String newInstanceId(String serviceId) {
        return serviceId + "-" + UUID.randomUUID();
    }
}


