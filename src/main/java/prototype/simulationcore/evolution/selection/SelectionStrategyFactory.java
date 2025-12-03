package prototype.simulationcore.evolution.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Agent;

@Component
public class SelectionStrategyFactory {

    public SelectionStrategy create(SelectionSettings settings) {
        SelectionStrategyType type = settings.strategyType();
        return switch (type) {
            case TOURNAMENT -> (population, survivorCount) ->
                    tournament(population, survivorCount, settings.tournamentSize());
            case ROULETTE -> (population, survivorCount) ->
                    roulette(population, survivorCount);
            case ELITISM -> (population, survivorCount) ->
                    elitism(population, Math.min(survivorCount, settings.elitismCount()));
            case SAFETY_AWARE -> (population, survivorCount) ->
                    safetyAware(population, survivorCount, settings.safetyPenalty());
        };
    }

    private List<Agent> tournament(List<Agent> population, int survivorCount, int tournamentSize) {
        List<Agent> survivors = new ArrayList<>();
        if (population.isEmpty()) {
            return survivors;
        }
        int size = Math.max(2, tournamentSize);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < survivorCount; i++) {
            Agent winner = null;
            for (int j = 0; j < size; j++) {
                Agent contender = population.get(random.nextInt(population.size()));
                if (winner == null || contender.getFitness() > winner.getFitness()) {
                    winner = contender;
                }
            }
            survivors.add(winner);
        }
        return survivors;
    }

    private List<Agent> roulette(List<Agent> population, int survivorCount) {
        List<Agent> survivors = new ArrayList<>();
        if (population.isEmpty()) {
            return survivors;
        }
        double minFitness = population.stream()
                .mapToDouble(Agent::getFitness)
                .min()
                .orElse(0.0);
        double offset = minFitness < 0 ? -minFitness : 0.0;
        double totalFitness = population.stream()
                .mapToDouble(agent -> agent.getFitness() + offset + 1e-6)
                .sum();
        if (totalFitness <= 0) {
            return randomSelection(population, survivorCount);
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < survivorCount; i++) {
            double slice = random.nextDouble(totalFitness);
            double cumulative = 0.0;
            for (Agent agent : population) {
                cumulative += agent.getFitness() + offset + 1e-6;
                if (cumulative >= slice) {
                    survivors.add(agent);
                    break;
                }
            }
        }
        return survivors;
    }

    private List<Agent> elitism(List<Agent> population, int survivorCount) {
        return population.stream()
                .sorted(Comparator.comparingDouble(Agent::getFitness).reversed())
                .limit(survivorCount)
                .toList();
    }

    private List<Agent> safetyAware(List<Agent> population, int survivorCount, double penalty) {
        return population.stream()
                .sorted((a, b) -> Double.compare(score(b, penalty), score(a, penalty)))
                .limit(survivorCount)
                .toList();
    }

    private double score(Agent agent, double penalty) {
        return agent.getFitness() - (penalty * agent.getSafetyViolations());
    }

    private List<Agent> randomSelection(List<Agent> population, int survivorCount) {
        List<Agent> survivors = new ArrayList<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < survivorCount; i++) {
            survivors.add(population.get(random.nextInt(population.size())));
        }
        return survivors;
    }
}


