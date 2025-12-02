package prototype.simulationcore.domain;

/**
 * Immutable snapshot of the agent's internal values at a specific step.
 */
public record AgentState(int stepCount, int energy) {

    public AgentState {
        if (stepCount < 0) {
            throw new IllegalArgumentException("stepCount must be non-negative");
        }
    }

    /**
     * @return baseline state for a newly created agent.
     */
    public static AgentState initial() {
        return new AgentState(0, 0);
    }

    /**
     * @param energyDelta amount to add (or subtract) from current energy
     * @return new state with the next step recorded
     */
    public AgentState next(int energyDelta) {
        return new AgentState(stepCount + 1, energy + energyDelta);
    }

    /**
     * @return a defensive copy that helps signal intent when sharing state.
     */
    public AgentState snapshot() {
        return new AgentState(stepCount, energy);
    }

    public AgentState withEnergy(int newEnergy) {
        return new AgentState(stepCount, newEnergy);
    }

    public AgentState resetSteps() {
        return new AgentState(0, energy);
    }
}

