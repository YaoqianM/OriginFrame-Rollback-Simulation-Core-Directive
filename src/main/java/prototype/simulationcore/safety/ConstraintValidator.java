package prototype.simulationcore.safety;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;

@Service
public class ConstraintValidator {

    private final List<SafetyConstraint> constraints;
    private final ViolationHandler violationHandler;

    public ConstraintValidator(List<SafetyConstraint> constraints,
                               ViolationHandler violationHandler) {
        this.constraints = constraints;
        this.violationHandler = violationHandler;
    }

    public List<Violation> validateAction(Agent agent, Action action, Environment environment) {
        return evaluate(agent, action, environment, ConstraintPhase.EXECUTION);
    }

    public boolean preActionCheck(Agent agent, Action action) {
        return preActionCheck(agent, action, new DefaultEnvironment(agent.getState()));
    }

    public boolean preActionCheck(Agent agent, Action action, Environment environment) {
        List<Violation> violations = evaluate(agent, action, environment, ConstraintPhase.PRE_ACTION);
        handleViolations(agent, environment, violations);
        return violations.stream().noneMatch(violation -> violation.severity() == Severity.CRITICAL);
    }

    public void postActionAudit(Agent agent, Action action, AgentState resultState) {
        Environment environment = new DefaultEnvironment(resultState);
        List<Violation> violations = evaluate(agent, action, environment, ConstraintPhase.POST_ACTION);
        handleViolations(agent, environment, violations);
    }

    public List<Violation> auditState(Agent agent) {
        return evaluate(agent, Action.WAIT, new DefaultEnvironment(agent.getState()), ConstraintPhase.POST_ACTION);
    }

    private List<Violation> evaluate(Agent agent,
                                     Action action,
                                     Environment environment,
                                     ConstraintPhase phase) {
        ConstraintContext.setPhase(phase);
        try {
            List<Violation> violations = new ArrayList<>();
            for (SafetyConstraint constraint : constraints) {
                ValidationResult result = constraint.validate(agent, action, environment);
                if (result.valid()) {
                    continue;
                }
                violations.add(Violation.fromResult(result, action));
            }
            return violations;
        } finally {
            ConstraintContext.clear();
        }
    }

    private void handleViolations(Agent agent, Environment environment, List<Violation> violations) {
        violations.forEach(violation -> violationHandler.handleViolation(agent, violation, environment));
    }
}

