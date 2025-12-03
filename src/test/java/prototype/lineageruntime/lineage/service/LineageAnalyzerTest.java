package prototype.lineageruntime.lineage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import prototype.lineageruntime.lineage.domain.AgentLineage;
import prototype.lineageruntime.lineage.domain.MutationEvent;
import prototype.lineageruntime.lineage.dto.LineageNodeView;
import prototype.lineageruntime.lineage.dto.LineageComparison;
import prototype.lineageruntime.lineage.dto.SuccessfulMutationPattern;
import prototype.lineageruntime.lineage.dto.ViolationTrace;
import prototype.lineageruntime.lineage.repository.AgentLineageRepository;

@ExtendWith(MockitoExtension.class)
class LineageAnalyzerTest {

    @Mock
    private AgentLineageRepository repository;

    @Mock
    private LineageGraphService graphService;

    @InjectMocks
    private LineageAnalyzer analyzer;

    @Test
    void identifySuccessfulMutationPatternsAggregatesByType() {
        AgentLineage first = AgentLineage.create(UUID.randomUUID(), UUID.randomUUID(), null, 1);
        first.setMutationsApplied(List.of(
                MutationEvent.of("energy", "increase energy", 2.0, 0.5, "system", false),
                MutationEvent.of("resilience", "better shielding", 1.0, 1.5, "system", false)
        ));
        AgentLineage second = AgentLineage.create(first.getLineageId(), UUID.randomUUID(), first.getAgentId(), 2);
        second.setMutationsApplied(List.of(
                MutationEvent.of("energy", "micro-optimizations", 1.0, 0.2, "system", false)
        ));
        when(repository.findAll()).thenReturn(List.of(first, second));

        List<SuccessfulMutationPattern> patterns = analyzer.identifySuccessfulMutationPatterns();

        assertThat(patterns)
                .hasSize(2)
                .extracting(SuccessfulMutationPattern::mutationType)
                .containsExactly("energy", "resilience");
        SuccessfulMutationPattern energy = patterns.get(0);
        assertThat(energy.occurrences()).isEqualTo(2);
        assertThat(energy.avgPerformanceDelta()).isCloseTo(1.5, withinTolerance());
    }

    @Test
    void findViolationOriginReturnsLatestLinkedMutation() {
        UUID agentId = UUID.randomUUID();
        AgentLineage lineage = AgentLineage.create(agentId, agentId, null, 3);
        MutationEvent benign = MutationEvent.of("energy", "safe", 0.5, 0.0, "system", false);
        MutationEvent violating = MutationEvent.of("safety", "toxic exposure", -2.0, -5.0, "system", true);
        lineage.setMutationsApplied(List.of(benign, violating));
        lineage.setEliminationReason("Energy depleted");

        when(repository.findById(agentId)).thenReturn(Optional.of(lineage));

        Optional<ViolationTrace> trace = analyzer.findViolationOrigin(agentId);

        assertThat(trace).isPresent();
        assertThat(trace.get().originMutation()).isEqualTo(violating);
    }

    @Test
    void compareLineagesHighlightsGaps() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        AgentLineage first = AgentLineage.create(firstId, firstId, null, 5);
        first.setPerformanceScore(50.0);
        first.setSafetyScore(90.0);
        first.setMutationsApplied(List.of(MutationEvent.of("energy", "boost", 2.0, 0.0, "system", false)));
        AgentLineage second = AgentLineage.create(secondId, secondId, firstId, 3);
        second.setPerformanceScore(40.0);
        second.setSafetyScore(80.0);
        second.setMutationsApplied(List.of(MutationEvent.of("energy", "boost", 1.0, 0.0, "system", false)));

        when(repository.findById(firstId)).thenReturn(Optional.of(first));
        when(repository.findById(secondId)).thenReturn(Optional.of(second));
        when(graphService.getLineage(firstId)).thenReturn(LineageNodeView.from(first));
        when(graphService.getLineage(secondId)).thenReturn(LineageNodeView.from(second));

        LineageComparison comparison = analyzer.compareLineages(firstId, secondId);

        assertThat(comparison.performanceGap()).isEqualTo(10.0);
        assertThat(comparison.sharedMutationTypes()).containsExactly("energy");
    }

    private static org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(0.001);
    }
}

