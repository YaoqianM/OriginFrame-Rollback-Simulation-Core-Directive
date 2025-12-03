package prototype.lineageruntime.lineage.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LineageGraphView(
        UUID rootAgentId,
        List<LineageNodeView> nodes,
        List<LineageEdgeView> edges,
        Map<Integer, LineageGenerationStats> generationStats
) {
}

