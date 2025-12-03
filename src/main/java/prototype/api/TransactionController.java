package prototype.api;

import static prototype.api.doc.ApiExamples.API_ERROR_INTERNAL;
import static prototype.api.doc.ApiExamples.API_ERROR_TRANSACTION_NOT_FOUND;
import static prototype.api.doc.ApiExamples.TRANSACTION_RESPONSE;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.api.dto.TransactionResponse;
import prototype.api.error.ApiError;
import prototype.api.error.ApiException;
import prototype.api.error.ErrorCode;
import prototype.lineageruntime.transaction.RollbackManager;

@Tag(name = "Transactions", description = "Rollback-aware transaction orchestration APIs.")
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final RollbackManager rollbackManager;

    public TransactionController(RollbackManager rollbackManager) {
        this.rollbackManager = rollbackManager;
    }

    @Operation(summary = "Begin a new transaction",
            description = "Creates a new lineage-aware transaction and returns the initial log entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction started",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "TransactionCreated", value = TRANSACTION_RESPONSE))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping
    public ResponseEntity<TransactionResponse> begin() {
        UUID transactionId = rollbackManager.beginTransaction();
        return ResponseEntity.ok(loadTransaction(transactionId));
    }

    @Operation(summary = "Commit a transaction",
            description = "Marks the transaction as committed and returns the final log.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction committed",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "TransactionCommitted", value = TRANSACTION_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "TRX-404 Transaction not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "TransactionNotFound", value = API_ERROR_TRANSACTION_NOT_FOUND))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/{transactionId}/commit")
    public ResponseEntity<TransactionResponse> commit(
            @Parameter(description = "Unique transaction identifier.", required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID transactionId) {
        try {
            rollbackManager.commit(transactionId);
            return ResponseEntity.ok(loadTransaction(transactionId));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.TRANSACTION_NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @Operation(summary = "Rollback a transaction",
            description = "Executes compensating actions (if any) and marks the transaction as rolled back.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction rolled back",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "TransactionRolledBack", value = TRANSACTION_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "TRX-404 Transaction not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "TransactionNotFound", value = API_ERROR_TRANSACTION_NOT_FOUND))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @PostMapping("/{transactionId}/rollback")
    public ResponseEntity<TransactionResponse> rollback(
            @Parameter(description = "Unique transaction identifier.", required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID transactionId) {
        try {
            rollbackManager.rollback(transactionId);
            return ResponseEntity.ok(loadTransaction(transactionId));
        } catch (IllegalArgumentException ex) {
            throw new ApiException(ErrorCode.TRANSACTION_NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @Operation(summary = "Fetch a transaction log",
            description = "Returns the latest state of a specific transaction.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transaction found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TransactionResponse.class),
                            examples = @ExampleObject(name = "TransactionResponse", value = TRANSACTION_RESPONSE))),
            @ApiResponse(responseCode = "404", description = "TRX-404 Transaction not found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "TransactionNotFound", value = API_ERROR_TRANSACTION_NOT_FOUND))),
            @ApiResponse(responseCode = "500", description = "GEN-500 Unexpected server error",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "InternalError", value = API_ERROR_INTERNAL)))
    })
    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> find(
            @Parameter(description = "Unique transaction identifier.", required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(loadTransaction(transactionId));
    }

    private TransactionResponse loadTransaction(UUID transactionId) {
        return rollbackManager.find(transactionId)
                .map(TransactionResponse::from)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.TRANSACTION_NOT_FOUND,
                        "Transaction %s was not found.".formatted(transactionId)));
    }
}

