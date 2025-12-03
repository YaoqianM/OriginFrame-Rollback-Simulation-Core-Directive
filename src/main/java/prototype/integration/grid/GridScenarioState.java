package prototype.integration.grid;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import prototype.simulationcore.evolution.dto.EvolutionStatus;
import prototype.simulationcore.evolution.dto.GenerationReport;
import prototype.simulationcore.infrastructure.FaultInjector;

public class GridScenarioState {

    private static final int EVENT_LIMIT = 64;

    private final String scenarioId;
    private final int targetTicks;
    private final int failureTick;
    private final String preferredFailureServiceId;

    private ScenarioPhase phase = ScenarioPhase.NOT_READY;
    private Instant setupAt;
    private Instant startedAt;
    private Instant completedAt;
    private int currentTick;
    private boolean failureRequested;
    private boolean failureInjected;
    private String failingNodeId;
    private UUID evolutionRunId;
    private EvolutionStatus evolutionStatus = EvolutionStatus.idle();
    private GenerationReport latestGeneration = GenerationReport.empty();
    private FaultInjector.InfrastructureFaultImpact lastFaultImpact;
    private final Map<String, GridNodeView> nodes = new LinkedHashMap<>();
    private List<GridAgentProfile> agents = new ArrayList<>();
    private final Deque<ScenarioEvent> events = new ArrayDeque<>();

    public GridScenarioState(String scenarioId,
                             int targetTicks,
                             int failureTick,
                             String preferredFailureServiceId) {
        this.scenarioId = scenarioId;
        this.targetTicks = targetTicks;
        this.failureTick = failureTick;
        this.preferredFailureServiceId = preferredFailureServiceId;
        this.setupAt = Instant.now();
    }

    public synchronized void applySetup(List<GridNodeView> nodeViews,
                                        UUID runId,
                                        EvolutionStatus status) {
        nodes.clear();
        nodeViews.forEach(view -> nodes.put(view.nodeId(), view));
        agents = new ArrayList<>();
        events.clear();
        evolutionRunId = runId;
        evolutionStatus = status == null ? EvolutionStatus.idle() : status;
        latestGeneration = GenerationReport.empty();
        currentTick = 0;
        failureRequested = false;
        failureInjected = false;
        failingNodeId = null;
        lastFaultImpact = null;
        setupAt = Instant.now();
        startedAt = null;
        completedAt = null;
        phase = ScenarioPhase.READY;
        logEvent("SETUP", "Self-Healing Grid world initialized",
                Map.of("nodes", nodes.size(), "population", evolutionStatus.populationSize()));
    }

    public synchronized void markRunning() {
        phase = ScenarioPhase.RUNNING;
        startedAt = Instant.now();
        logEvent("START", "Simulation run started", Map.of("targetTicks", targetTicks));
    }

    public synchronized void recordTick(int tick) {
        currentTick = tick;
    }

    public synchronized void updateNodes(List<GridNodeView> nodeViews) {
        nodeViews.forEach(view -> nodes.put(view.nodeId(), view));
    }

    public synchronized void recordGeneration(GenerationReport report, List<GridAgentProfile> profiles) {
        if (report != null) {
            latestGeneration = report;
        }
        if (profiles != null && !profiles.isEmpty()) {
            agents = new ArrayList<>(profiles);
        }
    }

    public synchronized void updateEvolutionStatus(EvolutionStatus status) {
        if (status != null) {
            evolutionStatus = status;
        }
    }

    public synchronized void requestFailure() {
        failureRequested = true;
        if (phase == ScenarioPhase.RUNNING) {
            phase = ScenarioPhase.WAITING_FOR_FAILURE;
        }
        logEvent("FAILURE_REQUESTED", "Operator requested fault injection",
                Map.of("failureTick", failureTick, "preferredNode", preferredFailureServiceId));
    }

    public synchronized boolean shouldInjectFailure(int tick) {
        return failureRequested && !failureInjected && tick >= failureTick;
    }

    public synchronized void recordFailure(String scenarioNodeId,
                                           FaultInjector.InfrastructureFaultImpact impact) {
        failureInjected = true;
        failingNodeId = scenarioNodeId;
        lastFaultImpact = impact;
        phase = ScenarioPhase.FAILURE_INJECTED;
        logEvent("FAILURE_INJECTED", impact.message(),
                Map.of("node", scenarioNodeId, "recovered", impact.recovered()));
    }

    public synchronized void recordRecovery(boolean recovered) {
        phase = recovered ? ScenarioPhase.RUNNING : ScenarioPhase.RECOVERING;
        logEvent("RECOVERY", recovered ? "Project A restored the grid segment" : "Recovery still in progress",
                Map.of("recovered", recovered));
    }

    public synchronized void markCompleted() {
        phase = ScenarioPhase.COMPLETED;
        completedAt = Instant.now();
        logEvent("COMPLETE", "Self-Healing Grid run completed",
                Map.of("ticksExecuted", currentTick));
    }

    public synchronized void markFailed(String reason) {
        phase = ScenarioPhase.FAILED;
        completedAt = Instant.now();
        logEvent("FAILED", reason == null ? "Scenario failed" : reason, Map.of());
    }

    public synchronized GridScenarioStatus snapshotStatus() {
        List<ScenarioEvent> recent = events.stream()
                .skip(Math.max(0, events.size() - 5))
                .collect(Collectors.toList());
        return new GridScenarioStatus(
                scenarioId,
                phase,
                currentTick,
                targetTicks,
                failureTick,
                failureRequested,
                failureInjected,
                failingNodeId,
                setupAt,
                startedAt,
                completedAt,
                evolutionStatus,
                latestGeneration,
                lastFaultImpact,
                List.copyOf(nodes.values()),
                List.copyOf(agents),
                recent
        );
    }

    public synchronized GridScenarioReport snapshotReport() {
        return new GridScenarioReport(
                scenarioId,
                phase,
                targetTicks,
                currentTick,
                failureInjected,
                failingNodeId,
                startedAt,
                completedAt,
                evolutionStatus,
                latestGeneration,
                lastFaultImpact,
                List.copyOf(nodes.values()),
                List.copyOf(agents),
                List.copyOf(events)
        );
    }

    public synchronized ScenarioEvent logEvent(String type, String message, Map<String, Object> details) {
        ScenarioEvent event = new ScenarioEvent(
                type,
                message,
                Instant.now(),
                details == null ? Map.of() : Map.copyOf(details)
        );
        events.addLast(event);
        while (events.size() > EVENT_LIMIT) {
            events.removeFirst();
        }
        return event;
    }

    public synchronized boolean isReady() {
        return phase == ScenarioPhase.READY;
    }

    public synchronized boolean hasActiveRun() {
        return phase == ScenarioPhase.RUNNING || phase == ScenarioPhase.WAITING_FOR_FAILURE
                || phase == ScenarioPhase.FAILURE_INJECTED || phase == ScenarioPhase.RECOVERING;
    }

    public int targetTicks() {
        return targetTicks;
    }

    public int failureTick() {
        return failureTick;
    }

    public String preferredFailureServiceId() {
        return preferredFailureServiceId;
    }

    public synchronized int currentTick() {
        return currentTick;
    }

    public synchronized String failingNodeId() {
        return failingNodeId;
    }

    public synchronized UUID evolutionRunId() {
        return evolutionRunId;
    }
}


