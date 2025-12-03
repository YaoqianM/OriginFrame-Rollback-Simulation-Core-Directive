package prototype.visualization.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.domain.Position;
import prototype.simulationcore.dto.AgentDto;
import prototype.simulationcore.repository.AgentRepository;
import prototype.visualization.model.AgentTrail;
import prototype.visualization.model.LineageTree;
import prototype.visualization.model.LineageTreeNode;
import prototype.visualization.model.RenderedGraph;
import prototype.visualization.model.TimelinePoint;
import prototype.visualization.model.TrailPoint;
import prototype.visualization.model.WorldMetrics;
import prototype.visualization.model.WorldSnapshot;

/**
 * Aggregates simulation data into visualization-friendly structures.
 */
@Service
public class VisualizationService {

    private final AgentRepository agentRepository;
    private final EventConsumer eventConsumer;
    private final GraphRenderer graphRenderer;

    public VisualizationService(AgentRepository agentRepository,
                                EventConsumer eventConsumer,
                                GraphRenderer graphRenderer) {
        this.agentRepository = agentRepository;
        this.eventConsumer = eventConsumer;
        this.graphRenderer = graphRenderer;
    }

    public WorldSnapshot getWorldSnapshot(String simulationId, Long requestedTick) {
        List<Agent> agents = agentRepository.findAll();
        List<AgentDto> agentDtos = agents.stream()
                .map(AgentDto::from)
                .toList();
        RenderedGraph agentNetwork = graphRenderer.renderAgentNetwork(agents);
        WorldMetrics metrics = WorldMetrics.fromAgents(agents);
        long historySize = eventConsumer.getHistory().size();
        long resolvedTick = Optional.ofNullable(requestedTick).orElse(historySize);
        return new WorldSnapshot(
                simulationId,
                resolvedTick,
                Instant.now(),
                agentDtos,
                metrics,
                agentNetwork
        );
    }

    public WorldSnapshot getLiveState(String simulationId) {
        return getWorldSnapshot(simulationId, null);
    }

    public List<AgentDto> getAgents(String simulationId) {
        return agentRepository.findAll().stream()
                .map(AgentDto::from)
                .toList();
    }

    public AgentTrail getAgentTrail(String simulationId, UUID agentId) {
        List<TrailPoint> waypoints = eventConsumer.getHistory().stream()
                .filter(event -> agentId.toString().equals(event.getAgentId()))
                .sorted(Comparator.comparing(LineageEvent::getTimestamp))
                .map(event -> new TrailPoint(
                        event.getTimestamp(),
                        event.getResultingState().position(),
                        event.getResultingState().energy(),
                        event.getResultingState().resources()
                ))
                .collect(Collectors.toCollection(ArrayList::new));

        if (waypoints.isEmpty()) {
            agentRepository.findById(agentId).ifPresent(agent -> {
                AgentState state = agent.getState();
                Position position = state == null ? Position.origin() : state.position();
                double energy = state == null ? 0.0 : state.energy();
                double resources = state == null ? 0.0 : state.resources();
                Instant created = agent.getCreatedAt() == null ? Instant.now() : agent.getCreatedAt();
                waypoints.add(new TrailPoint(created, position, energy, resources));
            });
        }

        return new AgentTrail(simulationId, agentId, waypoints);
    }

    public LineageTree getLineageTree(String simulationId, UUID agentId) {
        List<Agent> agents = agentRepository.findAll();
        Map<UUID, Agent> agentIndex = agents.stream()
                .filter(agent -> agent.getAgentId() != null)
                .collect(Collectors.toMap(Agent::getAgentId, agent -> agent));

        Agent target = agentIndex.get(agentId);
        if (target == null) {
            throw new NoSuchElementException("Agent not found: " + agentId);
        }

        Map<UUID, List<Agent>> byParent = agents.stream()
                .filter(agent -> agent.getParentId() != null && agent.getAgentId() != null)
                .collect(Collectors.groupingBy(Agent::getParentId));

        Agent root = target;
        while (root.getParentId() != null && agentIndex.containsKey(root.getParentId())) {
            root = agentIndex.get(root.getParentId());
        }

        LineageTreeNode rootNode = buildTree(root, byParent);
        return new LineageTree(simulationId, root.getAgentId(), rootNode);
    }

    public List<TimelinePoint> getTimeline(String simulationId) {
        List<LineageEvent> history = eventConsumer.getHistory();
        long[] counter = {0};
        return history.stream()
                .sorted(Comparator.comparing(LineageEvent::getTimestamp))
                .map(event -> new TimelinePoint(
                        simulationId,
                        ++counter[0],
                        event.getTimestamp(),
                        event.getAgentId(),
                        event.getResultingState().energy(),
                        event.getResultingState().resources()
                ))
                .toList();
    }

    private LineageTreeNode buildTree(Agent agent, Map<UUID, List<Agent>> byParent) {
        List<LineageTreeNode> children = byParent.getOrDefault(agent.getAgentId(), List.of()).stream()
                .map(child -> buildTree(child, byParent))
                .toList();
        return LineageTreeNode.from(agent, children);
    }
}


