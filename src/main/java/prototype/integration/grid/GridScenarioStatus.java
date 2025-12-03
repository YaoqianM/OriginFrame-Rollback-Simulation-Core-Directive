package prototype.integration.grid;

import java.time.Instant;
import java.util.List;
import prototype.lineageruntime.recovery.ServiceStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.infrastructure.FaultInjector;

public record GridScenarioStatus(
        String scenarioId,
        ScenarioPhase phase,
        int currentTick,
        int targetTicks,
        int failureTick,
        boolean failureRequested,
        boolean failureInjected,
        String failingNodeId,
        Instant setupAt,
        Instant startedAt,
        Instant completedAt,
        EvolutionStatus evolutionStatus,
        GenerationReport latestGeneration,
        FaultInjector.InfrastructureFaultImpact lastFaultImpact,
        List<GridNodeView> nodes,
        List<GridAgentProfile> agents,
        List<ScenarioEvent> recentEvents
) {

    public boolean isTerminal() {
        return phase == ScenarioPhase.COMPLETED || phase == ScenarioPhase.FAILED;
    }

    public boolean isReady() {
        return phase == ScenarioPhase.READY;
    }

    public ServiceStatus dominantRuntimeStatus() {
        return nodes.stream()
                .map(GridNodeView::runtimeStatus)
                .filter(status -> status != null)
                .findFirst()
                .orElse(ServiceStatus.UNKNOWN);
    }
}


