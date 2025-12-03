package prototype.visualization.model;

import java.util.List;
import java.util.UUID;

/**
 * Historical path for a single agent.
 */
public record AgentTrail(
        String simulationId,
        UUID agentId,
        List<TrailPoint> waypoints
) {

    public AgentTrail {
        waypoints = waypoints == null ? List.of() : List.copyOf(waypoints);
    }
}


