package prototype.lineageruntime.recovery;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.service.RollbackService;
import prototype.simulationcore.service.SimulationService;

@Service
public class RecoveryWorkflowOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RecoveryWorkflowOrchestrator.class);
    private static final String RECOVERY_TOPIC = "recovery-events";
    private static final String ROLLBACK_TOPIC = "rollback-events";

    private final ServiceReconstructor serviceReconstructor;
    private final DependencyHealer dependencyHealer;
    private final FailoverManager failoverManager;
    private final ServiceTopology topology;
    private final RollbackService rollbackService;
    private final SimulationService simulationService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RecoveryWorkflowOrchestrator(ServiceReconstructor serviceReconstructor,
                                        DependencyHealer dependencyHealer,
                                        FailoverManager failoverManager,
                                        ServiceTopology topology,
                                        RollbackService rollbackService,
                                        SimulationService simulationService,
                                        KafkaTemplate<String, Object> kafkaTemplate) {
        this.serviceReconstructor = serviceReconstructor;
        this.dependencyHealer = dependencyHealer;
        this.failoverManager = failoverManager;
        this.topology = topology;
        this.rollbackService = rollbackService;
        this.simulationService = simulationService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public RecoveryExecutionReport recover(String serviceId) {
        String workflowId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        List<WorkflowStepResult> steps = new ArrayList<>();

        ServiceSnapshot initialSnapshot = topology.snapshot(serviceId);
        steps.add(recordStep(workflowId, serviceId, WorkflowStage.DETECT, true,
                "Detected anomaly at status " + initialSnapshot.status(),
                Map.of("status", initialSnapshot.status().name())));

        List<DependencyImpact> impacts = dependencyHealer.analyzeImpact(serviceId);
        topology.updateStatus(serviceId, ServiceStatus.ISOLATED);
        steps.add(recordStep(workflowId, serviceId, WorkflowStage.ISOLATE, true,
                "Isolated service and identified " + impacts.size() + " impacted dependents",
                Map.of("impacted", impacts.stream().map(DependencyImpact::serviceId).toList())));

        List<DependencyHealResult> healResults = dependencyHealer.healDependencies(serviceId);

        steps.add(performRollback(workflowId, serviceId));

        ServiceRecoveryAction recoveryAction = serviceReconstructor.restartService(serviceId);
        steps.add(recordStep(workflowId, serviceId, WorkflowStage.RECOVER, recoveryAction.success(),
                recoveryAction.detail(), Map.of("attempts", recoveryAction.attempts(), "mode", "RESTART")));

        ServiceRecoveryAction redeployAction = null;
        boolean recoverySucceeded = recoveryAction.success();
        if (!recoverySucceeded) {
            redeployAction = serviceReconstructor.redeployService(serviceId, recoveryAction.snapshot().version());
            recoverySucceeded = redeployAction.success();
            String version = redeployAction.snapshot().version() == null ? "unknown" : redeployAction.snapshot().version();
            steps.add(recordStep(workflowId, serviceId, WorkflowStage.RECOVER, redeployAction.success(),
                    redeployAction.detail(),
                    Map.of("attempts", redeployAction.attempts(), "mode", "REDEPLOY", "version", version)));
        }

        boolean dependenciesHealthy = dependencyHealer.validateDependencyChain(serviceId);
        boolean serviceHealthy = serviceReconstructor.verifyHealth(serviceId);
        if (dependenciesHealthy && serviceHealthy && topology.snapshot(serviceId).fallbackActive()) {
            failoverManager.deactivateFallback(serviceId);
        }

        boolean validationSuccess = dependenciesHealthy && serviceHealthy;
        steps.add(recordStep(workflowId, serviceId, WorkflowStage.VALIDATE, validationSuccess,
                buildValidationDetail(dependenciesHealthy, serviceHealthy),
                Map.of("dependenciesHealthy", dependenciesHealthy, "serviceHealthy", serviceHealthy)));

        Instant completedAt = Instant.now();
        ServiceSnapshot finalSnapshot = topology.snapshot(serviceId);
        boolean overallSuccess = recoverySucceeded && validationSuccess;
        String message = overallSuccess
                ? "Recovery workflow completed successfully"
                : "Recovery workflow completed with issues";

        return new RecoveryExecutionReport(
                workflowId,
                serviceId,
                startedAt,
                completedAt,
                overallSuccess,
                message,
                finalSnapshot,
                List.copyOf(steps),
                impacts,
                healResults
        );
    }

    private WorkflowStepResult performRollback(String workflowId, String serviceId) {
        Agent agent = simulationService.currentAgent();
        AgentState before = agent.snapshotState();
        Agent result = rollbackService.rollback();
        AgentState after = result.snapshotState();

        RollbackEvent event = new RollbackEvent(
                workflowId,
                serviceId,
                result.getAgentId() == null ? "unknown-agent" : result.getAgentId().toString(),
                before.energy(),
                after.energy(),
                Instant.now(),
                "Rollback executed for service " + serviceId
        );
        kafkaTemplate.send(ROLLBACK_TOPIC, serviceId, event);
        log.info("Rollback completed for service {} from {} -> {}", serviceId, before.energy(), after.energy());

        return recordStep(workflowId, serviceId, WorkflowStage.ROLLBACK, true,
                "Rollback executed (energy " + before.energy() + " -> " + after.energy() + ")", Map.of());
    }

    private WorkflowStepResult recordStep(String workflowId,
                                          String serviceId,
                                          WorkflowStage stage,
                                          boolean success,
                                          String detail,
                                          Map<String, Object> metadata) {
        WorkflowStepResult result = new WorkflowStepResult(stage, success, detail, Instant.now());
        RecoveryWorkflowEvent event = new RecoveryWorkflowEvent(
                workflowId,
                serviceId,
                stage,
                success ? "SUCCESS" : "FAILURE",
                detail,
                result.occurredAt(),
                metadata == null ? Map.of() : Map.copyOf(metadata)
        );
        kafkaTemplate.send(RECOVERY_TOPIC, serviceId, event);
        return result;
    }

    private String buildValidationDetail(boolean dependenciesHealthy, boolean serviceHealthy) {
        if (dependenciesHealthy && serviceHealthy) {
            return "All dependency chains and service health checks passed";
        }
        if (!dependenciesHealthy && !serviceHealthy) {
            return "Service unhealthy and dependency chain contains faults";
        }
        if (!dependenciesHealthy) {
            return "Dependencies remain degraded";
        }
        return "Service did not pass health verification";
    }
}
