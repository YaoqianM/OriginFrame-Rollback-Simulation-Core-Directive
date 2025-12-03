package prototype.api.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(name = "ApiError", description = "Standard problem response returned when an API request fails.")
public record ApiError(
        @Schema(description = "Stable machine-readable error code.", example = "TRX-404")
        String code,
        @Schema(description = "HTTP status code associated with the error.", example = "404")
        int status,
        @Schema(description = "HTTP reason phrase for the status code.", example = "Not Found")
        String reason,
        @Schema(description = "Human-readable detail about what went wrong.", example = "Transaction 52a7d... not found.")
        String message,
        @Schema(description = "Correlation identifier that can be used to trace the failure in logs.", example = "9db528cc-4545-4f0c-97f3-4dfc9442c9c3")
        String traceId,
        @Schema(description = "Timestamp indicating when the error was generated.", example = "2025-05-01T14:12:00Z")
        Instant timestamp,
        @Schema(description = "Optional metadata that can help with diagnostics.")
        Map<String, Object> metadata
) {

    public ApiError {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static ApiError of(ErrorCode errorCode, String message, String traceId) {
        return new ApiError(
                errorCode.code(),
                errorCode.status().value(),
                errorCode.status().getReasonPhrase(),
                message == null ? errorCode.message() : message,
                traceId,
                Instant.now(),
                Map.of()
        );
    }

    public ApiError withMetadata(Map<String, Object> additionalMetadata) {
        return new ApiError(
                code,
                status,
                reason,
                message,
                traceId,
                timestamp,
                additionalMetadata == null ? Map.of() : Map.copyOf(additionalMetadata)
        );
    }
}


