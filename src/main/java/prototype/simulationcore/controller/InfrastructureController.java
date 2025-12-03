package prototype.simulationcore.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import prototype.simulationcore.infrastructure.FaultInjector;
import prototype.simulationcore.infrastructure.InfrastructureMutationResponse;
import prototype.simulationcore.infrastructure.InfrastructureTopologyView;
import prototype.simulationcore.infrastructure.VirtualInfrastructureService;

/**
 * REST entry-point for interacting with the virtual infrastructure overlay.
 */
@RestController
@RequestMapping("/simulation/{simulationId}/infrastructure")
public class InfrastructureController {

    private final VirtualInfrastructureService infrastructureService;
    private final FaultInjector faultInjector;

    public InfrastructureController(VirtualInfrastructureService infrastructureService,
                                    FaultInjector faultInjector) {
        this.infrastructureService = infrastructureService;
        this.faultInjector = faultInjector;
    }

    @PostMapping("/fail/{nodeId}")
    public InfrastructureMutationResponse failNode(@PathVariable String simulationId,
                                                   @PathVariable UUID nodeId) {
        try {
            return new InfrastructureMutationResponse(
                    simulationId,
                    faultInjector.failNode(nodeId),
                    infrastructureService.currentTopology()
            );
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, ex.getMessage(), ex);
        }
    }

    @GetMapping("/topology")
    public InfrastructureTopologyView topology(@PathVariable String simulationId) {
        return infrastructureService.currentTopology();
    }
}


