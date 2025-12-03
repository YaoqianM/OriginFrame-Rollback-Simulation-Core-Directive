package prototype.lineageruntime.lineage.dto;

import java.util.List;

public record LineageComparison(
        LineageNodeView first,
        LineageNodeView second,
        double performanceGap,
        double safetyGap,
        int generationGap,
        List<String> sharedMutationTypes
) {
}

