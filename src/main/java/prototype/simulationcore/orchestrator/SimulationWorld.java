package prototype.simulationcore.orchestrator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import prototype.simulationcore.events.SimulationEvent;

public class SimulationWorld {

    private static final int MAX_EVENT_HISTORY = 250;

    private final UUID simulationId = UUID.randomUUID();
    private final SimulationConfig config;
    private final EventScheduler scheduler;
    private final AtomicLong currentTick = new AtomicLong();
    private final AtomicInteger constraintViolations = new AtomicInteger();
    private volatile SimulationWorldStatus status = SimulationWorldStatus.CREATED;
    private final Instant createdAt = Instant.now();
    private volatile Instant updatedAt = createdAt;
    private volatile ScenarioDefinition scenarioDefinition;
    private final ConcurrentLinkedDeque<SimulationEvent> eventHistory = new ConcurrentLinkedDeque<>();
    private final Map<String, Object> environmentState = new ConcurrentHashMap<>();
    private final Map<String, Object> runtimeContext = new ConcurrentHashMap<>();

    public SimulationWorld(SimulationConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.scheduler = new EventScheduler();
        runtimeContext.put("name", config.getName());
    }

    public UUID getSimulationId() {
        return simulationId;
    }

    public SimulationConfig getConfig() {
        return config;
    }

    public long getCurrentTick() {
        return currentTick.get();
    }

    public long incrementTick() {
        long tick = currentTick.incrementAndGet();
        updatedAt = Instant.now();
        return tick;
    }

    public SimulationWorldStatus getStatus() {
        return status;
    }

    public void setStatus(SimulationWorldStatus status) {
        this.status = status;
        updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void attachScenario(ScenarioDefinition scenarioDefinition) {
        this.scenarioDefinition = scenarioDefinition;
    }

    public Optional<ScenarioDefinition> getScenarioDefinition() {
        return Optional.ofNullable(scenarioDefinition);
    }

    public EventScheduler getScheduler() {
        return scheduler;
    }

    public void recordEvent(SimulationEvent event) {
        eventHistory.addLast(event);
        while (eventHistory.size() > MAX_EVENT_HISTORY) {
            eventHistory.pollFirst();
        }
        updatedAt = Instant.now();
    }

    public List<SimulationEvent> getRecentEvents() {
        return new ArrayList<>(eventHistory);
    }

    public Map<String, Object> snapshotEnvironment() {
        return Map.copyOf(environmentState);
    }

    public Map<String, Object> mutateEnvironment(long tick) {
        environmentState.put("lastTick", tick);
        environmentState.merge("load", 0.1, (prev, delta) ->
                prev instanceof Number ? ((Number) prev).doubleValue() + (Double) delta : delta);
        environmentState.merge("temperature", 0.05, (prev, delta) ->
                prev instanceof Number ? ((Number) prev).doubleValue() + (Double) delta : delta);
        return snapshotEnvironment();
    }

    public void applyInitialEnvironment(Map<String, Object> initialState) {
        environmentState.clear();
        environmentState.putAll(initialState);
    }

    public void putRuntimeContext(String key, Object value) {
        runtimeContext.put(key, value);
    }

    public Map<String, Object> getRuntimeContext() {
        return Map.copyOf(runtimeContext);
    }

    public void recordConstraintViolation() {
        constraintViolations.incrementAndGet();
    }

    public int getConstraintViolationCount() {
        return constraintViolations.get();
    }
}

