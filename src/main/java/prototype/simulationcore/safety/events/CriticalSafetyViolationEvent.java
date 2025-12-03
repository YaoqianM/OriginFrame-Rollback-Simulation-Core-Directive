package prototype.simulationcore.safety.events;

import java.util.UUID;
import prototype.simulationcore.safety.Violation;

public record CriticalSafetyViolationEvent(UUID agentId, Violation violation) {
}

