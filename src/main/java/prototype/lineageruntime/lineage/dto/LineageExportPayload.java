package prototype.lineageruntime.lineage.dto;

import java.util.UUID;

public record LineageExportPayload(
        UUID rootAgentId,
        LineageGraphView graph,
        String graphJson,
        String graphml
) {
}

