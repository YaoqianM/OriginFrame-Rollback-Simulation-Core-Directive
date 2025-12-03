package prototype.simulationcore.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import prototype.simulationcore.events.SimulationEvent;
import prototype.simulationcore.orchestrator.ScenarioDefinition;
import prototype.simulationcore.orchestrator.SimulationWorld;
import prototype.simulationcore.orchestrator.SimulationWorldStatus;

public record SimulationWorldView(
        UUID id,
        String name,
        SimulationWorldStatus status,
        long currentTick,
        long maxTicks,
        String scenario,
        Instant createdAt,
        Instant updatedAt,
        Map<String, Object> environment,
        List<SimulationEvent> recentEvents,
        int constraintViolations
) {

    public static SimulationWorldView from(SimulationWorld world) {
        return new SimulationWorldView(
                world.getSimulationId(),
                world.getConfig().getName(),
                world.getStatus(),
                world.getCurrentTick(),
                world.getConfig().getMaxTicks(),
                world.getScenarioDefinition().map(ScenarioDefinition::name).orElse(null),
                world.getCreatedAt(),
                world.getUpdatedAt(),
                world.snapshotEnvironment(),
                world.getRecentEvents(),
                world.getConstraintViolationCount()
        );
    }
}

