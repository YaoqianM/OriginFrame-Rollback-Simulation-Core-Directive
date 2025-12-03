package prototype.lineageruntime.resilience;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FaultIsolator {

    private static final Logger log = LoggerFactory.getLogger(FaultIsolator.class);
    private static final String TOPIC = "fault-isolation-events";

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final ServiceRegistry serviceRegistry;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Set<String> isolatedServices = ConcurrentHashMap.newKeySet();

    public FaultIsolator(
            CircuitBreakerRegistry circuitBreakerRegistry,
            ServiceRegistry serviceRegistry,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.serviceRegistry = serviceRegistry;
        this.kafkaTemplate = kafkaTemplate;
    }

    public FaultIsolationEvent isolateService(String serviceId, String reason) {
        Objects.requireNonNull(serviceId, "serviceId");
        String isolationReason = reason == null || reason.isBlank()
                ? "Automated isolation"
                : reason;

        boolean cascadePrevented = serviceRegistry.getDependencies(serviceId).stream()
                .anyMatch(this::isIsolated);

        if (cascadePrevented) {
            FaultIsolationEvent event = new FaultIsolationEvent(
                    serviceId,
                    Instant.now(),
                    isolationReason + " (suppressed due to upstream isolation)",
                    circuitBreakerRegistry.getOrCreate(serviceId).getState(),
                    Collections.emptySet(),
                    true
            );
            kafkaTemplate.send(TOPIC, serviceId, event);
            log.info("Suppressed isolation for {} due to upstream dependency already isolated", serviceId);
            return event;
        }

        CircuitBreaker breaker = circuitBreakerRegistry.getOrCreate(serviceId);
        breaker.forceOpen(isolationReason);
        isolatedServices.add(serviceId);

        Set<String> dependents = Set.copyOf(serviceRegistry.getDependents(serviceId));
        dependents.forEach(dependent -> serviceRegistry.markDependencyIsolated(dependent, serviceId));

        FaultIsolationEvent event = new FaultIsolationEvent(
                serviceId,
                Instant.now(),
                isolationReason,
                breaker.getState(),
                dependents,
                false
        );
        kafkaTemplate.send(TOPIC, serviceId, event);
        log.warn("Isolated service {}. Notified dependents {}", serviceId, dependents);
        return event;
    }

    public boolean isIsolated(String serviceId) {
        return isolatedServices.contains(serviceId);
    }
}

