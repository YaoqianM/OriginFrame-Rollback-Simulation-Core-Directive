package prototype.simulationcore.safety;

import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.Environment;

/**
 * Contract for all safety constraints that can be evaluated against an agent action.
 */
public interface SafetyConstraint {

    ValidationResult validate(Agent agent, Action action, Environment environment);

    String getConstraintType();

    Severity getSeverity();
}

