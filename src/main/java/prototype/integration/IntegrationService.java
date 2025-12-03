package prototype.integration;

import jakarta.annotation.PreDestroy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import prototype.integration.config.GridDemoProperties;
import prototype.integration.events.GridEvent;
import prototype.integration.events.GridRecoveryEvent;
import prototype.integration.grid.GridAgentProfile;
import prototype.integration.grid.GridNodeView;
import prototype.integration.grid.GridScenarioReport;
import prototype.integration.grid.GridScenarioState;
import prototype.integration.grid.GridScenarioStatus;
import prototype.lineageruntime.recovery.ServiceSnapshot;
import prototype.lineageruntime.recovery.ServiceTopology;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.evolution.dto.AgentSummary;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.evolution.selection.SelectionSettings;
import prototype.simulationcore.evolution.service.EvolutionLoopService;
import prototype.simulationcore.infrastructure.FaultInjector;
import prototype.simulationcore.infrastructure.InfrastructureTopologyView;
import prototype.simulationcore.infrastructure.VirtualInfrastructureService;
import prototype.simulationcore.infrastructure.VirtualNodeStatus;
import prototype.simulationcore.service.SimulationService;
import prototype.lineageruntime.kafka.EventConsumer;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);
    private static final String GRID_EVENTS_TOPIC = "grid-events";
    private static final String GRID_LINEAGE_TOPIC = "grid-lineage";
    private static final String GRID_RECOVERY_TOPIC = "grid-recovery";

    private final SimulationService simulationService;
    private final EvolutionLoopService evolutionLoopService;
    private final VirtualInfrastructureService infrastructureService;
    private final FaultInjector faultInjector;
    private final ServiceTopology serviceTopology;
    private final EventConsumer eventConsumer;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GridDemoProperties gridProperties;
    private final GridScenarioState scenarioState;
    private final ExecutorService executor;
    private final Set<String> routedLineageEvents = ConcurrentHashMap.newKeySet();

    private Future<?> activeRun;

    public IntegrationService(SimulationService simulationService,
                              EvolutionLoopService evolutionLoopService,
                              VirtualInfrastructureService infrastructureService,
                              FaultInjector faultInjector,
                              ServiceTopology serviceTopology,
                              EventConsumer eventConsumer,
                              KafkaTemplate<String, Object> kafkaTemplate,
                              GridDemoProperties gridProperties) {
        this.simulationService = simulationService;
        this.evolutionLoopService = evolutionLoopService;
        this.infrastructureService = infrastructureService;
        this.faultInjector = faultInjector;
        this.serviceTopology = serviceTopology;
        this.eventConsumer = eventConsumer;
        this.kafkaTemplate = kafkaTemplate;
        this.gridProperties = gridProperties;
        this.scenarioState = new GridScenarioState(
                "self-healing-grid",
                Math.max(1, gridProperties.getTargetTicks()),
                Math.max(1, gridProperties.getFailureTick()),
                gridProperties.getFailureNodeId()
        );
        this.executor = Executors.newSingleThreadExecutor(new CustomizableThreadFactory("grid-demo-"));
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    public synchronized GridScenarioStatus setupScenario() {
        cancelActiveRun();
        try {
            List<GridNodeView> nodeViews = resolveNodeViews(0);
            SelectionSettings settings = SelectionSettings.defaults(Math.max(1, gridProperties.getAgents()));
            EvolutionStatus status = evolutionLoopService.initializePopulation(
                    Math.max(1, gridProperties.getAgents()),
                    null,
                    settings,
                    0.25
            );
            scenarioState.applySetup(nodeViews, status.runId(), status);
            publishGridEvent("SETUP", "Self-Healing Grid world initialized",
                    Map.of("nodes", nodeViews.size(), "population", status.populationSize()));
            return scenarioState.snapshotStatus();
        } catch (Exception ex) {
            log.error("Failed to setup Self-Healing Grid scenario", ex);
            throw new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to setup scenario: " + ex.getMessage(), ex);
        }
    }

    public synchronized GridScenarioStatus startScenario() {
        if (!scenarioState.isReady()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Scenario must be set up before starting");
        }
        cancelActiveRun();
        scenarioState.markRunning();
        publishGridEvent("START", "Self-Healing Grid demo started",
                Map.of("targetTicks", scenarioState.targetTicks(), "failureTick", scenarioState.failureTick()));
        activeRun = executor.submit(this::runScenarioLoop);
        return scenarioState.snapshotStatus();
    }

    public GridScenarioStatus status() {
        return scenarioState.snapshotStatus();
    }

    public GridScenarioStatus requestFailureInjection() {
        scenarioState.requestFailure();
        publishGridEvent("FAILURE_REQUESTED", "Manual fault injection requested",
                Map.of("failureTick", scenarioState.failureTick(), "preferredNode",
                        scenarioState.preferredFailureServiceId()));
        return scenarioState.snapshotStatus();
    }

    public GridScenarioReport report() {
        return scenarioState.snapshotReport();
    }

    private void runScenarioLoop() {
        try {
            for (int tick = scenarioState.currentTick() + 1; tick <= scenarioState.targetTicks(); tick++) {
                simulationService.step();
                scenarioState.recordTick(tick);
                routeLineageEvents();
                processEvolutionTick();

                List<GridNodeView> nodeViews = resolveNodeViews(tick);
                scenarioState.updateNodes(nodeViews);

                if (scenarioState.shouldInjectFailure(tick)) {
                    handleFailure();
                }

                Thread.sleep(75L);
            }
            scenarioState.markCompleted();
            publishGridEvent("COMPLETE", "Self-Healing Grid demo completed",
                    Map.of("ticksExecuted", scenarioState.currentTick()));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scenarioState.markFailed("Scenario interrupted");
            publishGridEvent("FAILED", "Scenario interrupted", Map.of());
        } catch (Exception ex) {
            log.error("Error during grid scenario run", ex);
            scenarioState.markFailed(ex.getMessage());
            publishGridEvent("FAILED", ex.getMessage(), Map.of());
        }
    }

    private void processEvolutionTick() {
        try {
            GenerationReport report = evolutionLoopService.runGeneration();
            EvolutionStatus status = evolutionLoopService.status();
            scenarioState.recordGeneration(report, toAgentProfiles(report));
            scenarioState.updateEvolutionStatus(status);
        } catch (IllegalStateException ex) {
            log.warn("Evolution engine not initialized: {}", ex.getMessage());
        }
    }

    private List<GridAgentProfile> toAgentProfiles(GenerationReport report) {
        if (report == null || report.bestAgents().isEmpty()) {
            return List.of();
        }
        List<GridAgentProfile> profiles = new ArrayList<>();
        int rank = 1;
        for (AgentSummary summary : report.bestAgents()) {
            profiles.add(new GridAgentProfile(
                    summary.agentId(),
                    summary.fitness(),
                    summary.cumulativeReward(),
                    summary.safetyViolations(),
                    report.generation(),
                    "Top-" + rank++
            ));
        }
        return profiles;
    }

    private void handleFailure() {
        String serviceId = scenarioState.preferredFailureServiceId();
        UUID nodeId = infrastructureService.nodeIdForService(serviceId);
        if (nodeId == null) {
            log.warn("Unable to locate infrastructure node for service {}", serviceId);
            scenarioState.recordFailure(serviceId, new FaultInjector.InfrastructureFaultImpact(
                    UUID.randomUUID(),
                    serviceId,
                    VirtualNodeStatus.UNKNOWN,
                    VirtualNodeStatus.UNKNOWN,
                    false,
                    false,
                    List.of(),
                    "Unknown node for service " + serviceId
            ));
            scenarioState.recordRecovery(false);
            return;
        }
        FaultInjector.InfrastructureFaultImpact impact = faultInjector.failNode(nodeId);
        String scenarioNodeId = resolveScenarioNodeId(serviceId);
        scenarioState.recordFailure(scenarioNodeId, impact);
        scenarioState.recordRecovery(impact.recovered());
        publishGridEvent("FAILURE_INJECTED", impact.message(),
                Map.of("node", scenarioNodeId, "recovered", impact.recovered()));
        GridRecoveryEvent event = new GridRecoveryEvent(
                scenarioState.snapshotStatus().scenarioId(),
                scenarioNodeId,
                serviceId,
                impact.recoveryTriggered(),
                impact.recovered(),
                impact.message(),
                impact.recoveries(),
                Instant.now()
        );
        kafkaTemplate.send(GRID_RECOVERY_TOPIC, serviceId, event);
    }

    private List<GridNodeView> resolveNodeViews(int tick) {
        InfrastructureTopologyView topology = infrastructureService.currentTopology();
        List<GridNodeView> views = new ArrayList<>();
        if (gridProperties.getNodes().isEmpty()) {
            topology.nodes().stream()
                    .limit(5)
                    .forEach(node -> views.add(toGridNodeView(node, null, tick)));
            return views;
        }

        for (GridDemoProperties.VirtualNodeSpec spec : gridProperties.getNodes()) {
            InfrastructureTopologyView.NodeView node = findNodeForService(topology, spec.getServiceId());
            GridNodeView view = node == null
                    ? fallbackNodeView(spec, tick)
                    : toGridNodeView(node, spec, tick);
            views.add(view);
        }
        return views;
    }

    private InfrastructureTopologyView.NodeView findNodeForService(InfrastructureTopologyView topology,
                                                                   String serviceId) {
        return topology.nodes().stream()
                .filter(node -> node.services().stream()
                        .anyMatch(service -> service.id().equalsIgnoreCase(serviceId)))
                .findFirst()
                .orElse(null);
    }

    private GridNodeView toGridNodeView(InfrastructureTopologyView.NodeView node,
                                        GridDemoProperties.VirtualNodeSpec spec,
                                        int tick) {
        String nodeId = spec != null ? spec.getNodeId() : node.name();
        String displayName = spec != null ? spec.getName() : node.name();
        String serviceId = spec != null ? spec.getServiceId() : node.services().isEmpty()
                ? node.name()
                : node.services().get(0).id();
        ServiceSnapshot snapshot = serviceTopology.snapshot(serviceId);
        double cpuUtil = utilization(node.cpuLoad(), node.cpuCapacity());
        double memUtil = utilization(node.memoryLoad(), node.memoryCapacity());
        return new GridNodeView(
                nodeId,
                displayName,
                serviceId,
                node.status(),
                snapshot.status(),
                cpuUtil,
                memUtil,
                node.degradationRatio(),
                snapshot.fallbackActive(),
                tick
        );
    }

    private GridNodeView fallbackNodeView(GridDemoProperties.VirtualNodeSpec spec, int tick) {
        ServiceSnapshot snapshot = serviceTopology.snapshot(spec.getServiceId());
        return new GridNodeView(
                spec.getNodeId(),
                spec.getName(),
                spec.getServiceId(),
                VirtualNodeStatus.UNKNOWN,
                snapshot.status(),
                0.0,
                0.0,
                0.0,
                snapshot.fallbackActive(),
                tick
        );
    }

    private double utilization(double load, double capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        double ratio = load / capacity;
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    private void routeLineageEvents() {
        List<LineageEvent> history = eventConsumer.getHistory();
        for (LineageEvent event : history) {
            if (event == null || event.getEventId() == null) {
                continue;
            }
            if (routedLineageEvents.add(event.getEventId().toString())) {
                kafkaTemplate.send(GRID_LINEAGE_TOPIC, event.getAgentId(), event);
            }
        }
    }

    private void cancelActiveRun() {
        if (activeRun != null && !activeRun.isDone()) {
            activeRun.cancel(true);
        }
    }

    private void publishGridEvent(String type, String message, Map<String, Object> details) {
        GridEvent event = new GridEvent(
                scenarioState.snapshotStatus().scenarioId(),
                type,
                message,
                Instant.now(),
                details == null ? Map.of() : Map.copyOf(details)
        );
        kafkaTemplate.send(GRID_EVENTS_TOPIC, type, event);
    }

    private String resolveScenarioNodeId(String serviceId) {
        return gridProperties.getNodes().stream()
                .filter(spec -> spec.getServiceId().equalsIgnoreCase(serviceId))
                .map(GridDemoProperties.VirtualNodeSpec::getNodeId)
                .findFirst()
                .orElse(serviceId);
    }
}


