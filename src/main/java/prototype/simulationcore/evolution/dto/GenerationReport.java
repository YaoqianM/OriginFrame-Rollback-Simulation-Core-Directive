package prototype.simulationcore.evolution.dto;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record GenerationReport(
        UUID runId,
        int generation,
        GenerationStats stats,
        List<AgentSummary> bestAgents,
        List<UUID> safetyViolations
) {

    public GenerationReport {
        stats = stats == null ? GenerationStats.empty() : stats;
        bestAgents = bestAgents == null ? List.of() : List.copyOf(bestAgents);
        safetyViolations = safetyViolations == null ? List.of() : List.copyOf(safetyViolations);
    }

    public static GenerationReport empty() {
        return new GenerationReport(null, 0, GenerationStats.empty(), Collections.emptyList(), Collections.emptyList());
    }
}


