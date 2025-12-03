package prototype.simulationcore.orchestrator;

import java.time.Instant;
import java.util.Map;

public record SimulationTickResult(
        java.util.UUID simulationId,
        long tick,
        Instant processedAt,
        boolean constraintViolated,
        Map<String, Object> environmentSnapshot
) {
}

