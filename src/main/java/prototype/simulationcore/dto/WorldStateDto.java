package prototype.simulationcore.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.world.EnvironmentState;
import prototype.simulationcore.world.WorldDimensions;
import prototype.simulationcore.world.WorldState;
import prototype.simulationcore.world.WorldStatus;
import prototype.simulationcore.world.VirtualNodeState;

public record WorldStateDto(
        UUID worldId,
        String name,
        WorldDimensions dimensions,
        long tick,
        Map<UUID, AgentState> agentStates,
        List<VirtualNodeState> nodeStates,
        EnvironmentState environmentState,
        WorldStatus status
) {

    public static WorldStateDto from(WorldState state) {
        return new WorldStateDto(
                state.worldId(),
                state.name(),
                state.dimensions(),
                state.tick(),
                state.agentStates(),
                state.nodeStates(),
                state.environmentState(),
                state.status()
        );
    }
}


