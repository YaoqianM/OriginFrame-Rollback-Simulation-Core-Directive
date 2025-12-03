package prototype.simulationcore.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;

/**
 * Aggregates per-tick agent and system metrics.
 */
@Component
public class MetricsCollector {

    private final SimulationRunRegistry registry;

    public MetricsCollector(SimulationRunRegistry registry) {
        this.registry = registry;
    }

    public List<AgentMetric> collectAgentMetrics(SimulationWorldSnapshot world) {
        return collectAgentMetrics(world, null);
    }

    private List<AgentMetric> collectAgentMetrics(SimulationWorldSnapshot world, SimulationRunState state) {
        List<AgentMetric> metrics = new ArrayList<>();
        Map<UUID, Action> actions = world.actionsPerformed();
        Map<UUID, Double> rewards = world.rewards();
        Map<UUID, Integer> violations = world.violationDeltas();

        for (Agent agent : world.agents()) {
            if (agent == null || agent.getAgentId() == null) {
                continue;
            }
            AgentState agentState = agent.getState();
            AgentMetric.Builder builder = AgentMetric.builder(world.simulationId(), agent.getAgentId())
                    .tick(world.tick())
                    .position(agentState == null ? null : agentState.position())
                    .energy(agentState == null ? 0.0 : agentState.energy())
                    .resources(agentState == null ? 0.0 : agentState.resources())
                    .rewardThisTick(resolveReward(agent, rewards, state))
                    .violationsThisTick(resolveViolations(agent, violations, state))
                    .recordedAt(world.capturedAt());

            Action action = actions.get(agent.getAgentId());
            if (action != null) {
                builder.action(action.name());
            }
            metrics.add(builder.build());
        }
        return metrics;
    }

    public SystemMetrics collectSystemMetrics(SimulationWorldSnapshot world) {
        List<Agent> agents = world.agents();
        int totalAgents = agents.size();
        int activeAgents = (int) agents.stream()
                .filter(agent -> agent != null && agent.getState() != null && agent.getState().energy() > 0.0)
                .count();
        double averageFitness = agents.stream()
                .filter(agent -> agent != null)
                .mapToDouble(Agent::getFitness)
                .average()
                .orElse(0.0);
        int totalViolations = agents.stream()
                .filter(agent -> agent != null)
                .mapToInt(Agent::getSafetyViolations)
                .sum();

        return SystemMetrics.builder(world.simulationId())
                .tick(world.tick())
                .activeAgents(activeAgents)
                .totalAgents(totalAgents)
                .healthyNodes(Math.max(world.healthyNodes(), totalAgents - world.failedNodes()))
                .failedNodes(world.failedNodes())
                .averageAgentFitness(averageFitness)
                .totalViolations(totalViolations)
                .networkLatencyAvg(world.networkLatencyAvg())
                .recordedAt(world.capturedAt())
                .build();
    }

    public void record(SimulationWorldSnapshot world) {
        SimulationRunState state = registry.stateFor(world.simulationId());
        List<AgentMetric> agentMetrics = collectAgentMetrics(world, state);
        SystemMetrics systemMetrics = collectSystemMetrics(world);
        state.appendMetrics(agentMetrics, systemMetrics);
    }

    public int nextTick(String simulationId) {
        return registry.stateFor(simulationId).nextTick();
    }

    public List<AgentMetric> getAgentMetrics(String simulationId, int fromTick, int toTick) {
        return registry.findState(simulationId)
                .map(state -> state.agentMetricsBetween(normalizeFrom(fromTick), normalizeTo(toTick)))
                .orElse(List.of());
    }

    public List<SystemMetrics> getSystemMetrics(String simulationId, int fromTick, int toTick) {
        return registry.findState(simulationId)
                .map(state -> state.systemMetricsBetween(normalizeFrom(fromTick), normalizeTo(toTick)))
                .orElse(List.of());
    }

    public int latestTick(String simulationId) {
        return registry.findState(simulationId)
                .map(SimulationRunState::latestTick)
                .orElse(0);
    }

    public boolean hasSimulation(String simulationId) {
        return registry.hasState(simulationId);
    }

    private int normalizeFrom(int fromTick) {
        return Math.max(fromTick, 0);
    }

    private int normalizeTo(int toTick) {
        return toTick <= 0 ? Integer.MAX_VALUE : toTick;
    }

    private double resolveReward(Agent agent,
                                 Map<UUID, Double> explicitRewards,
                                 SimulationRunState state) {
        UUID agentId = agent.getAgentId();
        if (explicitRewards.containsKey(agentId)) {
            return explicitRewards.get(agentId);
        }
        if (state == null) {
            return 0.0;
        }
        return state.rewardDelta(agentId, agent.getFitness());
    }

    private int resolveViolations(Agent agent,
                                  Map<UUID, Integer> explicit,
                                  SimulationRunState state) {
        UUID agentId = agent.getAgentId();
        if (explicit.containsKey(agentId)) {
            return explicit.get(agentId);
        }
        if (state == null) {
            return 0;
        }
        return state.violationDelta(agentId, agent.getSafetyViolations());
    }
}

