package prototype.lineageruntime.resilience;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    private final String serviceId;
    private final CircuitBreakerConfig config;
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenCallCount = new AtomicInteger(0);
    private final Clock clock;

    private volatile CircuitBreakerState state = CircuitBreakerState.CLOSED;
    private volatile Instant lastFailureTime;

    public CircuitBreaker(String serviceId, CircuitBreakerConfig config) {
        this(serviceId, config, Clock.systemUTC());
    }

    CircuitBreaker(String serviceId, CircuitBreakerConfig config, Clock clock) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.config = Objects.requireNonNull(config, "config");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean tryAcquirePermission() {
        synchronized (this) {
            if (state == CircuitBreakerState.OPEN) {
                if (hasResetTimeoutElapsed()) {
                    transitionToHalfOpen();
                } else {
                    return false;
                }
            }

            if (state == CircuitBreakerState.HALF_OPEN) {
                int attempts = halfOpenCallCount.incrementAndGet();
                if (attempts > config.halfOpenMaxCalls()) {
                    halfOpenCallCount.decrementAndGet();
                    return false;
                }
            }
            return true;
        }
    }

    public void recordSuccess() {
        synchronized (this) {
            failureCount.set(0);
            halfOpenCallCount.set(0);
            lastFailureTime = null;
            if (state != CircuitBreakerState.CLOSED) {
                log.info("Circuit breaker reset to CLOSED for service {}", serviceId);
            }
            state = CircuitBreakerState.CLOSED;
        }
    }

    public void recordFailure() {
        synchronized (this) {
            lastFailureTime = clock.instant();
            int failures = failureCount.incrementAndGet();

            if (state == CircuitBreakerState.HALF_OPEN || failures >= config.failureThreshold()) {
                transitionToOpen();
            }
        }
    }

    public void forceOpen(String reason) {
        Objects.requireNonNull(reason, "reason");
        synchronized (this) {
            failureCount.set(Math.max(failureCount.get(), config.failureThreshold()));
            lastFailureTime = clock.instant();
            transitionToOpen();
        }
    }

    public CircuitBreakerState getState() {
        return state;
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    public Instant getLastFailureTime() {
        return lastFailureTime;
    }

    public String getServiceId() {
        return serviceId;
    }

    private boolean hasResetTimeoutElapsed() {
        if (lastFailureTime == null) {
            return true;
        }
        Instant now = clock.instant();
        return now.isAfter(lastFailureTime.plus(config.resetTimeout()));
    }

    private void transitionToOpen() {
        if (state != CircuitBreakerState.OPEN) {
            log.warn("Circuit breaker opening for service {}", serviceId);
        }
        state = CircuitBreakerState.OPEN;
        halfOpenCallCount.set(0);
    }

    private void transitionToHalfOpen() {
        log.info("Circuit breaker transitioning to HALF_OPEN for service {}", serviceId);
        state = CircuitBreakerState.HALF_OPEN;
        halfOpenCallCount.set(0);
    }
}

