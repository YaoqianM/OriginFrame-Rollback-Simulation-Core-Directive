package prototype.visualization.model;

import java.time.Instant;
import prototype.simulationcore.domain.Position;

/**
 * Represents a single waypoint in an agent's trail.
 */
public record TrailPoint(
        Instant timestamp,
        Position position,
        double energy,
        double resources
) {
}


