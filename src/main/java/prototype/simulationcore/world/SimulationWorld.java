package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.SimulationEnvironment;
import prototype.simulationcore.world.config.WorldConfig;

/**
 * Aggregate describing the full simulation world, coordinating agents, infrastructure, and the
 * environment.
 */
public class SimulationWorld implements Serializable {

    @Serial
    private static final long serialVersionUID = -2080226345429275288L;

    private final UUID worldId;
    private final String name;
    private final WorldDimensions dimensions;
    private final Map<UUID, Agent> agents = new ConcurrentHashMap<>();
    private final List<VirtualNode> infrastructure = new CopyOnWriteArrayList<>();
    private final SimulationEnvironment environment;
    private final WorldConfig config;
    private final AtomicLong currentTick = new AtomicLong();
    private volatile WorldStatus status = WorldStatus.CREATED;

    public SimulationWorld(UUID worldId,
                           String name,
                           WorldDimensions dimensions,
                           SimulationEnvironment environment,
                           WorldConfig config) {
        this.worldId = Objects.requireNonNullElseGet(worldId, UUID::randomUUID);
        this.name = name == null || name.isBlank() ? "world-" + this.worldId.toString().substring(0, 6) : name;
        this.dimensions = Objects.requireNonNull(dimensions, "World dimensions must be provided.");
        this.environment = Objects.requireNonNull(environment, "Environment must be provided.");
        this.config = Objects.requireNonNull(config, "World config must be provided.");
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getName() {
        return name;
    }

    public WorldDimensions getDimensions() {
        return dimensions;
    }

    public long getCurrentTick() {
        return currentTick.get();
    }

    public WorldStatus getStatus() {
        return status;
    }

    public void setStatus(WorldStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public WorldConfig getConfig() {
        return config;
    }

    public SimulationEnvironment getEnvironment() {
        return environment;
    }

    public void registerAgent(Agent agent) {
        if (agent != null && agent.getAgentId() != null) {
            agents.put(agent.getAgentId(), agent);
        }
    }

    public void registerAgents(Collection<Agent> agents) {
        if (agents == null) {
            return;
        }
        agents.forEach(this::registerAgent);
    }

    public void registerInfrastructure(Collection<VirtualNode> nodes) {
        if (nodes == null) {
            return;
        }
        nodes.stream().filter(Objects::nonNull).forEach(infrastructure::add);
    }

    public void addNode(VirtualNode node) {
        if (node != null) {
            infrastructure.add(node);
        }
    }

    public WorldState advanceTick() {
        environment.tick();
        long tick = currentTick.incrementAndGet();
        return snapshotInternal(tick);
    }

    public void fastForward(long tick) {
        currentTick.set(Math.max(0, tick));
    }

    public WorldState snapshot() {
        return snapshotInternal(currentTick.get());
    }

    public List<Agent> listAgents() {
        return List.copyOf(agents.values());
    }

    public List<VirtualNode> listInfrastructure() {
        return List.copyOf(infrastructure);
    }

    private WorldState snapshotInternal(long tick) {
        Map<UUID, AgentState> agentStates = agents.values().stream()
                .filter(agent -> agent.getAgentId() != null)
                .collect(Collectors.toUnmodifiableMap(Agent::getAgentId, Agent::snapshotState));
        List<VirtualNodeState> nodeStates = infrastructure.stream()
                .map(VirtualNode::snapshot)
                .collect(Collectors.toUnmodifiableList());
        return new WorldState(
                worldId,
                name,
                dimensions,
                tick,
                agentStates,
                nodeStates,
                environment.snapshot(),
                status
        );
    }
}


