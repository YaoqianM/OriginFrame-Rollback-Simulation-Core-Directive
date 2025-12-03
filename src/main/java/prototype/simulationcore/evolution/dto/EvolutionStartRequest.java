package prototype.simulationcore.evolution.dto;

import java.util.UUID;
import prototype.simulationcore.evolution.selection.SelectionSettings;
import prototype.simulationcore.evolution.selection.SelectionStrategyType;

public record EvolutionStartRequest(
        Integer populationSize,
        UUID basePolicyId,
        SelectionStrategyType selectionStrategy,
        Integer survivorCount,
        Integer tournamentSize,
        Integer elitismCount,
        Double safetyPenalty,
        Double mutationRate
) {

    public int resolvePopulationSize() {
        return populationSize == null || populationSize <= 0 ? 16 : populationSize;
    }

    public SelectionSettings toSelectionSettings(int actualPopulationSize) {
        SelectionStrategyType type = selectionStrategy == null
                ? SelectionStrategyType.TOURNAMENT
                : selectionStrategy;
        int survivors = survivorCount == null
                ? Math.max(1, actualPopulationSize / 2)
                : Math.max(1, Math.min(actualPopulationSize, survivorCount));
        int tournament = tournamentSize == null
                ? Math.min(5, Math.max(2, actualPopulationSize / 4))
                : Math.max(2, tournamentSize);
        int elitism = elitismCount == null
                ? Math.max(1, survivors / 2)
                : Math.max(1, elitismCount);
        double penalty = safetyPenalty == null ? 5.0 : Math.max(0.0, safetyPenalty);
        return new SelectionSettings(type, survivors, tournament, elitism, penalty);
    }

    public double resolveMutationRate() {
        double rate = mutationRate == null ? 0.1 : mutationRate;
        return Math.max(0.0, Math.min(1.0, rate));
    }
}


