package prototype.simulationcore.controller;

import static prototype.api.doc.ApiExamples.AGENT_RESPONSE;
import static prototype.api.doc.ApiExamples.API_ERROR_INTERNAL;
import static prototype.api.doc.ApiExamples.API_ERROR_LINEAGE_UNAVAILABLE;
import static prototype.api.doc.ApiExamples.API_ERROR_REPLAY;
import static prototype.api.doc.ApiExamples.API_ERROR_ROLLBACK;
import static prototype.api.doc.ApiExamples.API_ERROR_SIMULATION;
import static prototype.api.doc.ApiExamples.LINEAGE_HISTORY;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.api.error.ApiError;
import prototype.api.error.ApiException;
import prototype.api.error.ErrorCode;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.lineageruntime.model.LineageRecord;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.dto.AgentDto;
import prototype.simulationcore.service.ReplayService;
import prototype.simulationcore.service.RollbackService;
import prototype.simulationcore.service.SimulationService;

@Tag(name = "Simulation", description = "Lifecycle operations for digital agents and lineage history.")
@RestController
@RequestMapping("/simulate")
public class SimulationController {

    private final SimulationService simulationService;
    private final RollbackService rollbackService;
    private final ReplayService replayService;
    private final EventConsumer eventConsumer;

    public SimulationController(SimulationService simulationService,
                                RollbackService rollbackService,
                                ReplayService replayService,
                                EventConsumer eventConsumer) {
        this.simulationService = simulationService;
        this.rollbackService = rollbackService;
        this.replayService = replayService;
        this.eventConsumer = eventConsumer;
    }

    @Operation(summary = "Advance the simulation by one step",
            description = "Evolves the active agent according to its policy and returns the updated state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent advanced",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AgentDto.class),
                            examples = @ExampleObject(name = "AgentState", value = AGENT_RESPONSE))),
            @ApiResponse(responseCode = "500", description = "SIM-500 Simulation step failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "SimulationError", value = API_ERROR_SIMULATION)))
    })
    @PostMapping("/step")
    public AgentDto step() {
        return executeAgentAction(simulationService::step, ErrorCode.SIMULATION_STEP_FAILED,
                "Failed to advance the simulation step.");
    }

    @Operation(summary = "Rollback the agent state",
            description = "Reverts the agent to the most recent checkpoint captured by the rollback service.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent rolled back",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AgentDto.class),
                            examples = @ExampleObject(name = "AgentState", value = AGENT_RESPONSE))),
            @ApiResponse(responseCode = "409", description = "SIM-409 Rollback could not be applied",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "RollbackError", value = API_ERROR_ROLLBACK))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/rollback")
    public AgentDto rollback() {
        return executeAgentAction(rollbackService::rollback, ErrorCode.ROLLBACK_FAILED,
                "Failed to rollback the agent state.");
    }

    @Operation(summary = "Replay the full lineage history",
            description = "Rebuilds the agent state from the lineage log and returns the reconstructed agent.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Agent reconstructed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AgentDto.class),
                            examples = @ExampleObject(name = "AgentState", value = AGENT_RESPONSE))),
            @ApiResponse(responseCode = "502", description = "SIM-502 Replay failed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "ReplayError", value = API_ERROR_REPLAY))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/replay")
    public AgentDto replay() {
        return executeAgentAction(replayService::replay, ErrorCode.REPLAY_FAILED,
                "Failed to replay the agent from lineage events.");
    }

    @Operation(summary = "Retrieve lineage history",
            description = "Returns the recent lineage events for audit and debugging.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lineage history returned",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = LineageRecord.class)),
                            examples = @ExampleObject(name = "LineageHistory", value = LINEAGE_HISTORY))),
            @ApiResponse(responseCode = "503", description = "LIN-503 Lineage stream unavailable",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "LineageUnavailable", value = API_ERROR_LINEAGE_UNAVAILABLE))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @GetMapping("/history")
    public List<LineageRecord> history() {
        try {
            return eventConsumer.getHistoryView();
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.LINEAGE_STREAM_UNAVAILABLE,
                    "Lineage history is temporarily unavailable.", ex);
        }
    }

    private AgentDto executeAgentAction(Supplier<Agent> action, ErrorCode failureCode, String fallbackMessage) {
        try {
            return AgentDto.from(action.get());
        } catch (IllegalStateException | IllegalArgumentException ex) {
            throw new ApiException(failureCode, ex.getMessage(), ex);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, fallbackMessage, ex);
        }
    }
}

