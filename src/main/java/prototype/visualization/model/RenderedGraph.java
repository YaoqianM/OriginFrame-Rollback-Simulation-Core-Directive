package prototype.visualization.model;

import java.util.List;

/**
 * Container for graph nodes and edges used by frontend visualizations.
 */
public record RenderedGraph(
        List<GraphNode> nodes,
        List<GraphEdge> edges
) {

    public RenderedGraph {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        edges = edges == null ? List.of() : List.copyOf(edges);
    }
}


