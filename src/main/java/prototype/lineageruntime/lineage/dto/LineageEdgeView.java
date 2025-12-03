package prototype.lineageruntime.lineage.dto;

import java.util.UUID;

public record LineageEdgeView(UUID parentAgentId, UUID childAgentId) {
}

