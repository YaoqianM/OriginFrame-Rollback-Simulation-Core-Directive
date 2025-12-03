package prototype.lineageruntime.controller;

import static prototype.api.doc.ApiExamples.API_ERROR_CHECKPOINT_NOT_FOUND;
import static prototype.api.doc.ApiExamples.API_ERROR_INTERNAL;
import static prototype.api.doc.ApiExamples.API_ERROR_SERVICE_NOT_FOUND;
import static prototype.api.doc.ApiExamples.API_ERROR_VALIDATION;
import static prototype.api.doc.ApiExamples.CHECKPOINT_LIST_RESPONSE;
import static prototype.api.doc.ApiExamples.CHECKPOINT_RESPONSE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prototype.api.error.ApiError;
import prototype.api.error.ApiException;
import prototype.api.error.ErrorCode;
import prototype.lineageruntime.checkpoint.CheckpointNotFoundException;
import prototype.lineageruntime.checkpoint.CheckpointService;
import prototype.lineageruntime.checkpoint.CheckpointType;
import prototype.lineageruntime.checkpoint.ServiceAdapterNotFoundException;
import prototype.lineageruntime.checkpoint.StateCheckpoint;

@Tag(name = "Runtime Checkpoints", description = "Capture, list, and restore state checkpoints for runtime services.")
@Validated
@RestController
@RequestMapping("/runtime")
public class CheckpointController {

    private static final int MAX_LIMIT = 100;

    private final CheckpointService checkpointService;

    public CheckpointController(CheckpointService checkpointService) {
        this.checkpointService = checkpointService;
    }

    @Operation(summary = "Create a checkpoint",
            description = "Captures the latest snapshot for the given service and returns the persisted checkpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Checkpoint created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StateCheckpoint.class),
                            examples = @ExampleObject(name = "CheckpointCreated", value = CHECKPOINT_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "SRV-404 Service not registered",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "ServiceNotFound", value = API_ERROR_SERVICE_NOT_FOUND))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/checkpoint/{serviceId}")
    public StateCheckpoint createCheckpoint(
            @Parameter(description = "Identifier of the service to snapshot.", example = "simulation-core", required = true)
            @PathVariable String serviceId,
            @Parameter(description = "Checkpoint strategy to use.", example = "MANUAL")
            @RequestParam(name = "type", defaultValue = "MANUAL") CheckpointType checkpointType) {
        try {
            return checkpointService.createCheckpoint(serviceId, checkpointType);
        } catch (ServiceAdapterNotFoundException ex) {
            throw new ApiException(ErrorCode.SERVICE_NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Failed to create checkpoint for service %s.".formatted(serviceId), ex);
        }
    }

    @Operation(summary = "Restore from a checkpoint",
            description = "Restores the service state from the referenced checkpoint and returns the checkpoint metadata.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Restore succeeded",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = StateCheckpoint.class),
                            examples = @ExampleObject(name = "CheckpointRestored", value = CHECKPOINT_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "CHK-404 / SRV-404 resource not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "CheckpointNotFound", value = API_ERROR_CHECKPOINT_NOT_FOUND),
                                    @ExampleObject(name = "ServiceNotFound", value = API_ERROR_SERVICE_NOT_FOUND)
                            })),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/restore/{checkpointId}")
    public StateCheckpoint restore(
            @Parameter(description = "Unique checkpoint identifier.", required = true,
                    example = "2e1f2f64-7c64-4f1a-912b-b6f5a7734246")
            @PathVariable UUID checkpointId) {
        try {
            return checkpointService.restoreFromCheckpoint(checkpointId);
        } catch (CheckpointNotFoundException ex) {
            throw new ApiException(ErrorCode.CHECKPOINT_NOT_FOUND, ex.getMessage(), ex);
        } catch (ServiceAdapterNotFoundException ex) {
            throw new ApiException(ErrorCode.SERVICE_NOT_FOUND, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Failed to restore checkpoint %s.".formatted(checkpointId), ex);
        }
    }

    @Operation(summary = "List recent checkpoints",
            description = "Returns the most recent checkpoints for a service (maximum of 100).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent checkpoints returned",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = StateCheckpoint.class)),
                            examples = @ExampleObject(name = "CheckpointList", value = CHECKPOINT_LIST_RESPONSE))),
            @ApiResponse(responseCode = "400", description = "CHK-400 Invalid limit parameter",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "ValidationError", value = API_ERROR_VALIDATION))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @GetMapping("/checkpoints/{serviceId}")
    public List<StateCheckpoint> listCheckpoints(
            @Parameter(description = "Identifier of the service whose checkpoints should be returned.",
                    example = "simulation-core", required = true)
            @PathVariable String serviceId,
            @Parameter(description = "Maximum number of checkpoints to return (1-100).", example = "10")
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        try {
            int boundedLimit = validateLimit(limit);
            return checkpointService.getRecentCheckpoints(serviceId, boundedLimit);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR,
                    "Failed to list checkpoints for service %s.".formatted(serviceId), ex);
        }
    }

    private int validateLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ApiException(ErrorCode.CHECKPOINT_LIMIT_INVALID,
                    "limit must be between 1 and %d (received %d).".formatted(MAX_LIMIT, limit));
        }
        return limit;
    }
}

