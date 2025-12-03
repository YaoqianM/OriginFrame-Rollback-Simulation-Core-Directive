package prototype.simulationcore.evolution.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prototype.simulationcore.evolution.domain.RewardTrajectory;
import prototype.simulationcore.evolution.dto.LeaderboardEntry;
import prototype.simulationcore.evolution.repository.RewardTrajectoryRepository;

@ExtendWith(MockitoExtension.class)
class RewardTrackerTest {

    @Mock
    private RewardTrajectoryRepository repository;

    @InjectMocks
    private RewardTracker rewardTracker;

    @Test
    void computesAggregatedStats() {
        UUID agentId = UUID.randomUUID();
        RewardTrajectory trajectory = new RewardTrajectory(agentId);
        trajectory.record(1.0, 0L);
        trajectory.record(3.0, 1L);
        when(repository.findByAgentId(agentId)).thenReturn(Optional.of(trajectory));

        var stats = rewardTracker.getAggregatedStats(agentId);

        assertThat(stats.mean()).isEqualTo(2.0);
        assertThat(stats.variance()).isEqualTo(1.0);
        assertThat(stats.trend()).isGreaterThan(0.0);
    }

    @Test
    void buildsLeaderboardEntries() {
        RewardTrajectory trajectory = new RewardTrajectory(UUID.randomUUID());
        trajectory.record(2.5, 0L);
        when(repository.findTop10ByOrderByCumulativeRewardDesc()).thenReturn(List.of(trajectory));

        List<LeaderboardEntry> leaderboard = rewardTracker.leaderboard(5);

        assertThat(leaderboard).hasSize(1);
        assertThat(leaderboard.get(0).cumulativeReward()).isEqualTo(2.5);
        assertThat(leaderboard.get(0).meanReward()).isEqualTo(2.5);
    }
}


