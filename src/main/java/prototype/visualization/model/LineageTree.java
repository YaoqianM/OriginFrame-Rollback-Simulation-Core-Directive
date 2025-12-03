package prototype.visualization.model;

import java.util.UUID;

/**
 * Rooted lineage tree for a given agent lineage.
 */
public record LineageTree(
        String simulationId,
        UUID rootAgentId,
        LineageTreeNode root
) {
}


