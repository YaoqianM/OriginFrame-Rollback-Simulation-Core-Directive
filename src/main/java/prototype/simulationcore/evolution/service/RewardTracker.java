package prototype.simulationcore.evolution.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import prototype.simulationcore.evolution.domain.RewardPoint;
import prototype.simulationcore.evolution.domain.RewardStats;
import prototype.simulationcore.evolution.domain.RewardTrajectory;
import prototype.simulationcore.evolution.dto.LeaderboardEntry;
import prototype.simulationcore.evolution.repository.RewardTrajectoryRepository;

@Service
public class RewardTracker {

    private final RewardTrajectoryRepository repository;

    public RewardTracker(RewardTrajectoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void recordReward(UUID agentId, double reward, long tick) {
        if (agentId == null) {
            return;
        }
        RewardTrajectory trajectory = resolve(agentId);
        trajectory.record(reward, tick);
        repository.save(trajectory);
    }

    @Transactional
    public void recordReward(UUID agentId, double reward) {
        recordReward(agentId, reward, Instant.now().toEpochMilli());
    }

    @Transactional
    public void updateGenerationRank(UUID agentId, int rank) {
        if (agentId == null) {
            return;
        }
        RewardTrajectory trajectory = resolve(agentId);
        trajectory.updateGenerationRank(rank);
        repository.save(trajectory);
    }

    public List<RewardPoint> getTrajectory(UUID agentId) {
        return repository.findByAgentId(agentId)
                .map(RewardTrajectory::getRewards)
                .orElse(Collections.emptyList());
    }

    public RewardStats getAggregatedStats(UUID agentId) {
        return repository.findByAgentId(agentId)
                .map(this::toStats)
                .orElse(RewardStats.empty());
    }

    public double getCumulativeReward(UUID agentId) {
        return repository.findByAgentId(agentId)
                .map(RewardTrajectory::getCumulativeReward)
                .orElse(0.0);
    }

    public List<LeaderboardEntry> leaderboard(int limit) {
        int effectiveLimit = Math.max(1, limit);
        return repository.findTop10ByOrderByCumulativeRewardDesc().stream()
                .limit(effectiveLimit)
                .map(this::toLeaderboardEntry)
                .toList();
    }

    private RewardTrajectory resolve(UUID agentId) {
        return repository.findByAgentId(agentId)
                .orElseGet(() -> new RewardTrajectory(agentId));
    }

    private LeaderboardEntry toLeaderboardEntry(RewardTrajectory trajectory) {
        RewardStats stats = toStats(trajectory);
        return new LeaderboardEntry(
                trajectory.getAgentId(),
                trajectory.getCumulativeReward(),
                stats.mean(),
                stats.variance(),
                stats.trend(),
                trajectory.getGenerationRank()
        );
    }

    private RewardStats toStats(RewardTrajectory trajectory) {
        List<RewardPoint> points = trajectory.getRewards();
        if (points.isEmpty()) {
            return RewardStats.empty();
        }
        double mean = points.stream()
                .mapToDouble(RewardPoint::getReward)
                .average()
                .orElse(0.0);
        double variance = points.stream()
                .mapToDouble(point -> Math.pow(point.getReward() - mean, 2))
                .average()
                .orElse(0.0);
        double trend = computeTrend(points, mean);
        return new RewardStats(mean, variance, trend);
    }

    private double computeTrend(List<RewardPoint> points, double meanReward) {
        if (points.size() < 2) {
            return 0.0;
        }
        double meanTick = points.stream().mapToDouble(RewardPoint::getTick).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;
        for (RewardPoint point : points) {
            double tickDelta = point.getTick() - meanTick;
            numerator += tickDelta * (point.getReward() - meanReward);
            denominator += tickDelta * tickDelta;
        }
        return denominator == 0.0 ? 0.0 : numerator / denominator;
    }
}


