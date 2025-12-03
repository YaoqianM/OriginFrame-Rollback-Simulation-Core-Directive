package prototype.api.error;

import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleApiException(ApiException exception) {
        return buildResponse(exception.getErrorCode(), exception.getMessage(), exception.getMetadata(), exception);
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public org.springframework.http.ResponseEntity<ApiError> handleValidationExceptions(Exception exception) {
        return buildResponse(ErrorCode.VALIDATION_ERROR, exception.getMessage(), Map.of(), exception);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        ErrorCode errorCode = switch (exception.getStatusCode().value()) {
            case 400 -> ErrorCode.VALIDATION_ERROR;
            case 404 -> ErrorCode.SERVICE_NOT_FOUND;
            case 409 -> ErrorCode.TRANSACTION_CONFLICT;
            case 503 -> ErrorCode.LINEAGE_STREAM_UNAVAILABLE;
            default -> ErrorCode.INTERNAL_ERROR;
        };
        return buildResponse(errorCode, exception.getReason(), Map.of(), exception);
    }

    @ExceptionHandler(Exception.class)
    public org.springframework.http.ResponseEntity<ApiError> handleFallback(Exception exception) {
        return buildResponse(ErrorCode.INTERNAL_ERROR, exception.getMessage(), Map.of(), exception);
    }

    private org.springframework.http.ResponseEntity<ApiError> buildResponse(ErrorCode errorCode,
                                                                           String message,
                                                                           Map<String, Object> metadata,
                                                                           Exception exception) {
        String traceId = UUID.randomUUID().toString();
        if (shouldLogAsError(errorCode)) {
            log.error("API error [{}] - {}", errorCode.code(), message, exception);
        } else {
            log.warn("API error [{}] - {}", errorCode.code(), message);
        }
        ApiError body = ApiError.of(errorCode, message, traceId)
                .withMetadata(metadata);
        return org.springframework.http.ResponseEntity.status(errorCode.status()).body(body);
    }

    private boolean shouldLogAsError(ErrorCode errorCode) {
        return errorCode == ErrorCode.INTERNAL_ERROR
                || errorCode == ErrorCode.RECOVERY_FAILED
                || errorCode == ErrorCode.SIMULATION_STEP_FAILED
                || errorCode == ErrorCode.ROLLBACK_FAILED
                || errorCode == ErrorCode.REPLAY_FAILED;
    }
}


