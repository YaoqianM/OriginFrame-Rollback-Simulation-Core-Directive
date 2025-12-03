package prototype.simulationcore.events;

/**
 * Well-known simulation event types that get broadcast to Kafka listeners.
 */
public enum SimulationEventType {
    AGENT_SPAWNED,
    AGENT_DIED,
    NODE_FAILED,
    NODE_RECOVERED,
    CONSTRAINT_VIOLATED,
    TICK_COMPLETED,
    SCENARIO_EVENT
}

