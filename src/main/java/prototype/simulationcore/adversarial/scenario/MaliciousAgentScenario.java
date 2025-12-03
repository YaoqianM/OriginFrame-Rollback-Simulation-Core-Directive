package prototype.simulationcore.adversarial.scenario;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import prototype.simulationcore.environment.Environment;

/**
 * Simulates adversarial peers influencing the agent.
 */
public class MaliciousAgentScenario extends AbstractAdversarialScenario {

    public static final String TYPE = "malicious_agent";

    private final double aggressionBias;

    public MaliciousAgentScenario(double severity) {
        this(severity, 0.75);
    }

    public MaliciousAgentScenario(double severity, double aggressionBias) {
        super(TYPE, severity);
        this.aggressionBias = Math.max(0.0, Math.min(1.0, aggressionBias));
    }

    @Override
    public Environment apply(Environment environment) {
        Map<String, Double> sensors = copySensors(environment);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double threatBoost = aggressionBias * getSeverity() * 100.0;
        sensors.put("threat", sensors.getOrDefault("threat", 0.0) + threatBoost);
        sensors.put("ally_trust", Math.max(0.0,
                sensors.getOrDefault("ally_trust", 1.0) - (getSeverity() * 0.5)));
        sensors.put("peer_interference", random.nextDouble(0.25, 1.0) * getSeverity());
        sensors.put("spoofed_signal", random.nextDouble(-1.0, 1.0) * 50.0 * getSeverity());
        return rebuild(environment, sensors);
    }
}


