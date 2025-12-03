package prototype.simulationcore.safety;

import java.time.Instant;
import java.util.Map;
import prototype.simulationcore.domain.Action;

/**
 * In-memory representation of a detected safety violation.
 */
public record Violation(
        String constraintType,
        Severity severity,
        String message,
        Action actionAttempted,
        Map<String, Object> context,
        Instant occurredAt
) {

    public static Violation fromResult(ValidationResult result, Action action) {
        Severity severity = result.severity() == null ? Severity.VIOLATION : result.severity();
        return new Violation(result.constraintType(), severity, result.message(),
                action, result.context(), Instant.now());
    }
}

