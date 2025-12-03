package prototype.visualization.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.recovery.ServiceSnapshot;
import prototype.lineageruntime.recovery.ServiceTopology;
import prototype.simulationcore.domain.Agent;
import prototype.visualization.model.GraphEdge;
import prototype.visualization.model.GraphNode;
import prototype.visualization.model.LineageTree;
import prototype.visualization.model.LineageTreeNode;
import prototype.visualization.model.RenderedGraph;

/**
 * Produces node-edge graphs for different visualization aspects.
 */
@Service
public class GraphRenderer {

    public RenderedGraph renderTopology(ServiceTopology topology) {
        if (topology == null) {
            return new RenderedGraph(List.of(), List.of());
        }

        Set<String> serviceIds = topology.serviceIds();
        List<GraphNode> nodes = new ArrayList<>(serviceIds.size());
        List<GraphEdge> edges = new ArrayList<>();

        for (String serviceId : serviceIds) {
            ServiceSnapshot snapshot = topology.snapshot(serviceId);
            String version = snapshot.version() == null ? "unknown" : snapshot.version();
            String instanceId = snapshot.instanceId() == null ? "n/a" : snapshot.instanceId();
            nodes.add(new GraphNode(
                    serviceId,
                    serviceId,
                    Map.of(
                            "status", snapshot.status().name(),
                            "version", version,
                            "fallbackActive", snapshot.fallbackActive(),
                            "instanceId", instanceId
                    )
            ));

            topology.dependenciesOf(serviceId)
                    .forEach(dependency -> edges.add(new GraphEdge(
                            serviceId,
                            dependency,
                            "depends_on",
                            Map.of("type", "dependency")
                    )));

            topology.fallbackOf(serviceId)
                    .ifPresent(fallback -> edges.add(new GraphEdge(
                            serviceId,
                            fallback,
                            "fallback",
                            Map.of("type", "fallback")
                    )));
        }

        return new RenderedGraph(nodes, edges);
    }

    public RenderedGraph renderAgentNetwork(List<Agent> agents) {
        if (agents == null || agents.isEmpty()) {
            return new RenderedGraph(List.of(), List.of());
        }

        List<GraphNode> nodes = agents.stream()
                .filter(agent -> agent.getAgentId() != null)
                .map(agent -> new GraphNode(
                        agent.getAgentId().toString(),
                        "Gen " + agent.getGeneration(),
                        Map.of(
                                "fitness", agent.getFitness(),
                                "safetyViolations", agent.getSafetyViolations(),
                                "energy", agent.getState() == null ? 0.0 : agent.getState().energy(),
                                "resources", agent.getState() == null ? 0.0 : agent.getState().resources()
                        )
                ))
                .toList();

        List<GraphEdge> edges = agents.stream()
                .filter(agent -> agent.getAgentId() != null && agent.getParentId() != null)
                .map(agent -> new GraphEdge(
                        agent.getParentId().toString(),
                        agent.getAgentId().toString(),
                        "parent",
                        Map.of("type", "lineage")
                ))
                .toList();

        return new RenderedGraph(nodes, edges);
    }

    public RenderedGraph renderLineageGraph(LineageTree tree) {
        if (tree == null || tree.root() == null) {
            return new RenderedGraph(List.of(), List.of());
        }

        List<GraphNode> nodes = new ArrayList<>();
        List<GraphEdge> edges = new ArrayList<>();
        traverseLineage(tree.root(), null, nodes, edges);
        return new RenderedGraph(nodes, edges);
    }

    private void traverseLineage(LineageTreeNode node,
                                 LineageTreeNode parent,
                                 List<GraphNode> nodes,
                                 List<GraphEdge> edges) {
        Map<String, Object> metadata = Map.of(
                "generation", node.generation(),
                "fitness", node.fitness()
        );
        String nodeId = node.agentId().toString();
        String label = "Agent " + nodeId.substring(0, Math.min(8, nodeId.length()));
        nodes.add(new GraphNode(nodeId, label, metadata));

        if (parent != null) {
            edges.add(new GraphEdge(
                    parent.agentId().toString(),
                    nodeId,
                    "lineage",
                    Map.of("type", "lineage")
            ));
        }

        node.children().forEach(child -> traverseLineage(child, node, nodes, edges));
    }
}


