package prototype.simulationcore.evolution.dto;

import java.util.UUID;

public record LeaderboardEntry(
        UUID agentId,
        double cumulativeReward,
        double meanReward,
        double rewardVariance,
        double rewardTrend,
        Integer generationRank
) {
}


