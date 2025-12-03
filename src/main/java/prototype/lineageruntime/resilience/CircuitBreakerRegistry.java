package prototype.lineageruntime.resilience;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class CircuitBreakerRegistry {

    private final ConcurrentHashMap<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final CircuitBreakerProperties properties;

    public CircuitBreakerRegistry(CircuitBreakerProperties properties) {
        this.properties = properties;
    }

    public CircuitBreaker getOrCreate(String serviceId) {
        return breakers.computeIfAbsent(serviceId, id -> new CircuitBreaker(id, properties.configFor(id)));
    }

    public Map<String, Integer> snapshotFailureCounts() {
        return Collections.unmodifiableMap(
                breakers.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().getFailureCount()
                        ))
        );
    }
}

