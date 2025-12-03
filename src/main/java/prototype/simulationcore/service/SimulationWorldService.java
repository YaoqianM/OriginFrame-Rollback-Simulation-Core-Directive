package prototype.simulationcore.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.dto.SimulationCreateRequest;
import prototype.simulationcore.environment.Resource;
import prototype.simulationcore.environment.SimulationEnvironment;
import prototype.simulationcore.metrics.MetricsCollector;
import prototype.simulationcore.metrics.SimulationWorldSnapshot;
import prototype.simulationcore.repository.AgentRepository;
import prototype.simulationcore.repository.SimulationRunRepository;
import prototype.simulationcore.world.SimulationRun;
import prototype.simulationcore.world.SimulationWorld;
import prototype.simulationcore.world.VirtualNode;
import prototype.simulationcore.world.VirtualNodeStatus;
import prototype.simulationcore.world.WorldDimensions;
import prototype.simulationcore.world.WorldState;
import prototype.simulationcore.world.WorldStatus;
import prototype.simulationcore.world.config.WorldConfig;
import prototype.simulationcore.timeline.TimelineEvent;
import prototype.simulationcore.timeline.TimelineRecorder;

@Service
public class SimulationWorldService {

    private final SimulationRunRepository simulationRunRepository;
    private final AgentRepository agentRepository;
    private final AgentPolicyBootstrapper policyBootstrapper;
    private final WorldConfig baseWorldConfig;
    private final ObjectMapper objectMapper;
    private final MetricsCollector metricsCollector;
    private final TimelineRecorder timelineRecorder;
    private final Map<UUID, SimulationWorld> activeWorlds = new ConcurrentHashMap<>();

    public SimulationWorldService(SimulationRunRepository simulationRunRepository,
                                  AgentRepository agentRepository,
                                  AgentPolicyBootstrapper policyBootstrapper,
                                  WorldConfig baseWorldConfig,
                                  ObjectMapper objectMapper,
                                  MetricsCollector metricsCollector,
                                  TimelineRecorder timelineRecorder) {
        this.simulationRunRepository = simulationRunRepository;
        this.agentRepository = agentRepository;
        this.policyBootstrapper = policyBootstrapper;
        this.baseWorldConfig = baseWorldConfig;
        this.objectMapper = objectMapper;
        this.metricsCollector = metricsCollector;
        this.timelineRecorder = timelineRecorder;
    }

    @Transactional
    public SimulationRun createWorld(SimulationCreateRequest request) {
        WorldConfig overrides = request == null ? null : request.config();
        WorldConfig effectiveConfig = baseWorldConfig.merged(overrides);
        SimulationWorld world = new SimulationWorld(
                UUID.randomUUID(),
                request == null ? null : request.name(),
                WorldDimensions.from(effectiveConfig.getGrid()),
                buildEnvironment(effectiveConfig),
                effectiveConfig.copy()
        );
        world.setStatus(WorldStatus.RUNNING);
        world.registerInfrastructure(buildInfrastructure(effectiveConfig));
        world.registerAgents(prepareAgents(effectiveConfig.getInitialAgentCount()));

        SimulationRun run = new SimulationRun();
        run.setWorldId(world.getWorldId());
        run.setWorldName(world.getName());
        run.setWorldConfigJson(writeConfig(effectiveConfig));
        run.setStartTime(Instant.now());
        run.setStatus(WorldStatus.RUNNING);
        SimulationRun persisted = simulationRunRepository.save(run);

        activeWorlds.put(world.getWorldId(), world);
        recordLifecycleEvent(world, persisted, "WORLD_CREATED", "Simulation world initialized.");
        return persisted;
    }

    @Transactional
    public WorldState snapshot(UUID runId) {
        SimulationRun run = simulationRunRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Simulation run not found: " + runId));
        SimulationWorld world = loadWorld(run);
        WorldState state = world.snapshot();
        run.recordTick(state.tick());
        simulationRunRepository.save(run);
        recordSnapshot(run, world, state);
        return state;
    }

    private SimulationWorld loadWorld(SimulationRun run) {
        return activeWorlds.computeIfAbsent(run.getWorldId(), ignored -> rebuildWorld(run));
    }

    private SimulationWorld rebuildWorld(SimulationRun run) {
        WorldConfig config = readConfig(run.getWorldConfigJson());
        SimulationWorld world = new SimulationWorld(
                run.getWorldId(),
                run.getWorldName(),
                WorldDimensions.from(config.getGrid()),
                buildEnvironment(config),
                config.copy()
        );
        world.setStatus(run.getStatus());
        world.registerInfrastructure(buildInfrastructure(config));
        world.registerAgents(prepareAgents(config.getInitialAgentCount()));
        world.fastForward(run.getTotalTicks());
        return world;
    }

    private SimulationEnvironment buildEnvironment(WorldConfig config) {
        int width = config.getGrid().getWidth();
        int height = config.getGrid().getHeight();
        int resourceCount = Math.max(1,
                (int) Math.round(width * height * Math.max(config.getResourceDistribution().getDensity(), 0.05)));

        Map<Position, Resource> resources = new HashMap<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < resourceCount; i++) {
            Position position = new Position(random.nextDouble(width), random.nextDouble(height), 0.0);
            double quantity = random.nextDouble(
                    config.getResourceDistribution().getMinQuantity(),
                    config.getResourceDistribution().getMaxQuantity());
            resources.put(position, new Resource(
                    config.getResourceDistribution().getDefaultResourceType(),
                    quantity,
                    config.getResourceDistribution().getRegenerationRate()));
        }

