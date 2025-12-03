package prototype.integration.grid;

import java.time.Instant;
import java.util.List;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.infrastructure.FaultInjector;

public record GridScenarioReport(
        String scenarioId,
        ScenarioPhase phase,
        int targetTicks,
        int completedTicks,
        boolean failureInjected,
        String failingNodeId,
        Instant startedAt,
        Instant completedAt,
        EvolutionStatus evolutionStatus,
        GenerationReport generationReport,
        FaultInjector.InfrastructureFaultImpact faultImpact,
        List<GridNodeView> nodes,
        List<GridAgentProfile> agents,
        List<ScenarioEvent> timeline
) {
}


