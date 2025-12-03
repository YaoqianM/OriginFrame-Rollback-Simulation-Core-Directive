package prototype.simulationcore.safety.constraints;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.SafetyConstraint;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.ValidationResult;

@Component
public class ResourceConstraint implements SafetyConstraint {

    private static final String TYPE = "RESOURCE_CONSTRAINT";

    private final SafetyProperties.Resource resource;

    public ResourceConstraint(SafetyProperties properties) {
        this.resource = properties.getResource();
    }

    @Override
    public ValidationResult validate(Agent agent, Action action, Environment environment) {
        double energy = agent.getState().energy();
        double resources = agent.getState().resources();

        if (energy < resource.getMinEnergy()) {
            return buildFailure("Energy level dropped below safety floor", energy, action, resource.getMinEnergy());
        }

        if (resources < resource.getMinResources()) {
            return buildFailure("Resource pool dropped below safety floor", resources, action, resource.getMinResources());
        }

        Double actionEnergyFloor = resource.getMinimumEnergyForAction().get(action);
        if (actionEnergyFloor != null && energy < actionEnergyFloor) {
            return buildFailure("Insufficient energy to perform action", energy, action, actionEnergyFloor);
        }

        Double actionResourceFloor = resource.getMinimumResourcesForAction().get(action);
        if (actionResourceFloor != null && resources < actionResourceFloor) {
            return buildFailure("Insufficient resources to perform action", resources, action, actionResourceFloor);
        }

        return ValidationResult.passed(getConstraintType());
    }

    private ValidationResult buildFailure(String message,
                                          double actual,
                                          Action action,
                                          double required) {
        Map<String, Object> context = new HashMap<>();
        context.put("actual", actual);
        context.put("required", required);
        context.put("action", action.name());
        return ValidationResult.failed(getConstraintType(), getSeverity(), message, context);
    }

    @Override
    public String getConstraintType() {
        return TYPE;
    }

    @Override
    public Severity getSeverity() {
        return resource.getSeverity();
    }
}

