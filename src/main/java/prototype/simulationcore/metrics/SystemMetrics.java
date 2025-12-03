package prototype.simulationcore.metrics;

import java.time.Instant;
import java.util.Objects;

/**
 * Aggregated system-level metrics per tick.
 */
public record SystemMetrics(
        String simulationId,
        int tick,
        int activeAgents,
        int totalAgents,
        int healthyNodes,
        int failedNodes,
        double averageAgentFitness,
        int totalViolations,
        double networkLatencyAvg,
        Instant recordedAt
) {

    public SystemMetrics {
        Objects.requireNonNull(simulationId, "simulationId");
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public static Builder builder(String simulationId) {
        return new Builder(simulationId);
    }

    public static final class Builder {
        private final String simulationId;
        private int tick;
        private int activeAgents;
        private int totalAgents;
        private int healthyNodes;
        private int failedNodes;
        private double averageAgentFitness;
        private int totalViolations;
        private double networkLatencyAvg;
        private Instant recordedAt;

        private Builder(String simulationId) {
            this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        }

        public Builder tick(int tick) {
            this.tick = tick;
            return this;
        }

        public Builder activeAgents(int activeAgents) {
            this.activeAgents = activeAgents;
            return this;
        }

        public Builder totalAgents(int totalAgents) {
            this.totalAgents = totalAgents;
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

        public Builder averageAgentFitness(double averageAgentFitness) {
            this.averageAgentFitness = averageAgentFitness;
            return this;
        }

        public Builder totalViolations(int totalViolations) {
            this.totalViolations = totalViolations;
            return this;
        }

        public Builder networkLatencyAvg(double networkLatencyAvg) {
            this.networkLatencyAvg = networkLatencyAvg;
            return this;
        }

        public Builder recordedAt(Instant recordedAt) {
            this.recordedAt = recordedAt;
            return this;
        }

        public SystemMetrics build() {
            return new SystemMetrics(
                    simulationId,
                    tick,
                    activeAgents,
                    totalAgents,
                    healthyNodes,
                    failedNodes,
                    averageAgentFitness,
                    totalViolations,
                    networkLatencyAvg,
                    recordedAt
            );
        }
    }
}

