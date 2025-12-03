package prototype.lineageruntime.recovery;

public record DependencyHealResult(
        String serviceId,
        String strategy,
        boolean success,
        String detail,
        ServiceSnapshot snapshot
) {

    public static DependencyHealResult fromRecovery(ServiceRecoveryAction action) {
        return new DependencyHealResult(
                action.serviceId(),
                action.action().name(),
                action.success(),
                action.detail(),
                action.snapshot()
        );
    }
}


