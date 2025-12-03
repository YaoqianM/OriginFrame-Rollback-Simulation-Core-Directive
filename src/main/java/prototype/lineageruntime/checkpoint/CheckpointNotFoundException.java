package prototype.lineageruntime.checkpoint;

import java.util.UUID;

public class CheckpointNotFoundException extends RuntimeException {

    public CheckpointNotFoundException(UUID checkpointId) {
        super("Checkpoint %s was not found.".formatted(checkpointId));
    }
}


