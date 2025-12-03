package prototype.simulationcore.metrics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;

/**
 * Immutable snapshot of the simulation world for a single tick.
 */
public final class SimulationWorldSnapshot {

    private final String simulationId;
    private final int tick;
    private final Instant capturedAt;
    private final List<Agent> agents;
    private final Map<UUID, Action> actionsPerformed;
    private final Map<UUID, Double> rewards;
    private final Map<UUID, Integer> violationDeltas;
    private final int healthyNodes;
    private final int failedNodes;
    private final double networkLatencyAvg;

    private SimulationWorldSnapshot(Builder builder) {
        this.simulationId = builder.simulationId;
        this.tick = builder.tick;
        this.capturedAt = builder.capturedAt == null ? Instant.now() : builder.capturedAt;
        this.agents = List.copyOf(builder.agents);
        this.actionsPerformed = Map.copyOf(builder.actionsPerformed);
        this.rewards = Map.copyOf(builder.rewards);
        this.violationDeltas = Map.copyOf(builder.violationDeltas);
        this.healthyNodes = builder.healthyNodes;
        this.failedNodes = builder.failedNodes;
        this.networkLatencyAvg = builder.networkLatencyAvg;
    }

    public String simulationId() {
        return simulationId;
    }

    public int tick() {
        return tick;
    }

    public Instant capturedAt() {
        return capturedAt;
    }

    public List<Agent> agents() {
        return agents;
    }

    public Map<UUID, Action> actionsPerformed() {
        return actionsPerformed;
    }

    public Map<UUID, Double> rewards() {
        return rewards;
    }

    public Map<UUID, Integer> violationDeltas() {
        return violationDeltas;
    }

    public int healthyNodes() {
        return healthyNodes;
    }

    public int failedNodes() {
        return failedNodes;
    }

    public double networkLatencyAvg() {
        return networkLatencyAvg;
    }

    public static Builder builder(String simulationId) {
        return new Builder(simulationId);
    }

    public static final class Builder {
        private final String simulationId;
        private int tick;
        private Instant capturedAt;
        private List<Agent> agents = new ArrayList<>();
        private Map<UUID, Action> actionsPerformed = new HashMap<>();
        private Map<UUID, Double> rewards = new HashMap<>();
        private Map<UUID, Integer> violationDeltas = new HashMap<>();
        private int healthyNodes = 0;
        private int failedNodes = 0;
        private double networkLatencyAvg = 0.0;

        private Builder(String simulationId) {
            this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        }

        public Builder tick(int tick) {
            this.tick = tick;
            return this;
        }

        public Builder capturedAt(Instant capturedAt) {
            this.capturedAt = capturedAt;
            return this;
        }

        public Builder agents(List<Agent> agents) {
            this.agents = agents == null ? Collections.emptyList() : new ArrayList<>(agents);
            return this;
        }

        public Builder action(UUID agentId, Action action) {
            if (agentId != null && action != null) {
                this.actionsPerformed.put(agentId, action);
            }
            return this;
        }

        public Builder reward(UUID agentId, double rewardDelta) {
            if (agentId != null) {
                this.rewards.put(agentId, rewardDelta);
            }
            return this;
        }

        public Builder violationDelta(UUID agentId, int violations) {
            if (agentId != null) {
                this.violationDeltas.put(agentId, violations);
            }
            return this;
        }

        public Builder healthyNodes(int healthyNodes) {
            this.healthyNodes = healthyNodes;
            return this;
        }

        public Builder failedNodes(int failedNodes) {
            this.failedNodes = failedNodes;
            return this;
        }

        public Builder networkLatencyAvg(double networkLatencyAvg) {
            this.networkLatencyAvg = networkLatencyAvg;
            return this;
        }

        public SimulationWorldSnapshot build() {
            if (tick < 0) {
                throw new IllegalArgumentException("tick must be non-negative");
            }
            return new SimulationWorldSnapshot(this);
        }
    }
}

