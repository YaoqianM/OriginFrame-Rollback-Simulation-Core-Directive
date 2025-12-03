package prototype.simulationcore.adversarial.scenario;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import prototype.simulationcore.environment.Environment;

/**
 * Blocks or disrupts agent communication channels.
 */
public class CommunicationFailureScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "communication_failure";

    private final double dropProbability;

    public CommunicationFailureScenario(double severity) {
        this(severity, 0.8);
    }

    public CommunicationFailureScenario(double severity, double dropProbability) {
        super(TYPE, severity);
        this.dropProbability = Math.max(0.0, Math.min(1.0, dropProbability));
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = copySensors(environment);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        sensors.replaceAll((key, value) -> {
            double original = value == null ? 0.0 : value;
            if (!key.toLowerCase().contains("comm") && !key.toLowerCase().contains("signal")) {
                return original;
            }
            if (random.nextDouble() > dropProbability * getSeverity()) {
                return original * 0.5;
            }
            return 0.0;
        });
        sensors.put("communication_blocked", getSeverity());
        return rebuild(environment, sensors);
    }
}


