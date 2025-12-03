package prototype.simulationcore.environment;

import java.util.Map;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.Position;

/**
 * Lightweight immutable environment snapshot derived from an agent's sensors.
 */
public class DefaultEnvironment implements Environment {

    private final Position targetPosition;
    private final Map<String, Double> sensors;

    public DefaultEnvironment(AgentState state) {
        this(Position.origin(), state == null ? Map.of() : state.sensorReadings());
    }

    public DefaultEnvironment(Position targetPosition, Map<String, Double> sensors) {
        this.targetPosition = targetPosition == null ? Position.origin() : targetPosition;
        this.sensors = sensors == null ? Map.of() : Map.copyOf(sensors);
    }

    @Override
    public Position getTargetPosition() {
        return targetPosition;
    }

    @Override
    public double readSignal(String key) {
        return sensors.getOrDefault(key, 0.0);
    }

    @Override
    public Map<String, Double> snapshotSensors() {
        return sensors;
    }
}


