package prototype.simulationcore.orchestrator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.lineageruntime.health.HealthMonitorService;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.events.SimulationEvent;
import prototype.simulationcore.events.SimulationEventType;
import prototype.simulationcore.kafka.SimulationEventPublisher;
import prototype.simulationcore.service.SafetyConstraintsService;
import prototype.simulationcore.service.SimulationService;

@Component
public class TickManager {

    private static final Logger log = LoggerFactory.getLogger(TickManager.class);

    private final SimulationService simulationService;
    private final SafetyConstraintsService safetyConstraintsService;
    private final HealthMonitorService healthMonitorService;
    private final SimulationEventPublisher eventPublisher;
    private final ScenarioEngine scenarioEngine;

    public TickManager(SimulationService simulationService,
                       SafetyConstraintsService safetyConstraintsService,
                       HealthMonitorService healthMonitorService,
                       SimulationEventPublisher eventPublisher,
                       ScenarioEngine scenarioEngine) {
        this.simulationService = simulationService;
        this.safetyConstraintsService = safetyConstraintsService;
        this.healthMonitorService = healthMonitorService;
        this.eventPublisher = eventPublisher;
        this.scenarioEngine = scenarioEngine;
    }

    public SimulationTickResult processTick(SimulationWorld world) {
        if (world.getStatus() == SimulationWorldStatus.COMPLETED
                || world.getStatus() == SimulationWorldStatus.STOPPED) {
            log.debug("Skipping tick for simulation {} while in terminal state {}", world.getSimulationId(), world.getStatus());
            return new SimulationTickResult(world.getSimulationId(), world.getCurrentTick(), Instant.now(), false, world.snapshotEnvironment());
        }

        long tick = world.incrementTick();
        log.debug("Processing tick {} for simulation {}", tick, world.getSimulationId());

        scenarioEngine.executeScenarioStep(world, tick);

        Map<String, Object> environmentSnapshot = world.mutateEnvironment(tick);
        emitScheduledEvents(world, tick);

        Agent agent = simulationService.step();
        SafetyConstraintsService.SafetyEvaluation evaluation = safetyConstraintsService.enforce(agent);

        if (evaluation.violated()) {
            world.recordConstraintViolation();
            SimulationEvent violation = SimulationEvent.of(
                    SimulationEventType.CONSTRAINT_VIOLATED,
                    world.getSimulationId(),
                    tick,
                    Map.of(
                            "agentId", agent.getAgentId().toString(),
                            "reason", evaluation.reason()
                    )
            );
            publish(world, violation);
        }

        updateInfrastructureHealth(world);

        SimulationEvent tickEvent = SimulationEvent.of(
                SimulationEventType.TICK_COMPLETED,
                world.getSimulationId(),
                tick,
                Map.of(
                        "agentId", agent.getAgentId().toString(),
                        "generation", agent.getGeneration(),
                        "status", world.getStatus().name()
                )
        );
        publish(world, tickEvent);

        return new SimulationTickResult(
                world.getSimulationId(),
                tick,
                Instant.now(),
                evaluation.violated(),
                environmentSnapshot
        );
    }

    private void publish(SimulationWorld world, SimulationEvent event) {
        world.recordEvent(event);
        eventPublisher.publish(event);
    }

    private void emitScheduledEvents(SimulationWorld world, long tick) {
        List<SimulationEvent> due = world.getScheduler().drainDueEvents(tick);
        due.forEach(event -> {
            log.debug("Executing scheduled {} for simulation {}", event.getType(), world.getSimulationId());
            publish(world, event);
        });
    }

    private void updateInfrastructureHealth(SimulationWorld world) {
        String serviceId = world.getConfig().getName();
        double latency = ThreadLocalRandom.current().nextDouble(1.0, 25.0);
        double errorRate = ThreadLocalRandom.current().nextDouble(0.0, 0.05);
        healthMonitorService.recordLatency(serviceId, latency);
        healthMonitorService.recordErrorRate(serviceId, errorRate);
        healthMonitorService.recordHeartbeat(serviceId);
    }
}

