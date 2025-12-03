package prototype.simulationcore.dto;

import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.world.SimulationRun;
import prototype.simulationcore.world.WorldStatus;

public record SimulationRunDto(
        UUID runId,
        UUID worldId,
        String worldName,
        WorldStatus status,
        Instant startTime,
        Instant endTime,
        long totalTicks
) {

    public static SimulationRunDto from(SimulationRun run) {
        return new SimulationRunDto(
                run.getRunId(),
                run.getWorldId(),
                run.getWorldName(),
                run.getStatus(),
                run.getStartTime(),
                run.getEndTime(),
                run.getTotalTicks()
        );
    }
}


