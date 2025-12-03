package prototype.visualization.model;

import java.util.List;
import java.util.Objects;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;

/**
 * Aggregated statistics over the current simulation world.
 */
public record WorldMetrics(
        int totalAgents,
        double averageEnergy,
        double averageResources,
        double totalFitness,
        double averageFitness
) {

    public static WorldMetrics fromAgents(List<Agent> agents) {
        List<Agent> safeList = agents == null ? List.of() : List.copyOf(agents);
        int total = safeList.size();

        double totalEnergy = safeList.stream()
                .map(Agent::getState)
                .filter(Objects::nonNull)
                .mapToDouble(AgentState::energy)
                .sum();

        double totalResources = safeList.stream()
                .map(Agent::getState)
                .filter(Objects::nonNull)
                .mapToDouble(AgentState::resources)
                .sum();

        double totalFitness = safeList.stream()
                .mapToDouble(Agent::getFitness)
                .sum();

        double avgEnergy = total == 0 ? 0.0 : totalEnergy / total;
        double avgResources = total == 0 ? 0.0 : totalResources / total;
        double avgFitness = total == 0 ? 0.0 : totalFitness / total;

        return new WorldMetrics(
                total,
                avgEnergy,
                avgResources,
                totalFitness,
                avgFitness
        );
    }
}


