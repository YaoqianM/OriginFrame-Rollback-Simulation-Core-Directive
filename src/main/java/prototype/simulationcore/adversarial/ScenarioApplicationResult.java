package prototype.simulationcore.adversarial;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;

/**
 * Result bundle returned after applying active adversarial scenarios.
 */
public record ScenarioApplicationResult(Environment environment, List<UUID> perturbationRecordIds) {

    public ScenarioApplicationResult {
        environment = Objects.requireNonNull(environment, "environment");
        perturbationRecordIds = perturbationRecordIds == null ? List.of() : List.copyOf(perturbationRecordIds);
    }

    public static ScenarioApplicationResult noop(Environment environment) {
        Environment resolved = environment == null
                ? new DefaultEnvironment(Position.origin(), Map.of())
                : environment;
        return new ScenarioApplicationResult(resolved, List.of());
    }
}


