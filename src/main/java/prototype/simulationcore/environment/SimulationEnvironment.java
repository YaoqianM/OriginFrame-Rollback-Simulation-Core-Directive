package prototype.simulationcore.environment;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.world.EnvironmentState;
import prototype.simulationcore.world.config.WorldConfig;

/**
 * Mutable environment implementation that tracks resources, obstacles, and environmental factors for
 * each world tick.
 */
public class SimulationEnvironment implements Environment, Serializable {

    @Serial
    private static final long serialVersionUID = 5783573537370204375L;

    private final Map<Position, Resource> resources = new ConcurrentHashMap<>();
    private final Set<Position> obstacles = ConcurrentHashMap.newKeySet();
    private final Map<String, Double> environmentalFactors = new ConcurrentHashMap<>();
    private final WorldConfig.Physics physics;
    private final AtomicInteger tickCounter = new AtomicInteger();
    private volatile Position targetPosition = Position.origin();

    public SimulationEnvironment(WorldConfig.Physics physics,
                                 Map<Position, Resource> initialResources,
                                 Set<Position> initialObstacles,
                                 Map<String, Double> initialFactors) {
        this.physics = physics == null ? new WorldConfig.Physics() : physics;
        if (initialResources != null) {
            resources.putAll(initialResources);
        }
        if (initialObstacles != null) {
            obstacles.addAll(initialObstacles);
        }
        if (initialFactors != null) {
            environmentalFactors.putAll(initialFactors);
        }
    }

    public void registerResource(Position position, Resource resource) {
        if (position != null && resource != null) {
            resources.put(position, resource);
        }
    }

    public void addObstacle(Position position) {
        if (position != null) {
            obstacles.add(position);
        }
    }

    public void setTargetPosition(Position position) {
        if (position != null) {
            targetPosition = position;
        }
    }

    public void setEnvironmentalFactor(String key, Double value) {
        if (key != null && !key.isBlank() && value != null) {
            environmentalFactors.put(key, value);
        }
    }

    /**
     * Advances the environment state by a single tick applying passive physics effects.
     */
    public void tick() {
        resources.replaceAll((position, resource) -> resource == null ? null : resource.regenerate());
        double energyDecay = Math.max(physics.getEnergyDecay(), 0.0001);
        environmentalFactors.compute("weatherSeverity", (key, current) -> {
            double base = current == null ? 0.5 : current;
            double delta = ThreadLocalRandom.current().nextDouble(-energyDecay, energyDecay);
            return clamp(base + delta, 0.0, 1.0);
        });
        environmentalFactors.compute("temperature", (key, current) -> {
            double base = current == null ? 20.0 : current;
            double delta = ThreadLocalRandom.current().nextDouble(-1.5, 1.5) - physics.getFriction();
            return clamp(base + delta, -50.0, 70.0);
        });
        tickCounter.incrementAndGet();
    }

    public EnvironmentState snapshot() {
        return new EnvironmentState(
                tickCounter.get(),
                encodeResources(),
                Map.copyOf(environmentalFactors),
                Set.copyOf(obstacles)
        );
    }

    @Override
    public Position getTargetPosition() {
        return targetPosition;
    }

    @Override
    public double readSignal(String key) {
        return environmentalFactors.getOrDefault(key, 0.0);
    }

    @Override
    public Map<String, Double> snapshotSensors() {
        return Map.copyOf(environmentalFactors);
    }

    private Map<String, Resource> encodeResources() {
        Map<String, Resource> snapshot = new HashMap<>();
        resources.forEach((position, resource) -> snapshot.put(encodePosition(position), resource));
        return snapshot;
    }

    private String encodePosition(Position position) {
        Position safe = position == null ? Position.origin() : position;
        return safe.x() + "," + safe.y() + "," + safe.z();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


