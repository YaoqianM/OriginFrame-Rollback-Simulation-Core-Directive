package prototype.visualization.model;

import java.time.Instant;

/**
 * Represents a value plotted on the simulation timeline.
 */
public record TimelinePoint(
        String simulationId,
        long index,
        Instant timestamp,
        String agentId,
        double energy,
        double resources
) {
}


