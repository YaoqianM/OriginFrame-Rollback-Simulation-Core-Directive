package prototype.lineageruntime.recovery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DependencyHealer {

    private static final Logger log = LoggerFactory.getLogger(DependencyHealer.class);

    private final ServiceTopology topology;
    private final ServiceReconstructor serviceReconstructor;
    private final FailoverManager failoverManager;

    public DependencyHealer(ServiceTopology topology,
                            ServiceReconstructor serviceReconstructor,
                            FailoverManager failoverManager) {
        this.topology = topology;
        this.serviceReconstructor = serviceReconstructor;
        this.failoverManager = failoverManager;
    }

    public List<DependencyImpact> analyzeImpact(String failedServiceId) {
        Set<String> impacted = new LinkedHashSet<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        queue.add(failedServiceId);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            Set<String> dependents = topology.dependentsOf(current);
            for (String dependent : dependents) {
                if (impacted.add(dependent)) {
                    queue.addLast(dependent);
                }
            }
        }

        return impacted.stream()
                .map(service -> new DependencyImpact(service, DependencyImpactType.BLOCKED,
                        List.copyOf(topology.dependenciesOf(service))))
                .toList();
    }

    public List<DependencyHealResult> healDependencies(String failedServiceId) {
        List<DependencyImpact> impacts = analyzeImpact(failedServiceId);
        List<DependencyHealResult> results = new ArrayList<>();

        for (DependencyImpact impact : impacts) {
            DependencyHealResult result;
            if (failoverManager.hasFallback(impact.serviceId())) {
                FailoverAction action = failoverManager.activateFallback(impact.serviceId());
                result = new DependencyHealResult(
                        impact.serviceId(),
                        RecoveryActionType.FAILOVER.name(),
                        action.success(),
                        action.detail(),
                        action.snapshot()
                );
            } else {
                ServiceRecoveryAction action = serviceReconstructor.restartService(impact.serviceId());
                result = DependencyHealResult.fromRecovery(action);
            }
            results.add(result);
            log.info("Healed dependency {} via {}", result.serviceId(), result.strategy());
        }
        return results;
    }

    public boolean validateDependencyChain(String serviceId) {
        return topology.dependenciesOf(serviceId).stream()
                .map(topology::snapshot)
                .allMatch(snapshot -> snapshot.status().isHealthy());
    }
}


