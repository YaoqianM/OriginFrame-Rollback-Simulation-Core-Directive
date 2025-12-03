package prototype.visualization.model;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;

/**
 * Aggregated metrics streamed to the dashboard.
 */
public record DashboardMetrics(
        String simulationId,
        Instant generatedAt,
        int totalAgents,
        double averageEnergy,
        double averageResources,
        double averageFitness,
        double eventRatePerMinute,
        double latestEnergy,
        double latestResources,
        long totalEvents
) {

    public static DashboardMetrics from(String simulationId,
                                        List<Agent> agents,
                                        List<LineageEvent> events) {
        Instant now = Instant.now();
        List<Agent> safeAgents = agents == null ? List.of() : List.copyOf(agents);
        List<LineageEvent> safeEvents = events == null ? List.of() : List.copyOf(events);
        int totalAgents = safeAgents.size();

        double totalEnergy = safeAgents.stream()
                .map(Agent::getState)
                .filter(Objects::nonNull)
                .mapToDouble(AgentState::energy)
                .sum();

        double totalResources = safeAgents.stream()
                .map(Agent::getState)
                .filter(Objects::nonNull)
                .mapToDouble(AgentState::resources)
                .sum();

        double totalFitness = safeAgents.stream()
                .mapToDouble(Agent::getFitness)
                .sum();

        double averageEnergy = totalAgents == 0 ? 0.0 : totalEnergy / totalAgents;
        double averageResources = totalAgents == 0 ? 0.0 : totalResources / totalAgents;
        double averageFitness = totalAgents == 0 ? 0.0 : totalFitness / totalAgents;

        LineageEvent latest = safeEvents.isEmpty() ? null : safeEvents.get(safeEvents.size() - 1);
        double latestEnergy = latest == null ? averageEnergy : latest.getResultingState().energy();
        double latestResources = latest == null ? averageResources : latest.getResultingState().resources();

        Instant cutoff = now.minus(Duration.ofMinutes(1));
        long recentEvents = safeEvents.stream()
                .filter(event -> event.getTimestamp().isAfter(cutoff))
                .count();

        double eventRatePerMinute = recentEvents;

        return new DashboardMetrics(
                simulationId,
                now,
                totalAgents,
                averageEnergy,
                averageResources,
                averageFitness,
                eventRatePerMinute,
                latestEnergy,
                latestResources,
                safeEvents.size()
        );
    }
}


