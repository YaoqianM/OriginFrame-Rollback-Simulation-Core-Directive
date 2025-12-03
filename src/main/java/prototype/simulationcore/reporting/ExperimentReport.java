package prototype.simulationcore.reporting;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import prototype.simulationcore.behavior.EmergentSignal;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.SystemMetrics;
import prototype.simulationcore.timeline.TimelineEvent;

public record ExperimentReport(
        String simulationId,
        int fromTick,
        int toTick,
        Instant generatedAt,
        Map<String, Object> configuration,
        List<SystemMetrics> systemMetrics,
        List<AgentMetric> agentMetrics,
        List<TimelineEvent> keyEvents,
        List<EmergentSignal> emergentSignals,
        String markdown,
        byte[] pdfBytes
) {

    public ExperimentReport {
        Objects.requireNonNull(simulationId, "simulationId");
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
        configuration = configuration == null
                ? Map.of()
                : Collections.unmodifiableMap(new HashMap<>(configuration));
        systemMetrics = systemMetrics == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(systemMetrics));
        agentMetrics = agentMetrics == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(agentMetrics));
        keyEvents = keyEvents == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(keyEvents));
        emergentSignals = emergentSignals == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(emergentSignals));
        markdown = markdown == null ? "" : markdown;
        pdfBytes = pdfBytes == null ? new byte[0] : pdfBytes.clone();
    }
}

