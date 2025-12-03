package prototype.lineageruntime.recovery;

import java.time.Instant;
import java.util.Map;

public record RecoveryWorkflowEvent(
        String workflowId,
        String serviceId,
        WorkflowStage stage,
        String status,
        String detail,
        Instant timestamp,
        Map<String, Object> metadata
) {
}


