package prototype.simulationcore.dto;

import java.util.List;
import java.util.Objects;
import prototype.simulationcore.behavior.EmergentSignal;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.SystemMetrics;
import prototype.simulationcore.timeline.TimelineEvent;

public record SimulationMetricsResponse(
        String simulationId,
        int fromTick,
        int toTick,
        List<SystemMetrics> systemMetrics,
        List<AgentMetric> agentMetrics,
        List<TimelineEvent> timeline,
        List<EmergentSignal> emergentSignals
) {

    public SimulationMetricsResponse {
        Objects.requireNonNull(simulationId, "simulationId");
        systemMetrics = systemMetrics == null ? List.of() : List.copyOf(systemMetrics);
        agentMetrics = agentMetrics == null ? List.of() : List.copyOf(agentMetrics);
        timeline = timeline == null ? List.of() : List.copyOf(timeline);
        emergentSignals = emergentSignals == null ? List.of() : List.copyOf(emergentSignals);
    }
}

