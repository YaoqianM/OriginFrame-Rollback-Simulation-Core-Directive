package prototype.lineageruntime.recovery;

public enum ServiceStatus {
    HEALTHY,
    DEGRADED,
    RESTARTING,
    RECOVERING,
    ISOLATED,
    FAILED,
    UNKNOWN;

    public boolean isHealthy() {
        return this == HEALTHY || this == DEGRADED;
    }

    public boolean isTerminalFailure() {
        return this == FAILED;
    }
}


