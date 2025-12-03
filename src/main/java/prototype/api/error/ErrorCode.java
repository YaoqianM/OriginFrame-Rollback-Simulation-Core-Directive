package prototype.api.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    TRANSACTION_NOT_FOUND("TRX-404", HttpStatus.NOT_FOUND, "Transaction not found."),
    TRANSACTION_CONFLICT("TRX-409", HttpStatus.CONFLICT, "Transaction state prevents the requested operation."),
    CHECKPOINT_NOT_FOUND("CHK-404", HttpStatus.NOT_FOUND, "Checkpoint was not found."),
    CHECKPOINT_LIMIT_INVALID("CHK-400", HttpStatus.BAD_REQUEST, "Checkpoint limit must be between 1 and 100."),
    SERVICE_NOT_FOUND("SRV-404", HttpStatus.NOT_FOUND, "Service is not registered in the runtime."),
    RECOVERY_FAILED("RCV-500", HttpStatus.INTERNAL_SERVER_ERROR, "Recovery workflow failed to complete."),
    SIMULATION_STEP_FAILED("SIM-500", HttpStatus.INTERNAL_SERVER_ERROR, "Simulation step failed."),
    ROLLBACK_FAILED("SIM-409", HttpStatus.CONFLICT, "Rollback could not be applied to the current agent state."),
    REPLAY_FAILED("SIM-502", HttpStatus.BAD_GATEWAY, "Replay could not rebuild the agent state."),
    LINEAGE_STREAM_UNAVAILABLE("LIN-503", HttpStatus.SERVICE_UNAVAILABLE, "Lineage history is temporarily unavailable."),
    VALIDATION_ERROR("VAL-400", HttpStatus.BAD_REQUEST, "Request validation failed."),
    INTERNAL_ERROR("GEN-500", HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");

    private final String code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(String code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
