package prototype.integration.events;

import java.time.Instant;
import java.util.List;
import prototype.simulationcore.infrastructure.FaultInjector;

public record GridRecoveryEvent(
        String scenarioId,
        String nodeId,
        String serviceId,
        boolean recoveryTriggered,
        boolean recovered,
        String message,
        List<FaultInjector.RecoveryWorkflowSummary> recoveries,
        Instant timestamp
) {
}


