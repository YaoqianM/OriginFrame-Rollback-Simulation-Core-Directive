package prototype.simulationcore.reporting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;
import prototype.simulationcore.behavior.EmergentBehaviorDetector;
import prototype.simulationcore.behavior.EmergentSignal;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.MetricsCollector;
import prototype.simulationcore.metrics.SystemMetrics;
import prototype.simulationcore.timeline.TimelineEvent;
import prototype.simulationcore.timeline.TimelineRecorder;

@Component
public class ExperimentReportGenerator {

    private final MetricsCollector metricsCollector;
    private final TimelineRecorder timelineRecorder;
    private final EmergentBehaviorDetector behaviorDetector;

    public ExperimentReportGenerator(MetricsCollector metricsCollector,
                                     TimelineRecorder timelineRecorder,
                                     EmergentBehaviorDetector behaviorDetector) {
        this.metricsCollector = metricsCollector;
        this.timelineRecorder = timelineRecorder;
        this.behaviorDetector = behaviorDetector;
    }

    public ExperimentReport generate(String simulationId, int fromTick, int toTick) {
        int resolvedFrom = Math.max(0, fromTick);
        int resolvedTo = toTick <= 0 ? metricsCollector.latestTick(simulationId) : toTick;
        if (resolvedTo < resolvedFrom) {
            resolvedTo = resolvedFrom;
        }

        List<AgentMetric> agentMetrics = metricsCollector.getAgentMetrics(simulationId, resolvedFrom, resolvedTo);
        List<SystemMetrics> systemMetrics = metricsCollector.getSystemMetrics(simulationId, resolvedFrom, resolvedTo);
        if (agentMetrics.isEmpty() && systemMetrics.isEmpty()) {
            throw new IllegalStateException("No metrics available for simulation " + simulationId);
        }

        List<TimelineEvent> timeline = timelineRecorder.getTimeline(simulationId, resolvedFrom, resolvedTo);
        List<EmergentSignal> emergentSignals = new ArrayList<>();
        emergentSignals.add(behaviorDetector.detectClustering(agentMetrics));
        emergentSignals.add(behaviorDetector.detectCompetition(agentMetrics));
        emergentSignals.add(behaviorDetector.detectCooperation(agentMetrics));
        emergentSignals.addAll(behaviorDetector.flagUnusualPatterns(systemMetrics));

        Map<String, Object> configuration = buildConfiguration(agentMetrics, systemMetrics, resolvedFrom, resolvedTo);
        List<TimelineEvent> keyEvents = timeline.stream().limit(25).toList();

        String markdown = buildMarkdown(simulationId, resolvedFrom, resolvedTo, configuration,
                systemMetrics, agentMetrics, emergentSignals, keyEvents);
        byte[] pdfBytes = renderPdf(markdown);

        return new ExperimentReport(
                simulationId,
                resolvedFrom,
                resolvedTo,
                Instant.now(),
                configuration,
                systemMetrics,
                agentMetrics,
                keyEvents,
                emergentSignals,
                markdown,
                pdfBytes
        );
    }

