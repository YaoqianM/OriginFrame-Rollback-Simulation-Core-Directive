package prototype.lineageruntime.recovery;

import java.time.Instant;

public record RollbackEvent(
        String workflowId,
        String serviceId,
        String agentId,
        double previousEnergy,
        double resultingEnergy,
        Instant timestamp,
        String detail
) {
}


