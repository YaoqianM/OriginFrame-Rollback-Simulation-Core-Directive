package prototype.simulationcore.service;

import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.checkpoint.RuntimeServiceIds;
import prototype.lineageruntime.checkpoint.ServiceOperationEvent;
import prototype.lineageruntime.kafka.EventProducer;
import prototype.lineageruntime.lineage.model.LineageMetrics;
import prototype.lineageruntime.lineage.service.LineageTrackerService;
import prototype.lineageruntime.resilience.CircuitBreakerGuard;
import prototype.simulationcore.adversarial.ScenarioApplicationResult;
import prototype.simulationcore.adversarial.service.ScenarioInjector;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.LineageEvent;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.repository.AgentRepository;
import prototype.simulationcore.safety.ConstraintValidator;
import prototype.simulationcore.safety.monitor.SafetyMonitor;

@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final EventProducer eventProducer;
    private final ApplicationEventPublisher eventPublisher;
    private final AgentRepository agentRepository;
    private final AgentPolicyBootstrapper policyBootstrapper;
    private final AgentDynamics agentDynamics;
    private final ScenarioInjector scenarioInjector;
    private final LineageTrackerService lineageTrackerService;
    private final ConstraintValidator constraintValidator;
    private final SafetyMonitor safetyMonitor;

    public SimulationService(EventProducer eventProducer,
                             ApplicationEventPublisher eventPublisher,
                             AgentRepository agentRepository,
                             AgentPolicyBootstrapper policyBootstrapper,
                             AgentDynamics agentDynamics,
                             ScenarioInjector scenarioInjector,
                             LineageTrackerService lineageTrackerService,
                             ConstraintValidator constraintValidator,
                             SafetyMonitor safetyMonitor) {
        this.eventProducer = eventProducer;
        this.eventPublisher = eventPublisher;
        this.agentRepository = agentRepository;
        this.policyBootstrapper = policyBootstrapper;
        this.agentDynamics = agentDynamics;
        this.scenarioInjector = scenarioInjector;
        this.lineageTrackerService = lineageTrackerService;
        this.constraintValidator = constraintValidator;
        this.safetyMonitor = safetyMonitor;
    }

    @CircuitBreakerGuard(serviceId = "simulation-service")
    @Transactional
    public Agent step() {
        Agent agent = resolveActiveAgent();
        if (safetyMonitor.isEliminationCandidate(agent.getAgentId())) {
            log.warn("Agent {} flagged for elimination. Recycling into new cohort.", agent.getAgentId());
            agentRepository.delete(agent);
            Agent replacement = Agent.bootstrap(policyBootstrapper.resolveDefaultPolicy());
            return agentRepository.save(replacement);
        }
        AgentState previous = agent.snapshotState();
        Environment environment = new DefaultEnvironment(previous);
        String simulationId = agent.getAgentId() == null
                ? ScenarioInjector.DEFAULT_SIMULATION_ID
                : agent.getAgentId().toString();
        ScenarioApplicationResult scenarioResult = scenarioInjector.applyActiveScenarios(simulationId, environment);
        Environment actionEnvironment = scenarioResult.environment();
        Action action = agent.decide(actionEnvironment);
        if (!constraintValidator.preActionCheck(agent, action, actionEnvironment)) {
            log.warn("Action {} blocked by safety constraints for agent {}", action, agent.getAgentId());
            return agent;
        }
        AgentState updated = agentDynamics.apply(action, previous);
        agent.setState(updated);
        double fitnessDelta = agentDynamics.score(action);
        agent.adjustFitness(fitnessDelta);
        agent.incrementGeneration();

        Agent persisted = agentRepository.save(agent);

        LineageEvent event = LineageEvent.capture(agent.getAgentId().toString(), previous, updated);
        eventProducer.send(event);
        eventPublisher.publishEvent(new ServiceOperationEvent(RuntimeServiceIds.SIMULATION_CORE));
        scenarioInjector.recordPerformanceImpact(simulationId, scenarioResult.perturbationRecordIds(), previous, updated, fitnessDelta);
        constraintValidator.postActionAudit(agent, action, updated);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("lastAction", action.name());
        metadata.put("energy", updated.energy());
        metadata.put("resources", updated.resources());
        metadata.put("timestamp", Instant.now().toString());
        metadata.put("scenarioPerturbations", scenarioResult.perturbationRecordIds());
        lineageTrackerService.recordPerformanceUpdate(persisted, LineageMetrics.fromAgent(persisted, metadata));
        agentDynamics.evaluateSafety(updated)
                .ifPresent(reason -> lineageTrackerService.recordViolation(persisted, reason));

        return persisted;
    }

    @Transactional
    public Agent currentAgent() {
        return resolveActiveAgent();
    }

    private Agent resolveActiveAgent() {
        return agentRepository.findTopByOrderByCreatedAtAsc()
                .orElseGet(() -> agentRepository.save(Agent.bootstrap(policyBootstrapper.resolveDefaultPolicy())));
    }

}


