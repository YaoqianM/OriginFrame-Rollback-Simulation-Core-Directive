package prototype.visualization.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import prototype.lineageruntime.recovery.ServiceTopology;
import prototype.simulationcore.dto.AgentDto;
import prototype.visualization.model.AgentTrail;
import prototype.visualization.model.LineageTree;
import prototype.visualization.model.LineageView;
import prototype.visualization.model.RenderedGraph;
import prototype.visualization.model.TimelinePoint;
import prototype.visualization.model.WorldSnapshot;
import prototype.visualization.service.DashboardDataService;
import prototype.visualization.service.GraphRenderer;
import prototype.visualization.service.VisualizationService;

/**
 * REST API that exposes visualization-friendly data structures.
 */
@RestController
@RequestMapping("/visualization")
public class VisualizationController {

    private final VisualizationService visualizationService;
    private final GraphRenderer graphRenderer;
    private final ServiceTopology serviceTopology;
    private final DashboardDataService dashboardDataService;

    public VisualizationController(VisualizationService visualizationService,
                                   GraphRenderer graphRenderer,
                                   ServiceTopology serviceTopology,
                                   DashboardDataService dashboardDataService) {
        this.visualizationService = visualizationService;
        this.graphRenderer = graphRenderer;
        this.serviceTopology = serviceTopology;
        this.dashboardDataService = dashboardDataService;
    }

    @GetMapping("/{simId}/world")
    public WorldSnapshot world(@PathVariable String simId,
                               @RequestParam(value = "tick", required = false) Long tick) {
        return visualizationService.getWorldSnapshot(simId, tick);
    }

    @GetMapping("/{simId}/topology")
    public RenderedGraph topology(@PathVariable String simId) {
        return graphRenderer.renderTopology(serviceTopology);
    }

    @GetMapping("/{simId}/agents")
    public List<AgentDto> agents(@PathVariable String simId) {
        return visualizationService.getAgents(simId);
    }

    @GetMapping("/{simId}/agents/{agentId}/trail")
    public AgentTrail agentTrail(@PathVariable String simId,
                                 @PathVariable UUID agentId) {
        return visualizationService.getAgentTrail(simId, agentId);
    }

    @GetMapping("/{simId}/lineage/{agentId}")
    public LineageView lineage(@PathVariable String simId,
                               @PathVariable UUID agentId) {
        try {
            LineageTree tree = visualizationService.getLineageTree(simId, agentId);
            return new LineageView(tree, graphRenderer.renderLineageGraph(tree));
        } catch (NoSuchElementException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/{simId}/timeline")
    public List<TimelinePoint> timeline(@PathVariable String simId) {
        return visualizationService.getTimeline(simId);
    }

    @GetMapping(path = "/{simId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String simId) {
        return dashboardDataService.streamMetrics(simId);
    }
}


