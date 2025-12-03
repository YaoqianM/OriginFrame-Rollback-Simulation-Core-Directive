package prototype.lineageruntime.lineage.model;

import java.util.Map;
import prototype.simulationcore.domain.Agent;

public record LineageMetrics(
        double performanceScore,
        double safetyScore,
        int survivedGenerations,
        Map<String, Object> metadata
) {

    public LineageMetrics {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    public static LineageMetrics fromAgent(Agent agent, Map<String, Object> metadata) {
        double performance = agent.getFitness();
        double safety = Math.max(0.0, 100.0 - (agent.getSafetyViolations() * 5.0));
        return new LineageMetrics(performance, safety, agent.getGeneration(), metadata);
    }
}

