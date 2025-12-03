package prototype.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import prototype.lineageruntime.transaction.TransactionLog;
import prototype.lineageruntime.transaction.TransactionStatus;

@Schema(name = "TransactionResponse", description = "Snapshot of a lineage-aware transaction log.")
public record TransactionResponse(
        @Schema(description = "Stable transaction identifier.", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID transactionId,
        @Schema(description = "Current transaction status.", example = "COMMITTED")
        TransactionStatus status,
        @Schema(description = "Ordered list of operations executed within the transaction.")
        List<String> operations,
        @Schema(description = "Compensating actions that can be replayed to rollback the transaction.")
        List<String> compensatingActions,
        @Schema(description = "Timestamp when the transaction was created.", example = "2025-05-05T10:15:30Z")
        Instant createdAt,
        @Schema(description = "Timestamp when the transaction completed, if applicable.", example = "2025-05-05T10:16:02Z")
        Instant completedAt
) {

    public static TransactionResponse from(TransactionLog log) {
        return new TransactionResponse(
                log.getTransactionId(),
                log.getStatus(),
                log.getOperationSequence(),
                log.getCompensatingActions(),
                log.getCreatedAt(),
                log.getCompletedAt()
        );
    }
}

