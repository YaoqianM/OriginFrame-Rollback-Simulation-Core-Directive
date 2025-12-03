package prototype.visualization.model;

import java.util.Map;

/**
 * Basic node representation for rendered graphs consumed by the UI.
 */
public record GraphNode(
        String id,
        String label,
        Map<String, Object> metadata
) {

    public GraphNode {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}


