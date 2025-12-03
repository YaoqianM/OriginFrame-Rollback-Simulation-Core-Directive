package prototype.simulationcore.evolution.dto;

public record GenerationStats(
        double averageFitness,
        double maxFitness,
        double rewardMean,
        double rewardVariance,
        double rewardTrend
) {
    public static GenerationStats empty() {
        return new GenerationStats(0.0, 0.0, 0.0, 0.0, 0.0);
    }
}


