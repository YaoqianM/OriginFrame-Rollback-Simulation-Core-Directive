package prototype.simulationcore.evolution.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;

@Embeddable
public class RewardPoint {

    @Column(name = "tick_mark")
    private long tick;

    @Column(name = "reward_value")
    private double reward;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    public RewardPoint() {
    }

    public RewardPoint(long tick, double reward, Instant recordedAt) {
        this.tick = tick;
        this.reward = reward;
        this.recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public long getTick() {
        return tick;
    }

    public double getReward() {
        return reward;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}


