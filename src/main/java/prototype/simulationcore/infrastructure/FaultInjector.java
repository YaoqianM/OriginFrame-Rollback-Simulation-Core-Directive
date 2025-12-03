package prototype.simulationcore.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.lineageruntime.health.HealthMonitorService;
import prototype.lineageruntime.recovery.RecoveryExecutionReport;
import prototype.lineageruntime.recovery.RecoveryWorkflowOrchestrator;
import prototype.lineageruntime.recovery.ServiceStatus;
import prototype.lineageruntime.recovery.ServiceTopology;

/**
 * Applies infrastructure-level failures and integrates with Project A for recovery.
 */
@Component
public class FaultInjector {

    private static final Logger log = LoggerFactory.getLogger(FaultInjector.class);

    private final VirtualNetwork virtualNetwork;
    private final HealthMonitorService healthMonitorService;
    private final RecoveryWorkflowOrchestrator recoveryWorkflowOrchestrator;
    private final ServiceTopology serviceTopology;
    private final NavigableMap<Long, List<UUID>> scheduledFailures = new ConcurrentSkipListMap<>();

    public FaultInjector(VirtualNetwork virtualNetwork,
                         HealthMonitorService healthMonitorService,
                         RecoveryWorkflowOrchestrator recoveryWorkflowOrchestrator,
                         ServiceTopology serviceTopology) {
        this.virtualNetwork = virtualNetwork;
        this.healthMonitorService = healthMonitorService;
        this.recoveryWorkflowOrchestrator = recoveryWorkflowOrchestrator;
        this.serviceTopology = serviceTopology;
    }

    public InfrastructureFaultImpact failNode(UUID nodeId) {
        VirtualNode node = resolveNode(nodeId);
        VirtualNodeStatus previous = node.getStatus();
        node.setStatus(VirtualNodeStatus.FAILED);
        node.getResources().collapse();
        updateTopology(node, ServiceStatus.FAILED);
        node.getServices().forEach(service -> healthMonitorService.recordErrorRate(service.id(), 1.0));

        List<RecoveryWorkflowSummary> recoveries = triggerRecoveries(node);
        boolean recovered = recoveries.stream().allMatch(RecoveryWorkflowSummary::success);
        if (recovered) {
            node.setStatus(VirtualNodeStatus.HEALTHY);
            node.getResources().restore();
            updateTopology(node, ServiceStatus.HEALTHY);
        }

        String message = recovered
                ? "Recovery workflow restored node"
                : "Node remains failed after recovery attempt";
        log.warn("Node {} failed. recoveryTriggered={}, recovered={}",
                node.getName(), !recoveries.isEmpty(), recovered);

        return new InfrastructureFaultImpact(
                node.getNodeId(),
                node.getName(),
                previous,
                node.getStatus(),
                !recoveries.isEmpty(),
                recovered,
                recoveries,
                message
        );
    }

    public InfrastructureFaultImpact degradeNode(UUID nodeId, double percentage) {
        VirtualNode node = resolveNode(nodeId);
        VirtualNodeStatus previous = node.getStatus();
        node.setStatus(VirtualNodeStatus.DEGRADED);
        node.getResources().degrade(percentage);
        node.getServices().forEach(service -> healthMonitorService.recordLatency(service.id(), percentage * 10));
        updateTopology(node, ServiceStatus.DEGRADED);

        return new InfrastructureFaultImpact(
                node.getNodeId(),
                node.getName(),
                previous,
                node.getStatus(),
                false,
                false,
                List.of(),
                "Node degraded by " + percentage + "%"
        );
    }

    public void partitionNetwork(Set<UUID> nodeGroup1, Set<UUID> nodeGroup2) {
        if (nodeGroup1 == null || nodeGroup2 == null) {
            return;
        }
        virtualNetwork.partition(nodeGroup1, nodeGroup2, true);
    }

    public void injectLatency(UUID source, UUID destination, long additionalLatencyMs) {
        virtualNetwork.injectLatency(source, destination, additionalLatencyMs);
    }

    public void scheduleFailure(UUID nodeId, long tick) {
        scheduledFailures.computeIfAbsent(tick, ignored -> new ArrayList<>()).add(nodeId);
    }

    public void processTick(long tick) {
        NavigableMap<Long, List<UUID>> due = scheduledFailures.headMap(tick + 1, true);
        if (due.isEmpty()) {
            return;
        }
        List<Long> processed = new ArrayList<>(due.keySet());
        processed.forEach(t -> {
            List<UUID> nodes = scheduledFailures.remove(t);
            if (nodes != null) {
                nodes.forEach(this::failNode);
            }
        });
    }

    private VirtualNode resolveNode(UUID nodeId) {
        return virtualNetwork.findNode(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node " + nodeId + " not found"));
    }

    private List<RecoveryWorkflowSummary> triggerRecoveries(VirtualNode node) {
        List<RecoveryWorkflowSummary> summaries = new ArrayList<>();
        for (VirtualService service : node.getServices()) {
            try {
                RecoveryExecutionReport report = recoveryWorkflowOrchestrator.recover(service.id());
                summaries.add(RecoveryWorkflowSummary.from(report));
            } catch (Exception ex) {
                log.error("Recovery workflow failed for {}", service.id(), ex);
                summaries.add(new RecoveryWorkflowSummary("unknown", service.id(), false, ex.getMessage()));
            }
        }
        return summaries;
    }

    private void updateTopology(VirtualNode node, ServiceStatus status) {
        String serviceId = node.primaryServiceId();
        if (serviceId != null) {
            serviceTopology.updateStatus(serviceId, status);
        }
    }

    public record InfrastructureFaultImpact(UUID nodeId,
                                            String nodeName,
                                            VirtualNodeStatus previousStatus,
                                            VirtualNodeStatus currentStatus,
                                            boolean recoveryTriggered,
                                            boolean recovered,
                                            List<RecoveryWorkflowSummary> recoveries,
                                            String message) {
    }

    public record RecoveryWorkflowSummary(String workflowId, String serviceId, boolean success, String message) {
        static RecoveryWorkflowSummary from(RecoveryExecutionReport report) {
            Objects.requireNonNull(report, "report");
            return new RecoveryWorkflowSummary(
                    report.workflowId(),
                    report.serviceId(),
                    report.overallSuccess(),
                    report.message()
            );
        }
    }
}


