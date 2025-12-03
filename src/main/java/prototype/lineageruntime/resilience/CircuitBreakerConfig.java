package prototype.lineageruntime.resilience;

import java.time.Duration;
import java.util.Objects;

public record CircuitBreakerConfig(
        int failureThreshold,
        Duration resetTimeout,
        int halfOpenMaxCalls
) {

    public CircuitBreakerConfig {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold must be > 0");
        }
        Objects.requireNonNull(resetTimeout, "resetTimeout");
        if (resetTimeout.isNegative() || resetTimeout.isZero()) {
            throw new IllegalArgumentException("resetTimeout must be positive");
        }
        if (halfOpenMaxCalls <= 0) {
            throw new IllegalArgumentException("halfOpenMaxCalls must be > 0");
        }
    }
}

