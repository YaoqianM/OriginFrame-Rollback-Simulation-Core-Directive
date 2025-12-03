package prototype.integration.events;

import java.time.Instant;
import java.util.Map;

public record GridEvent(
        String scenarioId,
        String type,
        String message,
        Instant timestamp,
        Map<String, Object> details
) {
}


