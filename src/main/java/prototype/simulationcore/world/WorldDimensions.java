package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import prototype.simulationcore.world.config.WorldConfig;

/**
 * Simple immutable structure describing the extents and topology of a world grid.
 */
public record WorldDimensions(int width, int height, String topology) implements Serializable {

    @Serial
    private static final long serialVersionUID = 736183779409247174L;

    public WorldDimensions {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("World dimensions must be positive.");
        }
        topology = topology == null || topology.isBlank() ? "GRID" : topology;
    }

    public static WorldDimensions from(WorldConfig.Grid grid) {
        if (grid == null) {
            return new WorldDimensions(32, 32, "GRID");
        }
        return new WorldDimensions(grid.getWidth(), grid.getHeight(), grid.getTopology());
    }
}


