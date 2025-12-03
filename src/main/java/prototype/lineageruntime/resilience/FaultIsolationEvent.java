package prototype.lineageruntime.resilience;

import java.time.Instant;
import java.util.Set;

public record FaultIsolationEvent(
        String serviceId,
        Instant timestamp,
        String reason,
        CircuitBreakerState state,
        Set<String> dependentsNotified,
        boolean cascadePrevented
) {
}