        int obstacleCount = Math.max(1, resourceCount / 4);
        var obstacles = IntStream.range(0, obstacleCount)
                .mapToObj(i -> new Position(random.nextDouble(width), random.nextDouble(height), 0.0))
                .collect(Collectors.toSet());

        Map<String, Double> factors = new HashMap<>();
        factors.put("temperature", 20.0 + random.nextDouble(-5.0, 5.0));
        factors.put("weatherSeverity", random.nextDouble(0.0, 1.0));
        factors.put("wind", random.nextDouble(0.0, 1.0));

        SimulationEnvironment environment = new SimulationEnvironment(config.getPhysics(), resources, obstacles, factors);
        environment.setTargetPosition(new Position(width / 2.0, height / 2.0, 0.0));
        return environment;
    }

    private List<VirtualNode> buildInfrastructure(WorldConfig config) {
        int width = config.getGrid().getWidth();
        int height = config.getGrid().getHeight();
        List<VirtualNode> nodes = new ArrayList<>();
        nodes.add(new VirtualNode("ingest-gateway", new Position(0, 0, 0)));
        nodes.add(new VirtualNode("processing-cluster", new Position(width / 2.0, height / 2.0, 0)));
        nodes.add(new VirtualNode("storage-array", new Position(width, height / 3.0, 0)));
        nodes.add(new VirtualNode("analytics-node", new Position(width / 3.0, height, 0)));
        return nodes;
    }

    private List<Agent> prepareAgents(int requestedCount) {
        int targetCount = Math.max(requestedCount, 1);
        List<Agent> agents = agentRepository.findAll(PageRequest.of(0, targetCount)).getContent();
        if (agents.size() >= targetCount) {
            return agents;
        }

        while (agents.size() < targetCount) {
            Agent seeded = Agent.bootstrap(policyBootstrapper.resolveDefaultPolicy());
            agents.add(agentRepository.save(seeded));
        }
        return agents;
    }

    private String writeConfig(WorldConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to persist world configuration.", e);
        }
    }

    private WorldConfig readConfig(String json) {
        if (json == null || json.isBlank()) {
            return baseWorldConfig.copy();
        }
        try {
            return objectMapper.readValue(json, WorldConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to read stored world configuration.", e);
        }
    }

    private void recordSnapshot(SimulationRun run, SimulationWorld world, WorldState state) {
        if (metricsCollector == null || timelineRecorder == null || world == null || state == null) {
            return;
        }
        String simulationId = run.getWorldId().toString();
        int tick = safeTick(state.tick());
        List<VirtualNode> nodes = world.listInfrastructure();
        long failedNodes = nodes.stream().filter(node -> node.getStatus() == VirtualNodeStatus.OFFLINE).count();
        long healthyNodes = nodes.size() - failedNodes;

        SimulationWorldSnapshot snapshot = SimulationWorldSnapshot.builder(simulationId)
                .tick(tick)
                .capturedAt(Instant.now())
                .agents(world.listAgents())
                .healthyNodes((int) healthyNodes)
                .failedNodes((int) failedNodes)
                .networkLatencyAvg(estimateNetworkLatency(world, nodes, failedNodes))
                .build();
        metricsCollector.record(snapshot);

        timelineRecorder.recordEvent(TimelineEvent.builder()
                .simulationId(simulationId)
                .tick(tick)
                .eventType("WORLD_SNAPSHOT")
                .description("World snapshot captured")
                .timestamp(snapshot.capturedAt())
                .metadata(Map.of(
                        "agentCount", snapshot.agents().size(),
                        "healthyNodes", snapshot.healthyNodes(),
                        "failedNodes", snapshot.failedNodes()
                ))
                .build());
    }

    private void recordLifecycleEvent(SimulationWorld world,
                                      SimulationRun run,
                                      String eventType,
                                      String description) {
        if (timelineRecorder == null || world == null || run == null) {
            return;
        }
        timelineRecorder.recordEvent(TimelineEvent.builder()
                .simulationId(run.getWorldId().toString())
                .tick(0)
                .eventType(eventType)
                .description(description)
                .timestamp(Instant.now())
                .metadata(Map.of(
                        "worldName", world.getName(),
                        "dimensions", world.getDimensions().toString(),
                        "status", world.getStatus().name()
                ))
                .build());
    }

    private double estimateNetworkLatency(SimulationWorld world,
                                          List<VirtualNode> nodes,
                                          long failedNodes) {
        double weatherSeverity = world.getEnvironment().readSignal("weatherSeverity");
        double baseLatency = 5.0 + weatherSeverity * 40.0;
        double nodePenalty = failedNodes * 7.5;
        double metadataAverage = nodes.stream()
                .map(node -> node.getMetadata().get("latencyMs"))
                .filter(Number.class::isInstance)
                .mapToDouble(value -> ((Number) value).doubleValue())
                .average()
                .orElse(0.0);
        return Math.max(1.0, baseLatency + nodePenalty + metadataAverage);
    }

    private int safeTick(long tick) {
        if (tick > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (tick < 0) {
            return 0;
        }
        return (int) tick;
    }
}


