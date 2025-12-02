package prototype.simulationcore.domain;

import java.util.Objects;

/**
 * Minimal digital agent that holds an identifier and mutable state reference.
 */
public class Agent {

    private final String id;
    private AgentState state;

    public Agent(String id) {
        this(id, AgentState.initial());
    }

    public Agent(String id, AgentState initialState) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = initialState == null ? AgentState.initial() : initialState;
    }

    public synchronized String getId() {
        return id;
    }

    public synchronized AgentState getState() {
        return state;
    }

    public synchronized AgentState snapshotState() {
        return state.snapshot();
    }

    public synchronized AgentState replaceState(AgentState newState) {
        this.state = Objects.requireNonNull(newState, "newState");
        return state;
    }
}

