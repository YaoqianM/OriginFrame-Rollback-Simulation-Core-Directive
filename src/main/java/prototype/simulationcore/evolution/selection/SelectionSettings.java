package prototype.simulationcore.evolution.selection;

import java.util.Objects;

public record SelectionSettings(
        SelectionStrategyType strategyType,
        int survivorCount,
        int tournamentSize,
        int elitismCount,
        double safetyPenalty
) {

    public SelectionSettings {
        Objects.requireNonNull(strategyType, "strategyType");
    }

    public static SelectionSettings defaults(int populationSize) {
        int survivors = Math.max(1, populationSize / 2);
        return new SelectionSettings(
                SelectionStrategyType.TOURNAMENT,
                survivors,
                Math.min(5, Math.max(2, populationSize / 4)),
                Math.max(1, survivors / 2),
                5.0
        );
    }
}


