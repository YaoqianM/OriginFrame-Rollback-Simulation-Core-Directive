package prototype.simulationcore.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import prototype.simulationcore.timeline.TimelineEvent;

public final class SimulationRunState {

    private final String simulationId;
    private final AtomicInteger tickCounter = new AtomicInteger(0);
    private final CopyOnWriteArrayList<AgentMetric> agentMetrics = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SystemMetrics> systemMetrics = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<TimelineEvent> timelineEvents = new CopyOnWriteArrayList<>();
    private final Map<UUID, Double> lastFitness = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> lastViolations = new ConcurrentHashMap<>();
    private final Instant createdAt = Instant.now();

    public SimulationRunState(String simulationId) {
        this.simulationId = simulationId;
    }

    public int nextTick() {
        return tickCounter.incrementAndGet();
    }

    public void appendMetrics(List<AgentMetric> agents, SystemMetrics system) {
        if (agents != null && !agents.isEmpty()) {
            agentMetrics.addAll(agents);
        }
        if (system != null) {
            systemMetrics.add(system);
        }
    }

    public void addTimelineEvent(TimelineEvent event) {
        if (event != null) {
            timelineEvents.add(event);
        }
    }

    public List<AgentMetric> agentMetricsBetween(int fromTickInclusive, int toTickInclusive) {
        return agentMetrics.stream()
                .filter(metric -> metric.tick() >= fromTickInclusive && metric.tick() <= toTickInclusive)
                .toList();
    }

    public List<SystemMetrics> systemMetricsBetween(int fromTickInclusive, int toTickInclusive) {
        return systemMetrics.stream()
                .filter(metric -> metric.tick() >= fromTickInclusive && metric.tick() <= toTickInclusive)
                .toList();
    }

    public List<TimelineEvent> timelineBetween(int fromTickInclusive, int toTickInclusive) {
        return timelineEvents.stream()
                .filter(event -> event.tick() >= fromTickInclusive && event.tick() <= toTickInclusive)
                .toList();
    }

    public int latestTick() {
        return tickCounter.get();
    }

    public Instant createdAt() {
        return createdAt;
    }

    double rewardDelta(UUID agentId, double currentFitness) {
        if (agentId == null) {
            return 0.0;
        }
        Double previous = lastFitness.put(agentId, currentFitness);
        if (previous == null) {
            return 0.0;
        }
        return currentFitness - previous;
    }

    int violationDelta(UUID agentId, int currentViolations) {
        if (agentId == null) {
            return 0;
        }
        Integer previous = lastViolations.put(agentId, currentViolations);
        if (previous == null) {
            return 0;
        }
        return currentViolations - previous;
    }
}

