package prototype.simulationcore.orchestrator;

import java.util.List;
import java.util.Map;

public record ScenarioDefinition(
        String name,
        String description,
        Map<String, Object> initialState,
        List<ScenarioEventDefinition> events,
        ScenarioSuccessCriteria successCriteria
) {

    public ScenarioDefinition {
        initialState = initialState == null ? Map.of() : Map.copyOf(initialState);
        events = events == null ? List.of() : List.copyOf(events);
    }

    public static ScenarioDefinition empty(String name) {
        return new ScenarioDefinition(name, null, Map.of(), List.of(), null);
    }
}

