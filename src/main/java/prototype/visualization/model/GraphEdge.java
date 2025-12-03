package prototype.visualization.model;

import java.util.Map;

/**
 * Basic edge representation for visual graphs.
 */
public record GraphEdge(
        String from,
        String to,
        String label,
        Map<String, Object> metadata
) {

    public GraphEdge {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}


