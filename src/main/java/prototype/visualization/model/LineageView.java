package prototype.visualization.model;

/**
 * Bundles a lineage tree with its rendered graph view.
 */
public record LineageView(
        LineageTree tree,
        RenderedGraph graph
) {
}


