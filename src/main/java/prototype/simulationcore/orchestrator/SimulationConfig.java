package prototype.simulationcore.orchestrator;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public final class SimulationConfig {

    private final String name;
    private final String scenarioFile;
    private final long maxTicks;
    private final Duration tickInterval;
    private final Map<String, Object> parameters;

    public SimulationConfig(String name,
                            String scenarioFile,
                            long maxTicks,
                            Duration tickInterval,
                            Map<String, Object> parameters) {
        this.name = Objects.requireNonNull(name, "name");
        this.scenarioFile = scenarioFile;
        this.maxTicks = Math.max(0, maxTicks);
        this.tickInterval = tickInterval == null ? Duration.ofMillis(250) : tickInterval;
        this.parameters = parameters == null ? Map.of() : Map.copyOf(parameters);
    }

    public String getName() {
        return name;
    }

    public String getScenarioFile() {
        return scenarioFile;
    }

    public long getMaxTicks() {
        return maxTicks;
    }

    public Duration getTickInterval() {
        return tickInterval;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}

