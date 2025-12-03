package prototype.simulationcore.dto;

import java.util.Base64;
import java.util.List;
import java.util.Objects;
import prototype.simulationcore.behavior.EmergentSignal;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.SystemMetrics;
import prototype.simulationcore.reporting.ExperimentReport;
import prototype.simulationcore.timeline.TimelineEvent;

public record ExperimentReportDto(
        String simulationId,
        int fromTick,
        int toTick,
        String markdown,
        String pdfBase64,
        List<EmergentSignal> emergentSignals,
        List<SystemMetrics> systemMetrics,
        List<AgentMetric> agentMetrics,
        List<TimelineEvent> keyEvents
) {

    public ExperimentReportDto {
        Objects.requireNonNull(simulationId, "simulationId");
        markdown = markdown == null ? "" : markdown;
        pdfBase64 = pdfBase64 == null ? "" : pdfBase64;
        emergentSignals = emergentSignals == null ? List.of() : List.copyOf(emergentSignals);
        systemMetrics = systemMetrics == null ? List.of() : List.copyOf(systemMetrics);
        agentMetrics = agentMetrics == null ? List.of() : List.copyOf(agentMetrics);
        keyEvents = keyEvents == null ? List.of() : List.copyOf(keyEvents);
    }

    public static ExperimentReportDto from(ExperimentReport report) {
        return new ExperimentReportDto(
                report.simulationId(),
                report.fromTick(),
                report.toTick(),
                report.markdown(),
                Base64.getEncoder().encodeToString(report.pdfBytes()),
                report.emergentSignals(),
                report.systemMetrics(),
                report.agentMetrics(),
                report.keyEvents()
        );
    }
}

