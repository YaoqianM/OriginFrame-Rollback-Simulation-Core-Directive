package prototype.simulationcore.adversarial.scenario;

import java.util.HashMap;
import java.util.Map;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.environment.Environment;

/**
 * Changes macro environment parameters mid-run.
 */
public class EnvironmentShiftScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "environment_shift";

    private final Position targetOffset;
    private final Map<String, Double> parameterDrift;

    public EnvironmentShiftScenario(double severity) {
        this(severity, new Position(1.0, 0.5, 0.0), Map.of("temperature", 5.0, "radiation", 2.0));
    }

    public EnvironmentShiftScenario(double severity, Position targetOffset, Map<String, Double> parameterDrift) {
        super(TYPE, severity);
        this.targetOffset = targetOffset == null ? Position.origin() : targetOffset;
        this.parameterDrift = parameterDrift == null ? Map.of() : Map.copyOf(parameterDrift);
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = new HashMap<>(copySensors(environment));
        parameterDrift.forEach((key, value) -> {
            double original = sensors.getOrDefault(key, 0.0);
            sensors.put(key, original + (value * getSeverity()));
        });
        Position shiftedTarget = environment == null
                ? Position.origin()
                : environment.getTargetPosition().offset(
                        targetOffset.x() * getSeverity(),
                        targetOffset.y() * getSeverity(),
                        targetOffset.z() * getSeverity()
        );
        return new prototype.simulationcore.environment.DefaultEnvironment(shiftedTarget, sensors);
    }
}


