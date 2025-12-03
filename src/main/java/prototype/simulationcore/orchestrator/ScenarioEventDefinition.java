package prototype.simulationcore.orchestrator;

import java.util.Map;
import prototype.simulationcore.events.SimulationEventType;

public record ScenarioEventDefinition(
        long tick,
        SimulationEventType type,
        Map<String, Object> payload
) {
    public ScenarioEventDefinition {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }
}

