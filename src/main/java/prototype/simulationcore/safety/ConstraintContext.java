package prototype.simulationcore.safety;

/**
 * Thread-local context that exposes the current constraint evaluation phase.
 */
public final class ConstraintContext {

    private static final ThreadLocal<ConstraintPhase> PHASE = new ThreadLocal<>();

    private ConstraintContext() {
    }

    public static ConstraintPhase getPhase() {
        return PHASE.get();
    }

    public static void setPhase(ConstraintPhase phase) {
        PHASE.set(phase);
    }

    public static void clear() {
        PHASE.remove();
    }
}

