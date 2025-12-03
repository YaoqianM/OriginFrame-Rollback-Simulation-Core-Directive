package prototype.simulationcore.safety.monitor;

import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.safety.Severity;

public record SafetyViolationEvent(
        UUID agentId,
        String constraintType,
        Severity severity,
        String action,
        String message,
        int generation,
        Instant timestamp
) {
}

