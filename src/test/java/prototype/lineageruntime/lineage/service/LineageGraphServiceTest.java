package prototype.lineageruntime.lineage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prototype.lineageruntime.lineage.domain.AgentLineage;
import prototype.lineageruntime.lineage.dto.LineageGenerationStats;
import prototype.lineageruntime.lineage.dto.LineageGraphView;
import prototype.lineageruntime.lineage.dto.LineageNodeView;
import prototype.lineageruntime.lineage.exception.LineageNotFoundException;
import prototype.lineageruntime.lineage.repository.AgentLineageRepository;

@ExtendWith(MockitoExtension.class)
class LineageGraphServiceTest {

    @Mock
    private AgentLineageRepository repository;

    @InjectMocks
    private LineageGraphService service;

    private AgentLineage root;
    private AgentLineage child;
    private AgentLineage grandChild;

    @BeforeEach
    void setUpLineages() {
        UUID rootId = UUID.randomUUID();
        root = AgentLineage.create(rootId, rootId, null, 0);
        root.setPerformanceScore(10.0);
        root.setSafetyScore(9.0);

        UUID childId = UUID.randomUUID();
        child = AgentLineage.create(rootId, childId, root.getAgentId(), 1);
        child.setPerformanceScore(12.0);
        child.setSafetyScore(8.0);

        UUID grandChildId = UUID.randomUUID();
        grandChild = AgentLineage.create(rootId, grandChildId, child.getAgentId(), 2);
        grandChild.setPerformanceScore(15.0);
        grandChild.setSafetyScore(7.5);
    }

    @Test
    void buildGraphTraversesDescendantsBreadthFirst() {
        when(repository.findById(root.getAgentId())).thenReturn(Optional.of(root));
        when(repository.findByParentId(root.getAgentId())).thenReturn(List.of(child));
        when(repository.findByParentId(child.getAgentId())).thenReturn(List.of(grandChild));
        when(repository.findByParentId(grandChild.getAgentId())).thenReturn(Collections.emptyList());

        LineageGraphView graph = service.buildGraph(root.getAgentId());

        assertThat(graph.nodes()).hasSize(3);
        assertThat(graph.edges()).hasSize(2);
        assertThat(graph.generationStats()).containsKeys(0, 1, 2);
    }

    @Test
    void findAncestorsRespectsDepthLimit() {
        when(repository.findById(child.getAgentId())).thenReturn(Optional.of(child));
        when(repository.findById(root.getAgentId())).thenReturn(Optional.of(root));

        List<LineageNodeView> ancestors = service.findAncestors(child.getAgentId(), 1);

        assertThat(ancestors).hasSize(1);
        assertThat(ancestors.get(0).agentId()).isEqualTo(root.getAgentId());
    }

    @Test
    void findDescendantsReturnsOnlyRequestedDepth() {
        when(repository.findById(root.getAgentId())).thenReturn(Optional.of(root));
        when(repository.findByParentId(root.getAgentId())).thenReturn(List.of(child));

        List<LineageNodeView> descendants = service.findDescendants(root.getAgentId(), 1);

        assertThat(descendants).hasSize(1);
        assertThat(descendants.get(0).agentId()).isEqualTo(child.getAgentId());
    }

    @Test
    void findCommonAncestorReturnsFirstSharedNode() {
        when(repository.findById(root.getAgentId())).thenReturn(Optional.of(root));
        when(repository.findById(child.getAgentId())).thenReturn(Optional.of(child));
        when(repository.findById(grandChild.getAgentId())).thenReturn(Optional.of(grandChild));

        Optional<LineageNodeView> ancestor = service.findCommonAncestor(child.getAgentId(), grandChild.getAgentId());

        assertThat(ancestor).isPresent();
        assertThat(ancestor.get().agentId()).isEqualTo(root.getAgentId());
    }

    @Test
    void getGenerationStatsAggregatesPerformanceAndSafety() {
        AgentLineage peer = AgentLineage.create(root.getAgentId(), UUID.randomUUID(), root.getAgentId(), 2);
        peer.setPerformanceScore(5.0);
        peer.setSafetyScore(5.0);
        peer.setEliminationReason("unsafe");
        grandChild.setEliminationReason(null);

        when(repository.findByGeneration(2)).thenReturn(List.of(grandChild, peer));

        LineageGenerationStats stats = service.getGenerationStats(2);

        assertThat(stats.agentCount()).isEqualTo(2);
        assertThat(stats.averagePerformanceScore()).isEqualTo(10.0);
        assertThat(stats.averageSafetyScore()).isEqualTo(6.25);
        assertThat(stats.survivors()).isEqualTo(1);
        assertThat(stats.eliminated()).isEqualTo(1);
    }

    @Test
    void getGenerationStatsThrowsWhenNoLineages() {
        when(repository.findByGeneration(99)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getGenerationStats(99))
                .isInstanceOf(LineageNotFoundException.class);
    }
}

