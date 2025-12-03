package prototype.lineageruntime.lineage.controller;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import prototype.lineageruntime.lineage.dto.LineageExportPayload;
import prototype.lineageruntime.lineage.dto.LineageGenerationStats;
import prototype.lineageruntime.lineage.dto.LineageGraphView;
import prototype.lineageruntime.lineage.dto.LineageNodeView;
import prototype.lineageruntime.lineage.service.LineageGraphService;
import prototype.lineageruntime.lineage.service.LineageTrackerService;

@RestController
@RequestMapping("/evolution/lineage")
public class LineageController {

    private final LineageGraphService graphService;
    private final LineageTrackerService trackerService;

    public LineageController(LineageGraphService graphService,
                             LineageTrackerService trackerService) {
        this.graphService = graphService;
        this.trackerService = trackerService;
    }

    @GetMapping("/{agentId}")
    public LineageNodeView lineage(@PathVariable UUID agentId) {
        return graphService.getLineage(agentId);
    }

    @GetMapping("/{agentId}/tree")
    public LineageExportPayload lineageTree(@PathVariable UUID agentId) {
        return trackerService.exportLineageTree(agentId);
    }

    @GetMapping("/generation/{generation}/stats")
    public LineageGenerationStats generationStats(@PathVariable int generation) {
        return graphService.getGenerationStats(generation);
    }

    @GetMapping("/{agentId}/graph")
    public LineageGraphView lineageGraph(@PathVariable UUID agentId) {
        return graphService.buildGraph(agentId);
    }
}

