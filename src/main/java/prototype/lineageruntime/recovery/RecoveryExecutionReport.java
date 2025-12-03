package prototype.lineageruntime.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(name = "RecoveryExecutionReport", description = "Detailed telemetry emitted after a recovery workflow finishes.")
public record RecoveryExecutionReport(
        @Schema(description = "Workflow correlation identifier.", example = "a4c2e6db-9d42-4a5b-9304-2a8c94a64c70")
        String workflowId,
        @Schema(description = "Recovered service identifier.", example = "ingestion-service")
        String serviceId,
        @Schema(description = "Start time of the workflow.", example = "2025-05-05T10:30:00Z")
        Instant startedAt,
        @Schema(description = "Completion time of the workflow.", example = "2025-05-05T10:31:45Z")
        Instant completedAt,
        @Schema(description = "Whether the workflow completed successfully.")
        boolean success,
        @Schema(description = "Human-readable status message.", example = "Recovery workflow completed successfully")
        String message,
        @Schema(description = "Final snapshot of the service after recovery.")
        ServiceSnapshot finalSnapshot,
        @Schema(description = "Timeline of each workflow step.")
        List<WorkflowStepResult> steps,
        @Schema(description = "Impacted downstream services.")
        List<DependencyImpact> impactedServices,
        @Schema(description = "Dependency healing actions performed as part of recovery.")
        List<DependencyHealResult> dependencyActions
) {

    public boolean overallSuccess() {
        return success;
    }
}

