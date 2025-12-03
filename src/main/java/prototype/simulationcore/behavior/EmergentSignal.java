package prototype.simulationcore.behavior;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record EmergentSignal(
        String type,
        boolean detected,
        String summary,
        Instant detectedAt,
        Map<String, Object> metadata
) {

    public EmergentSignal {
        Objects.requireNonNull(type, "type");
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
    }

    public static EmergentSignal notDetected(String type, String summary) {
        return new EmergentSignal(type, false, summary, Instant.now(), Map.of());
    }

    public static EmergentSignal detected(String type, String summary, Map<String, Object> metadata) {
        return new EmergentSignal(type, true, summary, Instant.now(), metadata);
    }
}

