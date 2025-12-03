package prototype.lineageruntime.recovery;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FailoverManager {

    private static final Logger log = LoggerFactory.getLogger(FailoverManager.class);

    private final ServiceTopology topology;

    public FailoverManager(ServiceTopology topology) {
        this.topology = topology;
    }

    public void registerFallback(String serviceId, String fallbackServiceId) {
        topology.registerFallback(serviceId, fallbackServiceId);
        log.info("Registered fallback {} for service {}", fallbackServiceId, serviceId);
    }

    public boolean hasFallback(String serviceId) {
        return topology.fallbackOf(serviceId).isPresent();
    }

    public Optional<String> fallbackOf(String serviceId) {
        return topology.fallbackOf(serviceId);
    }

    public FailoverAction activateFallback(String serviceId) {
        Optional<String> fallback = topology.fallbackOf(serviceId);
        if (fallback.isEmpty()) {
            return new FailoverAction(serviceId, null, false, "No fallback registered", topology.snapshot(serviceId));
        }

        ServiceSnapshot updated = topology.update(serviceId,
                snapshot -> snapshot.withFallbackActive(true).withStatus(ServiceStatus.DEGRADED));
        log.warn("Service {} switched to fallback {}", serviceId, fallback.get());
        return new FailoverAction(serviceId, fallback.get(), true, "Fallback activated", updated);
    }

    public FailoverAction deactivateFallback(String serviceId) {
        ServiceSnapshot updated = topology.update(serviceId,
                snapshot -> snapshot.withFallbackActive(false).withStatus(ServiceStatus.HEALTHY));
        log.info("Service {} returned to primary path", serviceId);
        return new FailoverAction(serviceId, topology.fallbackOf(serviceId).orElse(null), true, "Fallback deactivated", updated);
    }
}


