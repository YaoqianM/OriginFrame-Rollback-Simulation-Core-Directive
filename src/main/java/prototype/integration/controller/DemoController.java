package prototype.integration.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import prototype.integration.IntegrationService;
import prototype.integration.grid.GridScenarioReport;
import prototype.integration.grid.GridScenarioStatus;

@RestController
@RequestMapping("/demo/grid")
public class DemoController {

    private final IntegrationService integrationService;

    public DemoController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/setup")
    public GridScenarioStatus setup() {
        return integrationService.setupScenario();
    }

    @PostMapping("/start")
    public GridScenarioStatus start() {
        return integrationService.startScenario();
    }

    @GetMapping("/status")
    public GridScenarioStatus status() {
        return integrationService.status();
    }

    @PostMapping("/inject-failure")
    public GridScenarioStatus injectFailure() {
        GridScenarioStatus status = integrationService.requestFailureInjection();
        if (!status.phase().equals(prototype.integration.grid.ScenarioPhase.WAITING_FOR_FAILURE)
                && !status.phase().equals(prototype.integration.grid.ScenarioPhase.RUNNING)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Scenario is not in a state that allows failure injection");
        }
        return status;
    }

    @GetMapping("/report")
    public GridScenarioReport report() {
        return integrationService.report();
    }
}


