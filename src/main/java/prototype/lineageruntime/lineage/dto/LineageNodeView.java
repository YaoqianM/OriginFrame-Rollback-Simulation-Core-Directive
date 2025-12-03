package prototype.lineageruntime.lineage.dto;

import java.util.Map;
import java.util.UUID;
import prototype.lineageruntime.lineage.domain.AgentLineage;

public record LineageNodeView(
        UUID agentId,
        UUID lineageId,
        UUID parentId,
        int generation,
        double performanceScore,
        double safetyScore,
        int survivedGenerations,
        String eliminationReason,
        Map<String, Object> metadata
) {

    public LineageNodeView {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static LineageNodeView from(AgentLineage lineage) {
        return new LineageNodeView(
                lineage.getAgentId(),
                lineage.getLineageId(),
                lineage.getParentId(),
                lineage.getGeneration(),
                lineage.getPerformanceScore(),
                lineage.getSafetyScore(),
                lineage.getSurvivedGenerations(),
                lineage.getEliminationReason(),
                lineage.getMetadata()
        );
    }
}

