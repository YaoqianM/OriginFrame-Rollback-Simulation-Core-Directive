package prototype.simulationcore.evolution.dto;

import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.evolution.selection.SelectionStrategyType;

public record EvolutionStatus(
        UUID runId,
        boolean running,
        int generation,
        int populationSize,
        SelectionStrategyType selectionStrategy,
        double mutationRate,
        Instant startedAt,
        Instant lastUpdated
) {

    public static EvolutionStatus idle() {
        Instant now = Instant.now();
        return new EvolutionStatus(null, false, 0, 0, SelectionStrategyType.TOURNAMENT, 0.0, now, now);
    }
}


