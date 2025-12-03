package prototype.simulationcore.evolution.dto;

import java.util.UUID;

public record AgentSummary(
        UUID agentId,
        double fitness,
        int safetyViolations,
        double cumulativeReward
) {
}


