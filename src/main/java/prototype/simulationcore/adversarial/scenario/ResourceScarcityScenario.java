package prototype.simulationcore.adversarial.scenario;

import java.util.Map;
import prototype.simulationcore.environment.Environment;

/**
 * Simulates sudden drops in resource availability.
 */
public class ResourceScarcityScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "resource_scarcity";

    private final double depletionRatio;

    public ResourceScarcityScenario(double severity) {
        this(severity, 0.6);
    }

    public ResourceScarcityScenario(double severity, double depletionRatio) {
        super(TYPE, severity);
        this.depletionRatio = Math.max(0.1, Math.min(1.0, depletionRatio));
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = copySensors(environment);
        double factor = 1.0 - (depletionRatio * getSeverity());
        double clampFactor = Math.max(0.0, factor);
        sensors.computeIfPresent("resources", (k, v) -> Math.max(0.0, v * clampFactor));
        sensors.computeIfPresent("supply_density", (k, v) -> Math.max(0.0, v * clampFactor));
        sensors.put("resource_alert", getSeverity());
        return rebuild(environment, sensors);
    }
}


