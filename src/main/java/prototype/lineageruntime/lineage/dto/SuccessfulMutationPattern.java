package prototype.lineageruntime.lineage.dto;

public record SuccessfulMutationPattern(
        String mutationType,
        long occurrences,
        double avgPerformanceDelta,
        double avgSafetyDelta
) {
}

