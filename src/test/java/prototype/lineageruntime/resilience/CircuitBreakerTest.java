package prototype.lineageruntime.resilience;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class CircuitBreakerTest {

    @Test
    void transitionsAcrossStates() throws InterruptedException {
        CircuitBreakerConfig config = new CircuitBreakerConfig(2, Duration.ofMillis(40), 1);
        CircuitBreaker breaker = new CircuitBreaker("svc", config);

        assertTrue(breaker.tryAcquirePermission());
        breaker.recordFailure();
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());

        breaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());

        assertFalse(breaker.tryAcquirePermission());

        Thread.sleep(50);
        assertTrue(breaker.tryAcquirePermission());
        assertEquals(CircuitBreakerState.HALF_OPEN, breaker.getState());

        breaker.recordSuccess();
        assertEquals(CircuitBreakerState.CLOSED, breaker.getState());
    }

    @Test
    void failureWhileHalfOpenReopensBreaker() throws InterruptedException {
        CircuitBreakerConfig config = new CircuitBreakerConfig(1, Duration.ofMillis(20), 1);
        CircuitBreaker breaker = new CircuitBreaker("svc", config);

        breaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());

        Thread.sleep(25);
        assertTrue(breaker.tryAcquirePermission());
        assertEquals(CircuitBreakerState.HALF_OPEN, breaker.getState());

        breaker.recordFailure();
        assertEquals(CircuitBreakerState.OPEN, breaker.getState());
    }
}

