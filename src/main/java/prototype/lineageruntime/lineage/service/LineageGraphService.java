package prototype.lineageruntime.lineage.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prototype.lineageruntime.lineage.domain.AgentLineage;
import prototype.lineageruntime.lineage.dto.LineageEdgeView;
import prototype.lineageruntime.lineage.dto.LineageGenerationStats;
import prototype.lineageruntime.lineage.dto.LineageGraphView;
import prototype.lineageruntime.lineage.dto.LineageNodeView;
import prototype.lineageruntime.lineage.exception.LineageNotFoundException;
import prototype.lineageruntime.lineage.repository.AgentLineageRepository;

@Service
public class LineageGraphService {

    private final AgentLineageRepository repository;

    public LineageGraphService(AgentLineageRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public LineageGraphView buildGraph(UUID rootAgentId) {
        AgentLineage root = repository.findById(rootAgentId)
                .orElseThrow(() -> new LineageNotFoundException(rootAgentId));

        Map<UUID, LineageNodeView> nodes = new LinkedHashMap<>();
        List<LineageEdgeView> edges = new ArrayList<>();
        Deque<AgentLineage> queue = new ArrayDeque<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            AgentLineage current = queue.poll();
            UUID currentAgentId = current.getAgentId();
            nodes.putIfAbsent(currentAgentId, LineageNodeView.from(current));

            List<AgentLineage> children = repository.findByParentId(currentAgentId);
            for (AgentLineage child : children) {
                edges.add(new LineageEdgeView(currentAgentId, child.getAgentId()));
                queue.add(child);
            }
        }

        Map<Integer, LineageGenerationStats> stats = computeGenerationStats(nodes);
        return new LineageGraphView(rootAgentId, List.copyOf(nodes.values()), List.copyOf(edges), stats);
    }

    @Transactional(readOnly = true)
    public LineageNodeView getLineage(UUID agentId) {
        AgentLineage lineage = repository.findById(agentId)
                .orElseThrow(() -> new LineageNotFoundException(agentId));
        return LineageNodeView.from(lineage);
    }

    @Transactional(readOnly = true)
    public List<LineageNodeView> findAncestors(UUID agentId, int depth) {
        List<LineageNodeView> ancestors = new ArrayList<>();
        UUID currentId = agentId;
        int traversed = 0;
        while (currentId != null && (depth <= 0 || traversed < depth)) {
            UUID lookupCurrentId = currentId;
            AgentLineage current = repository.findById(lookupCurrentId)
                    .orElseThrow(() -> new LineageNotFoundException(agentId));
            UUID parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            UUID lookupParentId = parentId;
            AgentLineage parent = repository.findById(lookupParentId)
                    .orElseThrow(() -> new LineageNotFoundException(lookupParentId));
            ancestors.add(LineageNodeView.from(parent));
            currentId = parent.getAgentId();
            traversed++;
        }
        return ancestors;
    }

    @Transactional(readOnly = true)
    public List<LineageNodeView> findDescendants(UUID agentId, int depth) {
        AgentLineage root = repository.findById(agentId)
                .orElseThrow(() -> new LineageNotFoundException(agentId));
        List<LineageNodeView> descendants = new ArrayList<>();
        Deque<TraversalNode> queue = new ArrayDeque<>();
        queue.add(new TraversalNode(root, 0));

        while (!queue.isEmpty()) {
            TraversalNode cursor = queue.poll();
            if (cursor.depth() > 0) {
                descendants.add(LineageNodeView.from(cursor.lineage()));
            }
            if (depth > 0 && cursor.depth() >= depth) {
                continue;
            }
            List<AgentLineage> children = repository.findByParentId(cursor.lineage().getAgentId());
            for (AgentLineage child : children) {
                queue.add(new TraversalNode(child, cursor.depth() + 1));
            }
        }
        return descendants;
    }

    @Transactional(readOnly = true)
    public Optional<LineageNodeView> findCommonAncestor(UUID firstAgentId, UUID secondAgentId) {
        List<UUID> firstAncestors = collectAncestorIds(firstAgentId);
        UUID cursor = secondAgentId;

        while (cursor != null) {
            UUID lookupId = cursor;
            AgentLineage lineage = repository.findById(lookupId)
                    .orElseThrow(() -> new LineageNotFoundException(lookupId));
            if (firstAncestors.contains(lineage.getAgentId())) {
                return Optional.of(LineageNodeView.from(lineage));
            }
            cursor = lineage.getParentId();
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public LineageGenerationStats getGenerationStats(int generation) {
        List<AgentLineage> lineages = repository.findByGeneration(generation);
        if (lineages.isEmpty()) {
            throw new LineageNotFoundException("No lineages for generation %d".formatted(generation));
        }
        double avgPerformance = lineages.stream()
                .mapToDouble(AgentLineage::getPerformanceScore)
                .average()
                .orElse(0.0);
        double avgSafety = lineages.stream()
                .mapToDouble(AgentLineage::getSafetyScore)
                .average()
                .orElse(0.0);
        long survivors = lineages.stream().filter(lineage -> !lineage.isEliminated()).count();
        long eliminated = lineages.size() - survivors;
        return new LineageGenerationStats(generation, lineages.size(), avgPerformance, avgSafety, survivors, eliminated);
    }

    private Map<Integer, LineageGenerationStats> computeGenerationStats(Map<UUID, LineageNodeView> nodes) {
        Map<Integer, List<LineageNodeView>> grouped = nodes.values().stream()
                .collect(Collectors.groupingBy(LineageNodeView::generation, TreeMap::new, Collectors.toList()));

        Map<Integer, LineageGenerationStats> stats = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<LineageNodeView>> entry : grouped.entrySet()) {
            int generation = entry.getKey();
            List<LineageNodeView> generationNodes = entry.getValue();
            double avgPerformance = generationNodes.stream()
                    .mapToDouble(LineageNodeView::performanceScore)
                    .average()
                    .orElse(0.0);
            double avgSafety = generationNodes.stream()
                    .mapToDouble(LineageNodeView::safetyScore)
                    .average()
                    .orElse(0.0);
            long survivors = generationNodes.stream()
                    .filter(node -> node.eliminationReason() == null || node.eliminationReason().isBlank())
                    .count();
            long eliminated = generationNodes.size() - survivors;
            stats.put(generation, new LineageGenerationStats(
                    generation,
                    generationNodes.size(),
                    avgPerformance,
                    avgSafety,
                    survivors,
                    eliminated
            ));
        }
        return stats;
    }

    private List<UUID> collectAncestorIds(UUID agentId) {
        List<UUID> ancestors = new ArrayList<>();
        UUID cursor = agentId;
        while (cursor != null) {
            UUID lookupId = cursor;
            AgentLineage lineage = repository.findById(lookupId)
                    .orElseThrow(() -> new LineageNotFoundException(lookupId));
            cursor = lineage.getParentId();
            if (cursor != null) {
                ancestors.add(cursor);
            }
        }
        return ancestors;
    }

    private record TraversalNode(AgentLineage lineage, int depth) {
    }
}

