package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.environment.Resource;

/**
 * Serializable capture of the environment at a given tick.
 */
public record EnvironmentState(
        int tick,
        Map<String, Resource> resources,
        Map<String, Double> environmentalFactors,
        Set<Position> obstacles
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 8910375079871852053L;
}


