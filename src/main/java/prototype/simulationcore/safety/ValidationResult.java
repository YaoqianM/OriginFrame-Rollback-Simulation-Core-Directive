package prototype.simulationcore.safety;

import java.util.Map;

/**
 * Result object returned by each constraint evaluation.
 */
public record ValidationResult(
        boolean valid,
        Severity severity,
        String constraintType,
        String message,
        Map<String, Object> context
) {

    public static ValidationResult passed(String constraintType) {
        return new ValidationResult(true, null, constraintType, null, Map.of());
    }

    public static ValidationResult failed(String constraintType,
                                          Severity severity,
                                          String message,
                                          Map<String, Object> context) {
        return new ValidationResult(false, severity, constraintType,
                message, context == null ? Map.of() : Map.copyOf(context));
    }
}

