package prototype.simulationcore.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import prototype.simulationcore.behavior.EmergentBehaviorDetector;
import prototype.simulationcore.behavior.EmergentSignal;
import prototype.simulationcore.dto.ExperimentReportDto;
import prototype.simulationcore.dto.SimulationMetricsResponse;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.MetricsCollector;
import prototype.simulationcore.metrics.SystemMetrics;
import prototype.simulationcore.reporting.ExperimentReport;
import prototype.simulationcore.reporting.ExperimentReportGenerator;
import prototype.simulationcore.timeline.TimelineRecorder;
import prototype.simulationcore.repository.SimulationRunRepository;

@RestController
@RequestMapping("/simulation")
public class SimulationInsightsController {

    private final MetricsCollector metricsCollector;
    private final TimelineRecorder timelineRecorder;
    private final EmergentBehaviorDetector behaviorDetector;
    private final ExperimentReportGenerator reportGenerator;
    private final SimulationRunRepository simulationRunRepository;

    public SimulationInsightsController(MetricsCollector metricsCollector,
                                        TimelineRecorder timelineRecorder,
                                        EmergentBehaviorDetector behaviorDetector,
                                        ExperimentReportGenerator reportGenerator,
                                        SimulationRunRepository simulationRunRepository) {
        this.metricsCollector = metricsCollector;
        this.timelineRecorder = timelineRecorder;
        this.behaviorDetector = behaviorDetector;
        this.reportGenerator = reportGenerator;
        this.simulationRunRepository = simulationRunRepository;
    }

    @GetMapping("/{id}/metrics")
    public SimulationMetricsResponse metrics(@PathVariable("id") UUID simulationId,
                                             @RequestParam(name = "from", defaultValue = "0") int from,
                                             @RequestParam(name = "to", defaultValue = "0") int to) {
        ensureRunExists(simulationId);
        String key = simulationId.toString();
        int toTick = to <= 0 ? Integer.MAX_VALUE : to;
        List<AgentMetric> agentMetrics = metricsCollector.getAgentMetrics(key, from, toTick);
        List<SystemMetrics> systemMetrics = metricsCollector.getSystemMetrics(key, from, toTick);
        if (agentMetrics.isEmpty() && systemMetrics.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No metrics captured for simulation " + simulationId);
        }
        List<EmergentSignal> signals = new ArrayList<>();
        signals.add(behaviorDetector.detectClustering(agentMetrics));
        signals.add(behaviorDetector.detectCompetition(agentMetrics));
        signals.add(behaviorDetector.detectCooperation(agentMetrics));
        signals.addAll(behaviorDetector.flagUnusualPatterns(systemMetrics));

        return new SimulationMetricsResponse(
                key,
                Math.max(from, 0),
                toTick,
                systemMetrics,
                agentMetrics,
                timelineRecorder.getTimeline(key, from, toTick),
                signals
        );
    }

    @GetMapping("/{id}/report")
    public ExperimentReportDto report(@PathVariable("id") UUID simulationId,
                                      @RequestParam(name = "from", defaultValue = "0") int from,
                                      @RequestParam(name = "to", defaultValue = "0") int to) {
        ensureRunExists(simulationId);
        try {
            ExperimentReport report = reportGenerator.generate(simulationId.toString(), from, to);
            return ExperimentReportDto.from(report);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    private void ensureRunExists(UUID simulationId) {
        if (!simulationRunRepository.existsById(simulationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Simulation run not found: " + simulationId);
        }
    }
}

