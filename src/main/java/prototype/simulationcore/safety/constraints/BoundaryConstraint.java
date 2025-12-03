package prototype.simulationcore.safety.constraints;

import java.util.Map;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.safety.SafetyConstraint;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.ValidationResult;

@Component
public class BoundaryConstraint implements SafetyConstraint {

    private static final String TYPE = "BOUNDARY_CONSTRAINT";

    private final SafetyProperties.Boundary boundary;

    public BoundaryConstraint(SafetyProperties properties) {
        this.boundary = properties.getBoundary();
    }

    @Override
    public ValidationResult validate(Agent agent, Action action, Environment environment) {
        Position position = agent.getState().position();
        boolean withinBounds =
                position.x() >= boundary.getMinX() && position.x() <= boundary.getMaxX()
                        && position.y() >= boundary.getMinY() && position.y() <= boundary.getMaxY()
                        && position.z() >= boundary.getMinZ() && position.z() <= boundary.getMaxZ();
        if (withinBounds) {
            return ValidationResult.passed(getConstraintType());
        }

        return ValidationResult.failed(
                getConstraintType(),
                getSeverity(),
                "Agent position outside configured safety bounds",
                Map.of(
                        "x", position.x(),
                        "y", position.y(),
                        "z", position.z(),
                        "bounds", boundaryDescription()
                )
        );
    }

    @Override
    public String getConstraintType() {
        return TYPE;
    }

    @Override
    public Severity getSeverity() {
        return boundary.getSeverity();
    }

    private String boundaryDescription() {
        return "x:[" + boundary.getMinX() + "," + boundary.getMaxX() + "],"
                + "y:[" + boundary.getMinY() + "," + boundary.getMaxY() + "],"
                + "z:[" + boundary.getMinZ() + "," + boundary.getMaxZ() + "]";
    }
}

