package prototype.lineageruntime.checkpoint;

/**
 * Describes why a checkpoint was captured, enabling differentiated retention policies.
 */
public enum CheckpointType {
    PERIODIC,
    MANUAL,
    PRE_OPERATION
}

