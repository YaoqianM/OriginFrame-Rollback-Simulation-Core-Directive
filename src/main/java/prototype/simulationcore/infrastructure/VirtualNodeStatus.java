package prototype.simulationcore.infrastructure;

/**
 * Health status of a virtual node. Used to drive latency/packet-loss simulation and Project A hooks.
 */
public enum VirtualNodeStatus {
    HEALTHY,
    DEGRADED,
    FAILED,
    UNKNOWN;

    public boolean isAvailable() {
        return this == HEALTHY || this == DEGRADED;
    }
}


