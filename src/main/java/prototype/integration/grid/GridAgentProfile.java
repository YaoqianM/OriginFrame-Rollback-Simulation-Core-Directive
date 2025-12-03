package prototype.integration.grid;

import java.util.UUID;

public record GridAgentProfile(
        UUID agentId,
        double fitness,
        double cumulativeReward,
        int safetyViolations,
        int generation,
        String rankLabel
) {
}


