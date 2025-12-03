package prototype.simulationcore.service;

import java.util.List;
import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.safety.ConstraintValidator;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.Violation;

@Service
public class SafetyConstraintsService {

    private final ConstraintValidator constraintValidator;

    public SafetyConstraintsService(ConstraintValidator constraintValidator) {
        this.constraintValidator = constraintValidator;
    }

    public SafetyEvaluation enforce(Agent agent) {
        List<Violation> violations = constraintValidator.auditState(agent);
        if (violations.isEmpty()) {
            return SafetyEvaluation.passed();
        }
        Violation violation = violations.get(0);
        return SafetyEvaluation.violated(violation.message(), violation.severity());
    }

    public record SafetyEvaluation(boolean violated, String reason, Severity severity) {

        private static SafetyEvaluation passed() {
            return new SafetyEvaluation(false, null, null);
        }

        private static SafetyEvaluation violated(String reason, Severity severity) {
            return new SafetyEvaluation(true, reason, severity);
        }
    }
}

