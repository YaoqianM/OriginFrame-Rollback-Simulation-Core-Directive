package prototype.lineageruntime.lineage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prototype.lineageruntime.lineage.domain.AgentLineage;
import prototype.lineageruntime.lineage.domain.MutationEvent;
import prototype.lineageruntime.lineage.dto.LineageExportPayload;
import prototype.lineageruntime.lineage.dto.LineageGraphView;
import prototype.lineageruntime.lineage.model.LineageMetrics;
import prototype.lineageruntime.lineage.repository.AgentLineageRepository;
import prototype.simulationcore.domain.Agent;

@Service
public class LineageTrackerService {

    private static final Logger log = LoggerFactory.getLogger(LineageTrackerService.class);

    private final AgentLineageRepository repository;
    private final LineageGraphService graphService;
    private final ObjectMapper objectMapper;

    public LineageTrackerService(AgentLineageRepository repository,
                                 LineageGraphService graphService,
                                 ObjectMapper objectMapper) {
        this.repository = repository;
        this.graphService = graphService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentLineage recordBirth(Agent agent, Agent parent, List<MutationEvent> mutations) {
        return repository.findById(agent.getAgentId())
                .orElseGet(() -> createLineage(agent, parent, mutations));
    }

    @Transactional
    public AgentLineage recordPerformanceUpdate(Agent agent, LineageMetrics metrics) {
        AgentLineage lineage = repository.findById(agent.getAgentId())
                .orElseGet(() -> createLineage(agent, null, List.of()));

        lineage.setGeneration(agent.getGeneration());
        lineage.setPerformanceScore(metrics.performanceScore());
        lineage.setSafetyScore(metrics.safetyScore());
        lineage.setSurvivedGenerations(Math.max(lineage.getSurvivedGenerations(), metrics.survivedGenerations()));
        lineage.mergeMetadata(metrics.metadata());

        AgentLineage saved = repository.save(lineage);
        log.debug("Updated lineage metrics for agent {}", agent.getAgentId());
        return saved;
    }

    @Transactional
    public AgentLineage recordDeath(Agent agent, String reason) {
        AgentLineage lineage = repository.findById(agent.getAgentId())
                .orElseThrow(() -> new IllegalStateException("Lineage missing for agent " + agent.getAgentId()));
        lineage.setEliminationReason(reason);
        lineage.setSurvivedGenerations(Math.max(lineage.getSurvivedGenerations(), agent.getGeneration()));
        lineage.mergeMetadata(Map.of(
                "deathRecordedAt", Instant.now().toString(),
                "eliminationReason", reason
        ));
        return repository.save(lineage);
    }

    @Transactional
    public AgentLineage recordViolation(Agent agent, String violation) {
        AgentLineage lineage = repository.findById(agent.getAgentId())
                .orElseThrow(() -> new IllegalStateException("Lineage missing for agent " + agent.getAgentId()));
        lineage.setEliminationReason(violation);
        lineage.setSafetyScore(Math.max(0.0, lineage.getSafetyScore() - 10.0));
        lineage.mergeMetadata(Map.of(
                "violation", violation,
                "violationRecordedAt", Instant.now().toString()
        ));
        return repository.save(lineage);
    }

    @Transactional
    public LineageExportPayload exportLineageTree(UUID rootAgentId) {
        LineageGraphView graph = graphService.buildGraph(rootAgentId);
        try {
            String graphJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
            String graphml = toGraphMl(graph);
            return new LineageExportPayload(rootAgentId, graph, graphJson, graphml);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to export lineage graph", e);
        }
    }

    private AgentLineage createLineage(Agent agent, Agent parent, List<MutationEvent> mutations) {
        UUID parentAgentId = parent != null ? parent.getAgentId() : agent.getParentId();
        UUID lineageId = resolveLineageId(parentAgentId).orElse(agent.getAgentId());
        AgentLineage lineage = AgentLineage.create(lineageId, agent.getAgentId(), parentAgentId, agent.getGeneration());
        lineage.setPerformanceScore(agent.getFitness());
        lineage.setSafetyScore(Math.max(0.0, 100.0 - (agent.getSafetyViolations() * 5.0)));
        lineage.setSurvivedGenerations(Math.max(1, agent.getGeneration()));
        lineage.setMetadata(defaultMetadata(agent));
        lineage.setMutationsApplied(new ArrayList<>(mutations == null ? List.of() : mutations));
        AgentLineage saved = repository.save(lineage);
        log.debug("Recorded lineage birth for agent {} (lineage {})", agent.getAgentId(), lineageId);
        return saved;
    }

    private Optional<UUID> resolveLineageId(UUID parentAgentId) {
        if (parentAgentId == null) {
            return Optional.empty();
        }
        return repository.findById(parentAgentId).map(AgentLineage::getLineageId);
    }

    private Map<String, Object> defaultMetadata(Agent agent) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("createdAt", Instant.now().toString());
        metadata.put("generation", agent.getGeneration());
        metadata.put("policy", agent.getPolicy() == null ? "unknown" : agent.getPolicy().getClass().getSimpleName());
        metadata.put("fitness", agent.getFitness());
        metadata.put("safetyViolations", agent.getSafetyViolations());
        return metadata;
    }

    private String toGraphMl(LineageGraphView graph) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8"?>
                <graphml xmlns="http://graphml.graphdrawing.org/xmlns">
                  <graph id="lineage" edgedefault="directed">
                """);
        graph.nodes().forEach(node -> builder.append("""
                  <node id="%s">
                    <data key="generation">%d</data>
                    <data key="performance">%.4f</data>
                    <data key="safety">%.4f</data>
                  </node>
                """.formatted(node.agentId(), node.generation(), node.performanceScore(), node.safetyScore())));
        graph.edges().forEach(edge -> builder.append("""
                  <edge source="%s" target="%s"/>
                """.formatted(edge.parentAgentId(), edge.childAgentId())));
        builder.append("""
                  </graph>
                </graphml>
                """);
        return builder.toString();
    }
}

