package prototype.simulationcore.adversarial.scenario;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import prototype.simulationcore.environment.Environment;

/**
 * Simulates noisy sensors by corrupting readings with random jitter.
 */
public class SensorNoiseScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "sensor_noise";

    private final double maxAmplitude;
    private final double corruptionProbability;

    public SensorNoiseScenario(double severity) {
        this(severity, 10.0, 0.4);
    }

    public SensorNoiseScenario(double severity, double maxAmplitude, double corruptionProbability) {
        super(TYPE, severity);
        this.maxAmplitude = Math.max(0.1, maxAmplitude);
        this.corruptionProbability = Math.max(0.0, Math.min(1.0, corruptionProbability));
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = copySensors(environment);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        sensors.replaceAll((key, value) -> {
            double original = value == null ? 0.0 : value;
            if (random.nextDouble() > corruptionProbability * getSeverity()) {
                return original;
            }
            double noise = (random.nextDouble(-1.0, 1.0)) * maxAmplitude * Math.max(0.1, getSeverity());
            return original + noise;
        });
        return rebuild(environment, sensors);
    }
}


