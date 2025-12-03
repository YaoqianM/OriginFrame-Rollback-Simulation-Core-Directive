package prototype.simulationcore.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import prototype.simulationcore.domain.Position;

/**
 * Captures per-agent insights for a single tick.
 */
public record AgentMetric(
        String simulationId,
        UUID agentId,
        int tick,
        Position position,
        double energy,
        double resources,
        List<String> actionsPerformed,
        double rewardThisTick,
        int violationsThisTick,
        Instant recordedAt
) {

    public AgentMetric {
        Objects.requireNonNull(simulationId, "simulationId");
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(position, "position");
        actionsPerformed = actionsPerformed == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(actionsPerformed));
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public static Builder builder(String simulationId, UUID agentId) {
        return new Builder(simulationId, agentId);
    }

    public static final class Builder {
        private final String simulationId;
        private final UUID agentId;
        private int tick;
        private Position position = Position.origin();
        private double energy;
        private double resources;
        private List<String> actions = new ArrayList<>();
        private double rewardThisTick;
        private int violationsThisTick;
        private Instant recordedAt = Instant.now();

        private Builder(String simulationId, UUID agentId) {
            this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
            this.agentId = Objects.requireNonNull(agentId, "agentId");
        }

        public Builder tick(int tick) {
            this.tick = tick;
            return this;
        }

        public Builder position(Position position) {
            this.position = position == null ? Position.origin() : position;
            return this;
        }

        public Builder energy(double energy) {
            this.energy = energy;
            return this;
        }

        public Builder resources(double resources) {
            this.resources = resources;
            return this;
        }

        public Builder action(String action) {
            if (action != null) {
                this.actions.add(action);
            }
            return this;
        }

        public Builder rewardThisTick(double reward) {
            this.rewardThisTick = reward;
            return this;
        }

        public Builder violationsThisTick(int violations) {
            this.violationsThisTick = violations;
            return this;
        }

        public Builder recordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public AgentMetric build() {
            return new AgentMetric(
                    simulationId,
                    agentId,
                    tick,
                    position,
                    energy,
                    resources,
                    actions,
                    rewardThisTick,
                    violationsThisTick,
                    recordedAt
            );
        }
    }
}

