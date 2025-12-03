package prototype.simulationcore.adversarial.scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import prototype.simulationcore.environment.Environment;

/**
 * Injects unexpected obstacles by elevating hazard-related signals.
 */
public class ObstacleInjectionScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "obstacle_injection";

    private final int maxObstacles;

    public ObstacleInjectionScenario(double severity) {
        this(severity, 5);
    }

    public ObstacleInjectionScenario(double severity, int maxObstacles) {
        super(TYPE, severity);
        this.maxObstacles = Math.max(1, maxObstacles);
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = new HashMap<>(copySensors(environment));
        int injected = (int) Math.max(1, Math.round(maxObstacles * getSeverity()));
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double densityDelta = injected * getSeverity();
        sensors.put("obstacle_density", sensors.getOrDefault("obstacle_density", 0.0) + densityDelta);
        sensors.put("navigation_risk", Math.min(1.0,
                sensors.getOrDefault("navigation_risk", 0.0) + 0.2 * getSeverity()));
        sensors.put("unexpected_obstacles", (double) injected);
        sensors.put("obstacle_variability", random.nextDouble(0.0, 1.0) * getSeverity());
        return rebuild(environment, sensors);
    }
}


