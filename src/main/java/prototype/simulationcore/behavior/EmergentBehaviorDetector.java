package prototype.simulationcore.behavior;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.metrics.AgentMetric;
import prototype.simulationcore.metrics.SystemMetrics;

@Component
public class EmergentBehaviorDetector {

    private static final double CLUSTERING_DISTANCE_THRESHOLD = 2.5;
    private static final double COMPETITION_RESOURCE_DELTA = 10.0;
    private static final double COOPERATION_REWARD_THRESHOLD = 1.0;

    public EmergentSignal detectClustering(List<AgentMetric> metrics) {
        List<AgentMetric> latest = latestTickMetrics(metrics);
        if (latest.size() < 2) {
            return EmergentSignal.notDetected("CLUSTERING", "Not enough agents for clustering.");
        }
        double averageDistance = averagePairwiseDistance(latest);
        boolean clustered = averageDistance <= CLUSTERING_DISTANCE_THRESHOLD;
        Map<String, Object> metadata = Map.of(
                "averageDistance", averageDistance,
                "agentCount", latest.size(),
                "tick", latest.get(0).tick()
        );
        if (clustered) {
            return new EmergentSignal("CLUSTERING", true,
                    "Agents formed a tight cluster.", Instant.now(), metadata);
        }
        return new EmergentSignal("CLUSTERING", false,
                "Agents remain dispersed.", Instant.now(), metadata);
    }

    public EmergentSignal detectCompetition(List<AgentMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return EmergentSignal.notDetected("COMPETITION", "No metrics available.");
        }

        Map<Integer, Double> avgResourcesByTick = metrics.stream()
                .collect(Collectors.groupingBy(
                        AgentMetric::tick,
                        Collectors.averagingDouble(AgentMetric::resources)
                ));

        OptionalInt minTick = avgResourcesByTick.keySet().stream().mapToInt(Integer::intValue).min();
        OptionalInt maxTick = avgResourcesByTick.keySet().stream().mapToInt(Integer::intValue).max();
        if (minTick.isEmpty() || maxTick.isEmpty()) {
            return EmergentSignal.notDetected("COMPETITION", "Insufficient data window.");
        }

        double startingResources = avgResourcesByTick.get(minTick.getAsInt());
        double endingResources = avgResourcesByTick.get(maxTick.getAsInt());
        double delta = startingResources - endingResources;
        double totalViolations = metrics.stream().mapToInt(AgentMetric::violationsThisTick).sum();

        boolean competition = delta > COMPETITION_RESOURCE_DELTA || totalViolations > 0;
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resourcesDelta", delta);
        metadata.put("totalViolations", totalViolations);
        metadata.put("tickWindow", minTick.getAsInt() + "-" + maxTick.getAsInt());

        String summary = competition
                ? "Resource drawdown or violations suggest competition."
                : "Resource levels stable; competition unlikely.";
        return new EmergentSignal("COMPETITION", competition, summary, Instant.now(), metadata);
    }

    public EmergentSignal detectCooperation(List<AgentMetric> metrics) {
        List<AgentMetric> latest = latestTickMetrics(metrics);
        if (latest.isEmpty()) {
            return EmergentSignal.notDetected("COOPERATION", "No recent metrics.");
        }
        double avgReward = latest.stream()
                .mapToDouble(AgentMetric::rewardThisTick)
                .average()
                .orElse(0.0);
        long supportiveAgents = latest.stream()
                .filter(metric -> metric.rewardThisTick() >= COOPERATION_REWARD_THRESHOLD)
                .count();
        boolean cooperation = avgReward >= COOPERATION_REWARD_THRESHOLD && supportiveAgents > 1;
        Map<String, Object> metadata = Map.of(
                "avgReward", avgReward,
                "supportiveAgents", supportiveAgents,
                "tick", latest.get(0).tick()
        );
        String summary = cooperation
                ? "Agents show positive-sum collaboration."
                : "No cooperation patterns detected.";
        return new EmergentSignal("COOPERATION", cooperation, summary, Instant.now(), metadata);
    }

    public List<EmergentSignal> flagUnusualPatterns(List<SystemMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }

        List<EmergentSignal> signals = new ArrayList<>();
        metrics.stream()
                .filter(metric -> metric.failedNodes() > 0)
                .findFirst()
                .ifPresent(metric -> signals.add(new EmergentSignal(
                        "SYSTEM_FAILURE",
                        true,
                        "One or more nodes failed.",
                        Instant.now(),
                        Map.of("failedNodes", metric.failedNodes(), "tick", metric.tick())
                )));

        metrics.stream()
                .filter(metric -> metric.networkLatencyAvg() > 50.0)
                .findFirst()
                .ifPresent(metric -> signals.add(new EmergentSignal(
                        "NETWORK_LATENCY_SPIKE",
                        true,
                        "Network latency exceeded 50ms.",
                        Instant.now(),
                        Map.of("latency", metric.networkLatencyAvg(), "tick", metric.tick())
                )));

        DoubleSummaryStatistics fitnessStats = metrics.stream()
                .mapToDouble(SystemMetrics::averageAgentFitness)
                .summaryStatistics();
        double fitnessRange = fitnessStats.getMax() - fitnessStats.getMin();
        if (fitnessRange > 5.0) {
            signals.add(new EmergentSignal(
                    "FITNESS_VOLATILITY",
                    true,
                    "Average fitness fluctuated significantly.",
                    Instant.now(),
                    Map.of("range", fitnessRange)
            ));
        }

        if (signals.isEmpty()) {
            signals.add(EmergentSignal.notDetected("SYSTEM_HEALTH", "No unusual system patterns detected."));
        }
        return signals;
    }

    private List<AgentMetric> latestTickMetrics(List<AgentMetric> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return List.of();
        }
        int latestTick = metrics.stream()
                .mapToInt(AgentMetric::tick)
                .max()
                .orElse(0);
        return metrics.stream()
                .filter(metric -> metric.tick() == latestTick)
                .sorted(Comparator.comparing(AgentMetric::agentId))
                .toList();
    }

    private double averagePairwiseDistance(List<AgentMetric> metrics) {
        if (metrics.size() < 2) {
            return 0.0;
        }
        double total = 0.0;
        int comparisons = 0;
        for (int i = 0; i < metrics.size(); i++) {
            Position first = metrics.get(i).position();
            for (int j = i + 1; j < metrics.size(); j++) {
                total += first.distanceTo(metrics.get(j).position());
                comparisons++;
            }
        }
        return comparisons == 0 ? 0.0 : total / comparisons;
    }
}

