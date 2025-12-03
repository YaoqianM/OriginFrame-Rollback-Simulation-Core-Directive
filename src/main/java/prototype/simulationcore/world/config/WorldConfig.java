package prototype.simulationcore.world.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Declarative configuration for constructing simulation worlds. Values are sourced from YAML by
 * default but can also be overridden through JSON payloads.
 */
@Validated
@JsonIgnoreProperties(ignoreUnknown = true)
@ConfigurationProperties(prefix = "simulation.world")
public class WorldConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 6109174327332917659L;

    private Grid grid = new Grid();
    private int initialAgentCount = 1;
    private ResourceDistribution resourceDistribution = new ResourceDistribution();
    private Physics physics = new Physics();

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid == null ? new Grid() : grid.copy();
    }

    public int getInitialAgentCount() {
        return initialAgentCount;
    }

    public void setInitialAgentCount(int initialAgentCount) {
        this.initialAgentCount = initialAgentCount <= 0 ? this.initialAgentCount : initialAgentCount;
    }

    public ResourceDistribution getResourceDistribution() {
        return resourceDistribution;
    }

    public void setResourceDistribution(ResourceDistribution resourceDistribution) {
        this.resourceDistribution =
                resourceDistribution == null ? new ResourceDistribution() : resourceDistribution.copy();
    }

    public Physics getPhysics() {
        return physics;
    }

    public void setPhysics(Physics physics) {
        this.physics = physics == null ? new Physics() : physics.copy();
    }

    /**
     * @return deep copy to ensure callers never mutate the live configuration bean.
     */
    public WorldConfig copy() {
        WorldConfig copy = new WorldConfig();
        copy.setGrid(grid);
        copy.setInitialAgentCount(initialAgentCount);
        copy.setResourceDistribution(resourceDistribution);
        copy.setPhysics(physics);
        return copy;
    }

    /**
     * Creates a copy with optional overrides applied.
     */
    public WorldConfig merged(WorldConfig overrides) {
        WorldConfig effective = copy();
        if (overrides == null) {
            return effective;
        }
        if (overrides.initialAgentCount > 0) {
            effective.initialAgentCount = overrides.initialAgentCount;
        }
        effective.grid.apply(overrides.grid);
        effective.resourceDistribution.apply(overrides.resourceDistribution);
        effective.physics.apply(overrides.physics);
        return effective;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Grid implements Serializable {

        @Serial
        private static final long serialVersionUID = 741352671823908998L;

        private int width = 32;
        private int height = 32;
        private String topology = "GRID";

        public int getWidth() {
            return width;
        }

        public void setWidth(int width) {
            this.width = width > 0 ? width : this.width;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height > 0 ? height : this.height;
        }

        public String getTopology() {
            return topology;
        }

        public void setTopology(String topology) {
            this.topology = topology == null || topology.isBlank() ? this.topology : topology;
        }

        private Grid copy() {
            Grid copy = new Grid();
            copy.setWidth(width);
            copy.setHeight(height);
            copy.setTopology(topology);
            return copy;
        }

        private void apply(Grid override) {
            if (override == null) {
                return;
            }
            setWidth(override.width);
            setHeight(override.height);
            setTopology(override.topology);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResourceDistribution implements Serializable {

        @Serial
        private static final long serialVersionUID = -3300342871636313207L;

        private double density = 0.25;
        private double minQuantity = 5.0;
        private double maxQuantity = 25.0;
        private double regenerationRate = 0.5;
        private String defaultResourceType = "ENERGY";

        public double getDensity() {
            return density;
        }

        public void setDensity(double density) {
            this.density = density > 0 ? density : this.density;
        }

        public double getMinQuantity() {
            return minQuantity;
        }

        public void setMinQuantity(double minQuantity) {
            this.minQuantity = minQuantity >= 0 ? minQuantity : this.minQuantity;
        }

        public double getMaxQuantity() {
            return maxQuantity;
        }

        public void setMaxQuantity(double maxQuantity) {
            this.maxQuantity = maxQuantity >= minQuantity ? maxQuantity : this.maxQuantity;
        }

        public double getRegenerationRate() {
            return regenerationRate;
        }

        public void setRegenerationRate(double regenerationRate) {
            this.regenerationRate = regenerationRate >= 0 ? regenerationRate : this.regenerationRate;
        }

        public String getDefaultResourceType() {
            return defaultResourceType;
        }

        public void setDefaultResourceType(String defaultResourceType) {
            this.defaultResourceType =
                    defaultResourceType == null || defaultResourceType.isBlank() ? this.defaultResourceType
                            : defaultResourceType;
        }

        private ResourceDistribution copy() {
            ResourceDistribution copy = new ResourceDistribution();
            copy.setDensity(density);
            copy.setMinQuantity(minQuantity);
            copy.setMaxQuantity(maxQuantity);
            copy.setRegenerationRate(regenerationRate);
            copy.setDefaultResourceType(defaultResourceType);
            return copy;
        }

        private void apply(ResourceDistribution override) {
            if (override == null) {
                return;
            }
            setDensity(override.density);
            setMinQuantity(override.minQuantity);
            setMaxQuantity(override.maxQuantity);
            setRegenerationRate(override.regenerationRate);
            setDefaultResourceType(override.defaultResourceType);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Physics implements Serializable {

        @Serial
        private static final long serialVersionUID = -751530783783137875L;

        private Duration tickInterval = Duration.ofSeconds(1);
        private double friction = 0.05;
        private double gravity = 9.81;
        private double energyDecay = 0.01;

        public Duration getTickInterval() {
            return tickInterval;
        }

        public void setTickInterval(Duration tickInterval) {
            this.tickInterval = tickInterval == null ? this.tickInterval : tickInterval;
        }

        public double getFriction() {
            return friction;
        }

        public void setFriction(double friction) {
            this.friction = friction >= 0 ? friction : this.friction;
        }

        public double getGravity() {
            return gravity;
        }

        public void setGravity(double gravity) {
            this.gravity = gravity >= 0 ? gravity : this.gravity;
        }

        public double getEnergyDecay() {
            return energyDecay;
        }

        public void setEnergyDecay(double energyDecay) {
            this.energyDecay = energyDecay >= 0 ? energyDecay : this.energyDecay;
        }

        private Physics copy() {
            Physics copy = new Physics();
            copy.setTickInterval(tickInterval);
            copy.setFriction(friction);
            copy.setGravity(gravity);
            copy.setEnergyDecay(energyDecay);
            return copy;
        }

        private void apply(Physics override) {
            if (override == null) {
                return;
            }
            setTickInterval(override.tickInterval);
            setFriction(override.friction);
            setGravity(override.gravity);
            setEnergyDecay(override.energyDecay);
        }
    }
}


