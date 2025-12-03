package prototype.lineageruntime.resilience;

public class CircuitBreakerOpenException extends RuntimeException {

    public CircuitBreakerOpenException(String serviceId) {
        super("Circuit breaker is OPEN for service " + serviceId);
    }
}