    private Map<String, Object> buildConfiguration(List<AgentMetric> agentMetrics,
                                                   List<SystemMetrics> systemMetrics,
                                                   int fromTick,
                                                   int toTick) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("tickWindow", fromTick + "-" + toTick);
        config.put("agentCount", agentMetrics.stream().map(AgentMetric::agentId).distinct().count());
        config.put("activeAgentsPeak", systemMetrics.stream()
                .mapToInt(SystemMetrics::activeAgents)
                .max()
                .orElse(0));
        config.put("avgFitnessOverall", systemMetrics.stream()
                .mapToDouble(SystemMetrics::averageAgentFitness)
                .average()
                .orElse(0.0));
        config.put("totalViolationsMax", systemMetrics.stream()
                .mapToInt(SystemMetrics::totalViolations)
                .max()
                .orElse(0));
        config.put("latencyPeak", systemMetrics.stream()
                .mapToDouble(SystemMetrics::networkLatencyAvg)
                .max()
                .orElse(0.0));
        return config;
    }

    private String buildMarkdown(String simulationId,
                                 int fromTick,
                                 int toTick,
                                 Map<String, Object> configuration,
                                 List<SystemMetrics> systemMetrics,
                                 List<AgentMetric> agentMetrics,
                                 List<EmergentSignal> signals,
                                 List<TimelineEvent> keyEvents) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Experiment Report for ").append(simulationId).append("\n\n");
        sb.append(String.format("_Ticks %d to %d — generated %s_\n\n", fromTick, toTick, Instant.now()));

        sb.append("## Configuration\n");
        configuration.forEach((key, value) -> sb.append(String.format("- **%s**: %s%n", key, value)));
        sb.append('\n');

        sb.append("## System Metrics (sample)\n");
        if (systemMetrics.isEmpty()) {
            sb.append("- No system metrics captured in this window\n");
        } else {
            systemMetrics.stream()
                    .sorted((a, b) -> Integer.compare(a.tick(), b.tick()))
                    .limit(20)
                    .forEach(metric -> sb.append(String.format(
                            "- Tick %d → active=%d/%d, fitness=%.2f, violations=%d, latency=%.2fms%n",
                            metric.tick(),
                            metric.activeAgents(),
                            metric.totalAgents(),
                            metric.averageAgentFitness(),
                            metric.totalViolations(),
                            metric.networkLatencyAvg()
                    )));
        }
        sb.append('\n');

        sb.append("## Agent Metrics (latest tick)\n");
        int latestTick = agentMetrics.stream().mapToInt(AgentMetric::tick).max().orElse(fromTick);
        if (agentMetrics.isEmpty()) {
            sb.append("- No agent metrics captured in this window\n");
        } else {
            agentMetrics.stream()
                    .filter(metric -> metric.tick() == latestTick)
                    .forEach(metric -> sb.append(String.format(
                            "- Agent %s → energy=%.2f, resources=%.2f, reward=%.2f, actions=%s%n",
                            metric.agentId(),
                            metric.energy(),
                            metric.resources(),
                            metric.rewardThisTick(),
                            metric.actionsPerformed()
                    )));
        }
        sb.append('\n');

        sb.append("## Key Events\n");
        if (keyEvents.isEmpty()) {
            sb.append("- No timeline events captured\n");
        } else {
            keyEvents.forEach(event -> sb.append(String.format(
                    "- [%s] Tick %d — %s (%s)%n",
                    event.timestamp(),
                    event.tick(),
                    event.description(),
                    event.eventType()
            )));
        }
        sb.append('\n');

        sb.append("## Emergent Signals\n");
        signals.forEach(signal -> sb.append(String.format(
                "- %s → %s (%s)%n",
                signal.type(),
                signal.detected() ? "DETECTED" : "not detected",
                signal.summary()
        )));
        sb.append('\n');

        return sb.toString();
    }

    private byte[] renderPdf(String markdown) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            float fontSize = 11f;
            float leading = 1.2f * fontSize;
            float margin = 40f;
            float usableWidth = page.getMediaBox().getWidth() - 2 * margin;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            contentStream.beginText();
            float y = page.getMediaBox().getHeight() - margin;
            contentStream.newLineAtOffset(margin, y);

            for (String line : markdown.split("\n")) {
                List<String> wrapped = wrapLine(line, usableWidth, fontSize);
                if (wrapped.isEmpty()) {
                    wrapped = List.of("");
                }
                for (String segment : wrapped) {
                    if (y <= margin) {
                        contentStream.endText();
                        contentStream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                        contentStream.beginText();
                        y = page.getMediaBox().getHeight() - margin;
                        contentStream.newLineAtOffset(margin, y);
                    }
                    contentStream.showText(segment);
                    contentStream.newLineAtOffset(0, -leading);
                    y -= leading;
                }
            }

            contentStream.endText();
            contentStream.close();

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                document.save(baos);
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate PDF report", e);
        }
    }

    private List<String> wrapLine(String line, float usableWidth, float fontSize) {
        if (line == null || line.isEmpty()) {
            return List.of("");
        }
        List<String> wrapped = new ArrayList<>();
        String[] words = line.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            boolean currentEmpty = current.length() == 0;
            String candidate = currentEmpty ? word : current + " " + word;
            if (stringWidth(candidate, fontSize) > usableWidth && !currentEmpty) {
                wrapped.add(current.toString());
                current = new StringBuilder(word);
            } else {
                if (!currentEmpty) {
                    current.append(' ');
                }
                current.append(word);
            }
        }
        if (current.length() > 0) {
            wrapped.add(current.toString());
        }
        return wrapped;
    }

    private float stringWidth(String text, float fontSize) {
        try {
            return PDType1Font.HELVETICA.getStringWidth(text) / 1000 * fontSize;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to measure string width", e);
        }
    }
}

