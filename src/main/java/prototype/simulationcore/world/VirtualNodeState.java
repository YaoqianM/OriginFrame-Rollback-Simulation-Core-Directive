package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.UUID;
import prototype.simulationcore.domain.Position;

/**
 * Serializable snapshot of a virtual node used in world state dumps.
 */
public record VirtualNodeState(
        UUID nodeId,
        String name,
        Position position,
        VirtualNodeStatus status,
        Map<String, Object> metadata
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -3636454439559830571L;
}


