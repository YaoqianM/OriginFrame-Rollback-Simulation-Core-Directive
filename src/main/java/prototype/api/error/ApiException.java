package prototype.api.error;

import java.util.Map;

public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> metadata;

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null, null);
    }

    public ApiException(ErrorCode errorCode, String message, Throwable cause) {
        this(errorCode, message, null, cause);
    }

    public ApiException(ErrorCode errorCode, String message, Map<String, Object> metadata, Throwable cause) {
        super(message == null ? errorCode.message() : message, cause);
        this.errorCode = errorCode;
        this.metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}


