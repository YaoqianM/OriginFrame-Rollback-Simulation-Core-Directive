package prototype.simulationcore.controller;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.domain.SafetyViolation;
import prototype.simulationcore.service.SafetyViolationService;

@RestController
@RequestMapping("/evolution/safety")
public class SafetyViolationController {

    private final SafetyViolationService violationService;

    public SafetyViolationController(SafetyViolationService violationService) {
        this.violationService = violationService;
    }

    @GetMapping("/violations")
    public List<SafetyViolation> violations(@RequestParam(required = false) UUID agentId,
                                            @RequestParam(required = false) Severity severity) {
        return violationService.findViolations(agentId, severity);
    }
}

