package prototype.lineageruntime.lineage.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import prototype.lineageruntime.lineage.domain.AgentLineage;
import prototype.lineageruntime.lineage.domain.MutationEvent;
import prototype.lineageruntime.lineage.dto.LineageComparison;
import prototype.lineageruntime.lineage.dto.LineageNodeView;
import prototype.lineageruntime.lineage.dto.SuccessfulMutationPattern;
import prototype.lineageruntime.lineage.dto.ViolationTrace;
import prototype.lineageruntime.lineage.exception.LineageNotFoundException;
import prototype.lineageruntime.lineage.repository.AgentLineageRepository;

@Service
public class LineageAnalyzer {

    private final AgentLineageRepository repository;
    private final LineageGraphService graphService;

    public LineageAnalyzer(AgentLineageRepository repository,
                           LineageGraphService graphService) {
        this.repository = repository;
        this.graphService = graphService;
    }

    @Transactional
    public Optional<ViolationTrace> findViolationOrigin(UUID agentId) {
        AgentLineage lineage = repository.findById(agentId)
                .orElseThrow(() -> new LineageNotFoundException(agentId));
        return lineage.getMutationsApplied().stream()
                .filter(MutationEvent::isViolationLinked)
                .max(Comparator.comparing(MutationEvent::getAppliedAt))
                .map(event -> new ViolationTrace(agentId, event, lineage.getEliminationReason()));
    }

    @Transactional
    public LineageComparison compareLineages(UUID first, UUID second) {
        AgentLineage firstLineage = repository.findById(first)
                .orElseThrow(() -> new LineageNotFoundException(first));
        AgentLineage secondLineage = repository.findById(second)
                .orElseThrow(() -> new LineageNotFoundException(second));

        LineageNodeView firstView = graphService.getLineage(first);
        LineageNodeView secondView = graphService.getLineage(second);

        Set<String> firstMutations = firstLineage.getMutationsApplied().stream()
                .map(MutationEvent::getType)
                .collect(Collectors.toSet());
        List<String> sharedMutations = secondLineage.getMutationsApplied().stream()
                .map(MutationEvent::getType)
                .filter(firstMutations::contains)
                .distinct()
                .toList();

        return new LineageComparison(
                firstView,
                secondView,
                firstView.performanceScore() - secondView.performanceScore(),
                firstView.safetyScore() - secondView.safetyScore(),
                firstView.generation() - secondView.generation(),
                sharedMutations
        );
    }

    @Transactional
    public List<SuccessfulMutationPattern> identifySuccessfulMutationPatterns() {
        List<AgentLineage> lineages = repository.findAll();
        Map<String, MutationAggregation> aggregations = new HashMap<>();

        for (AgentLineage lineage : lineages) {
            for (MutationEvent mutation : lineage.getMutationsApplied()) {
                aggregations.computeIfAbsent(mutation.getType(), key -> new MutationAggregation())
                        .accumulate(mutation);
            }
        }

        return aggregations.entrySet().stream()
                .map(entry -> new SuccessfulMutationPattern(
                        entry.getKey(),
                        entry.getValue().count,
                        entry.getValue().performanceDeltaSum / entry.getValue().count,
                        entry.getValue().safetyDeltaSum / entry.getValue().count
                ))
                .sorted(Comparator.comparing(SuccessfulMutationPattern::avgPerformanceDelta).reversed())
                .toList();
    }

    private static final class MutationAggregation {
        private long count;
        private double performanceDeltaSum;
        private double safetyDeltaSum;

        private void accumulate(MutationEvent event) {
            count++;
            performanceDeltaSum += event.getPerformanceDelta();
            safetyDeltaSum += event.getSafetyDelta();
        }
    }
}

