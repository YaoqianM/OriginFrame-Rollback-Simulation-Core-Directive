package prototype.lineageruntime.resilience;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resilience.circuit-breaker")
public class CircuitBreakerProperties {

    private int failureThreshold = 5;
    private Duration resetTimeout = Duration.ofSeconds(30);
    private int halfOpenMaxCalls = 3;
    private Map<String, CircuitBreakerOverride> services = new HashMap<>();

    public CircuitBreakerConfig configFor(String serviceId) {
        CircuitBreakerOverride override = services.get(serviceId);
        int threshold = override != null && override.failureThreshold != null
                ? override.failureThreshold
                : failureThreshold;
        Duration timeout = override != null && override.resetTimeout != null
                ? override.resetTimeout
                : resetTimeout;
        int halfOpenCalls = override != null && override.halfOpenMaxCalls != null
                ? override.halfOpenMaxCalls
                : halfOpenMaxCalls;
        return new CircuitBreakerConfig(threshold, timeout, halfOpenCalls);
    }

    public Map<String, CircuitBreakerOverride> getServices() {
        return Collections.unmodifiableMap(services);
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public void setResetTimeout(Duration resetTimeout) {
        this.resetTimeout = resetTimeout;
    }

    public void setHalfOpenMaxCalls(int halfOpenMaxCalls) {
        this.halfOpenMaxCalls = halfOpenMaxCalls;
    }

    public void setServices(Map<String, CircuitBreakerOverride> services) {
        this.services = services != null ? new HashMap<>(services) : new HashMap<>();
    }

    public static class CircuitBreakerOverride {
        private Integer failureThreshold;
        private Duration resetTimeout;
        private Integer halfOpenMaxCalls;

        public Integer getFailureThreshold() {
            return failureThreshold;
        }

        public void setFailureThreshold(Integer failureThreshold) {
            this.failureThreshold = failureThreshold;
        }

        public Duration getResetTimeout() {
            return resetTimeout;
        }

        public void setResetTimeout(Duration resetTimeout) {
            this.resetTimeout = resetTimeout;
        }

        public Integer getHalfOpenMaxCalls() {
            return halfOpenMaxCalls;
        }

        public void setHalfOpenMaxCalls(Integer halfOpenMaxCalls) {
            this.halfOpenMaxCalls = halfOpenMaxCalls;
        }
    }
}

