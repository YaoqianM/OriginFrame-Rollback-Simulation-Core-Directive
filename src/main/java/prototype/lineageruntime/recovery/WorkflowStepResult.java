package prototype.lineageruntime.recovery;

import java.time.Instant;

public record WorkflowStepResult(
        WorkflowStage stage,
        boolean success,
        String detail,
        Instant occurredAt
) {
}


