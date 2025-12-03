package prototype.lineageruntime.lineage.dto;

public record LineageGenerationStats(
        int generation,
        long agentCount,
        double averagePerformanceScore,
        double averageSafetyScore,
        long survivors,
        long eliminated
) {
}

