package prototype.lineageruntime.checkpoint;

/**
 * Abstraction that knows how to capture and restore service state snapshots.
 */
public interface ServiceStateAdapter {

    /**
     * @return unique service identifier used across checkpointing operations.
     */
    String serviceId();

    /**
     * @return serialized representation of the current state.
     */
    String captureSnapshot();

    /**
     * Restores the service to the serialized state snapshot.
     *
     * @param snapshot serialized payload previously produced by {@link #captureSnapshot()}.
     */
    void restoreFromSnapshot(String snapshot);
}

