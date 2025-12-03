package prototype.simulationcore.metrics;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * Thread-safe registry tracking simulation runs in-memory.
 */
@Component
public class SimulationRunRegistry {

    private final ConcurrentMap<String, SimulationRunState> runs = new ConcurrentHashMap<>();

    public SimulationRunState stateFor(String simulationId) {
        return runs.computeIfAbsent(simulationId, SimulationRunState::new);
    }

    public Optional<SimulationRunState> findState(String simulationId) {
        return Optional.ofNullable(runs.get(simulationId));
    }

    public boolean hasState(String simulationId) {
        return runs.containsKey(simulationId);
    }
}

