package prototype.lineageruntime.recovery;

public record FailoverAction(
        String serviceId,
        String fallbackServiceId,
        boolean success,
        String detail,
        ServiceSnapshot snapshot
) {
}


