package prototype.simulationcore.evolution.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "reward_trajectories")
public class RewardTrajectory {

    @Id
    @GeneratedValue
    @Column(name = "trajectory_id", nullable = false, updatable = false)
    private UUID trajectoryId;

    @Column(name = "agent_id", nullable = false, unique = true)
    private UUID agentId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reward_points", joinColumns = @JoinColumn(name = "trajectory_id"))
    @OrderColumn(name = "point_index")
    private List<RewardPoint> rewards = new ArrayList<>();

    @Column(name = "cumulative_reward", nullable = false)
    private double cumulativeReward;

    @Column(name = "generation_rank")
    private Integer generationRank;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public RewardTrajectory() {
    }

    public RewardTrajectory(UUID agentId) {
        this.agentId = agentId;
        this.updatedAt = Instant.now();
    }

    public UUID getTrajectoryId() {
        return trajectoryId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public List<RewardPoint> getRewards() {
        return List.copyOf(rewards);
    }

    public double getCumulativeReward() {
        return cumulativeReward;
    }

    public Integer getGenerationRank() {
        return generationRank;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void record(double reward, long tick) {
        if (rewards == null) {
            rewards = new ArrayList<>();
        }
        rewards.add(new RewardPoint(tick, reward, Instant.now()));
        cumulativeReward += reward;
        updatedAt = Instant.now();
    }

    public void updateGenerationRank(int rank) {
        generationRank = rank;
        updatedAt = Instant.now();
    }
}


