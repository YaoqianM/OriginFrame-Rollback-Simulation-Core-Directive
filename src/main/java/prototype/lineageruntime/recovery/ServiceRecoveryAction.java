package prototype.lineageruntime.recovery;

public record ServiceRecoveryAction(
        String serviceId,
        RecoveryActionType action,
        int attempts,
        boolean success,
        String detail,
        ServiceSnapshot snapshot
) {
}


