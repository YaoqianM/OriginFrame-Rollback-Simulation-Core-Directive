package prototype.simulationcore.evolution.domain;

public record RewardStats(
        double mean,
        double variance,
        double trend
) {
    public static RewardStats empty() {
        return new RewardStats(0.0, 0.0, 0.0);
    }
}


