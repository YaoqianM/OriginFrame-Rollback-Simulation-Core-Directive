package prototype.simulationcore.adversarial.scenario;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;

/**
 * Base helper that normalizes severity and provides common utilities.
 */
public abstract class AbstractAdversarialScenario implements AdversarialScenario {

    private final String scenarioType;
    private final double severity;

    protected AbstractAdversarialScenario(String scenarioType, double severity) {
        this.scenarioType = Objects.requireNonNull(scenarioType, "scenarioType");
        this.severity = clamp(severity);
    }

    @Override
    public String getScenarioType() {
        return scenarioType;
    }

    @Override
    public double getSeverity() {
        return severity;
    }

    protected Map<String, Double> copySensors(Environment environment) {
        return new HashMap<>(environment == null ? Map.of() : environment.snapshotSensors());
    }

    protected Environment rebuild(Environment original, Map<String, Double> sensors) {
        Position target = original == null ? Position.origin() : original.getTargetPosition();
        return new DefaultEnvironment(target, sensors);
    }

    private double clamp(double raw) {
        if (Double.isNaN(raw)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, raw));
    }
}


