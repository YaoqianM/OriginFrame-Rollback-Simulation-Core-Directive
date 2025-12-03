package prototype.simulationcore.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Rich snapshot of an agent's internal and external context.
 */
public record AgentState(
        Position position,
        double energy,
        double resources,
        Map<String, Double> sensorReadings,
        Map<String, Double> internalState
) implements Serializable {

    private static final long serialVersionUID = -1978907712711866508L;

    public AgentState {
        position = position == null ? Position.origin() : position;
        sensorReadings = sensorReadings == null ? Map.of() : Map.copyOf(sensorReadings);
        internalState = internalState == null ? Map.of() : Map.copyOf(internalState);
    }

    /**
     * Baseline state for a freshly minted agent.
     */
    public static AgentState initial() {
        return new AgentState(Position.origin(), 100.0, 0.0, Map.of(), Map.of());
    }

    /**
     * @return deep copy of the current state for lineage captures.
     */
    public AgentState snapshot() {
        return new AgentState(position, energy, resources, sensorReadings, internalState);
    }

    public AgentState withPosition(Position newPosition) {
        return new AgentState(newPosition == null ? Position.origin() : newPosition,
                energy, resources, sensorReadings, internalState);
    }

    public AgentState adjustEnergy(double delta) {
        return new AgentState(position, Math.max(0.0, energy + delta), resources, sensorReadings, internalState);
    }

    public AgentState adjustResources(double delta) {
        return new AgentState(position, energy, Math.max(0.0, resources + delta), sensorReadings, internalState);
    }

    public AgentState withSensorReading(String key, double value) {
        Map<String, Double> updated = new HashMap<>(sensorReadings);
        updated.put(key, value);
        return new AgentState(position, energy, resources, updated, internalState);
    }

    public AgentState withInternalState(String key, double value) {
        Map<String, Double> updated = new HashMap<>(internalState);
        updated.put(key, value);
        return new AgentState(position, energy, resources, sensorReadings, updated);
    }
}

