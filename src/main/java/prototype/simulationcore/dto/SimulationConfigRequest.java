package prototype.simulationcore.dto;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import prototype.simulationcore.orchestrator.SimulationConfig;

public record SimulationConfigRequest(
        String name,
        String scenarioFile,
        Long maxTicks,
        Long tickIntervalMillis,
        Map<String, Object> parameters
) {

    public SimulationConfig toConfig() {
        String resolvedName = (name == null || name.isBlank())
                ? "simulation-" + UUID.randomUUID()
                : name;
        long resolvedMaxTicks = maxTicks == null ? 0 : maxTicks;
        Duration interval = Duration.ofMillis(tickIntervalMillis == null ? 500 : tickIntervalMillis);
        Map<String, Object> safeParameters = parameters == null ? Map.of() : Map.copyOf(parameters);
        return new SimulationConfig(resolvedName, scenarioFile, resolvedMaxTicks, interval, safeParameters);
    }
}

