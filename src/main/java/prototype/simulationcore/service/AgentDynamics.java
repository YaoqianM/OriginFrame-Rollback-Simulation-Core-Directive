package prototype.simulationcore.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.AgentState;

/**
 * Encapsulates reusable agent transition logic so other services can keep their orchestration lean.
 */
@Component
public class AgentDynamics {

    public AgentState apply(Action action, AgentState state) {
        AgentState reference = state == null ? AgentState.initial() : state;
        return switch (action) {
            case MOVE -> reference.withPosition(reference.position().offset(1, 0, 0)).adjustEnergy(-5.0);
            case CONSUME -> reference.adjustResources(10.0).adjustEnergy(2.5);
            case INTERACT -> reference.withInternalState("lastInteraction", (double) Instant.now().toEpochMilli())
                    .adjustEnergy(-1.0);
            case REPLICATE -> reference.adjustResources(-25.0).adjustEnergy(-15.0);
            case REST -> reference.adjustEnergy(5.0);
            case WAIT -> reference;
        };
    }

    public double score(Action action) {
        return switch (action) {
            case MOVE -> 0.5;
            case CONSUME -> 1.0;
            case INTERACT -> 1.5;
            case REPLICATE -> 3.0;
            case REST -> 0.25;
            case WAIT -> 0.1;
        };
    }

    public Optional<String> evaluateSafety(AgentState state) {
        AgentState reference = state == null ? AgentState.initial() : state;
        if (reference.energy() <= 0) {
            return Optional.of("Energy depleted");
        }
        if (reference.resources() < 0) {
            return Optional.of("Resources below zero");
        }
        if (reference.sensorReadings().getOrDefault("toxicity", 0.0) > 80.0) {
            return Optional.of("Environmental toxicity exceeded");
        }
        return Optional.empty();
    }
}


