package prototype.lineageruntime.controller;

import static prototype.api.doc.ApiExamples.API_ERROR_RECOVERY_FAILED;
import static prototype.api.doc.ApiExamples.API_ERROR_SERVICE_NOT_FOUND;
import static prototype.api.doc.ApiExamples.RECOVERY_REPORT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.api.error.ApiError;
import prototype.api.error.ApiException;
import prototype.api.error.ErrorCode;
import prototype.lineageruntime.recovery.RecoveryExecutionReport;
import prototype.lineageruntime.recovery.RecoveryWorkflowOrchestrator;

@Tag(name = "Runtime Recovery", description = "Trigger autonomous recovery workflows for registered services.")
@RestController
@RequestMapping("/runtime")
public class RuntimeRecoveryController {

    private final RecoveryWorkflowOrchestrator orchestrator;

    public RuntimeRecoveryController(RecoveryWorkflowOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Operation(summary = "Execute the recovery workflow for a service",
            description = "Runs the detect → isolate → rollback → recover → validate pipeline for the provided service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recovery workflow completed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RecoveryExecutionReport.class),
                            examples = @ExampleObject(name = "RecoveryReport", value = RECOVERY_REPORT))),
            @ApiResponse(responseCode = "404", description = "SRV-404 Service not registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "ServiceNotFound", value = API_ERROR_SERVICE_NOT_FOUND))),
            @ApiResponse(responseCode = "500", description = "RCV-500 Recovery failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "RecoveryFailed", value = API_ERROR_RECOVERY_FAILED)))
    })
    @PostMapping("/recover/{serviceId}")
    public ResponseEntity<RecoveryExecutionReport> recover(
            @Parameter(description = "Identifier of the service that should be recovered.", example = "ingestion-service")
            @PathVariable String serviceId) {
        try {
            return ResponseEntity.ok(orchestrator.recover(serviceId));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.SERVICE_NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.RECOVERY_FAILED,
                    "Recovery workflow failed for service %s.".formatted(serviceId), ex);
        }
    }
}

