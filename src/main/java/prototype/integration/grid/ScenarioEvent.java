package prototype.integration.grid;

import java.time.Instant;
import java.util.Map;

public record ScenarioEvent(
        String type,
        String message,
        Instant timestamp,
        Map<String, Object> details
) {
}


