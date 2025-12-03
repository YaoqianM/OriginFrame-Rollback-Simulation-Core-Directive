package prototype.visualization.model;

import java.time.Instant;
import java.util.List;
import prototype.simulationcore.dto.AgentDto;

/**
 * Represents a snapshot of the simulation world suitable for visualization.
 */
public record WorldSnapshot(
        String simulationId,
        long tick,
        Instant capturedAt,
        List<AgentDto> agents,
        WorldMetrics metrics,
        RenderedGraph agentNetwork
) {

    public WorldSnapshot {
        agents = agents == null ? List.of() : List.copyOf(agents);
    }
}


