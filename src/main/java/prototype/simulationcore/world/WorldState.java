package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import prototype.simulationcore.domain.AgentState;

/**
 * Serializable capture of a world's full state at a particular tick.
 */
public record WorldState(
        UUID worldId,
        String name,
        WorldDimensions dimensions,
        long tick,
        Map<UUID, AgentState> agentStates,
        List<VirtualNodeState> nodeStates,
        EnvironmentState environmentState,
        WorldStatus status
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -4533037090585418216L;
}


