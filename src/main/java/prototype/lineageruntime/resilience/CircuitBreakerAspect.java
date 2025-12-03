package prototype.lineageruntime.resilience;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CircuitBreakerAspect {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public CircuitBreakerAspect(CircuitBreakerRegistry circuitBreakerRegistry) {
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Around("@annotation(guard)")
    public Object guardCall(ProceedingJoinPoint joinPoint, CircuitBreakerGuard guard) throws Throwable {
        String serviceId = guard.serviceId();
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.getOrCreate(serviceId);

        if (!circuitBreaker.tryAcquirePermission()) {
            throw new CircuitBreakerOpenException(serviceId);
        }

        try {
            Object result = joinPoint.proceed();
            circuitBreaker.recordSuccess();
            return result;
        } catch (Throwable throwable) {
            circuitBreaker.recordFailure();
            throw throwable;
        }
    }
}

