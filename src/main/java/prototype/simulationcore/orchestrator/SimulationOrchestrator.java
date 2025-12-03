package prototype.simulationcore.orchestrator;

import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimulationOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SimulationOrchestrator.class);

    private final TickManager tickManager;
    private final ScenarioEngine scenarioEngine;
    private final Map<UUID, SimulationWorld> worlds = new ConcurrentHashMap<>();
    private final Map<UUID, Future<?>> activeLoops = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SimulationOrchestrator(TickManager tickManager, ScenarioEngine scenarioEngine) {
        this.tickManager = tickManager;
        this.scenarioEngine = scenarioEngine;
    }

    public SimulationWorld createSimulation(SimulationConfig config) {
        SimulationWorld world = new SimulationWorld(config);
        if (config.getScenarioFile() != null) {
            ScenarioDefinition scenario = scenarioEngine.loadScenario(config.getScenarioFile());
            scenarioEngine.applyScenario(world, scenario);
        }
        worlds.put(world.getSimulationId(), world);
        log.info("Created simulation {} ({})", world.getSimulationId(), config.getName());
        return world;
    }

    public List<SimulationWorld> listSimulations() {
        return List.copyOf(worlds.values());
    }

    public SimulationWorld start(UUID simulationId) {
        SimulationWorld world = resolve(simulationId);
        if (world.getStatus() == SimulationWorldStatus.RUNNING) {
            return world;
        }
        world.setStatus(SimulationWorldStatus.RUNNING);
        Future<?> loop = executorService.submit(() -> runLoop(world));
        activeLoops.put(simulationId, loop);
        log.info("Simulation {} started", simulationId);
        return world;
    }

    public SimulationWorld pause(UUID simulationId) {
        SimulationWorld world = resolve(simulationId);
        if (world.getStatus() != SimulationWorldStatus.RUNNING) {
            return world;
        }
        world.setStatus(SimulationWorldStatus.PAUSED);
        cancelLoop(simulationId);
        log.info("Simulation {} paused at tick {}", simulationId, world.getCurrentTick());
        return world;
    }

    public SimulationWorld stop(UUID simulationId) {
        SimulationWorld world = resolve(simulationId);
        world.setStatus(SimulationWorldStatus.STOPPED);
        cancelLoop(simulationId);
        world.getScheduler().clear();
        log.info("Simulation {} stopped", simulationId);
        return world;
    }

    public SimulationWorld step(UUID simulationId) {
        SimulationWorld world = resolve(simulationId);
        if (world.getStatus() == SimulationWorldStatus.RUNNING) {
            throw new IllegalStateException("Cannot step while simulation is running");
        }
        tickManager.processTick(world);
        return world;
    }

    public SimulationWorld runUntil(UUID simulationId, long targetTick) {
        SimulationWorld world = resolve(simulationId);
        if (world.getStatus() == SimulationWorldStatus.RUNNING) {
            throw new IllegalStateException("Pause simulation before runUntil");
        }
        while (world.getCurrentTick() < targetTick
                && world.getStatus() != SimulationWorldStatus.COMPLETED
                && world.getStatus() != SimulationWorldStatus.STOPPED) {
            tickManager.processTick(world);
            enforceMaxTicks(world);
        }
        return world;
    }

    public SimulationWorld loadScenario(UUID simulationId, String scenarioFile) {
        SimulationWorld world = resolve(simulationId);
        ScenarioDefinition scenario = scenarioEngine.loadScenario(scenarioFile);
        scenarioEngine.applyScenario(world, scenario);
        return world;
    }

    public Optional<SimulationWorld> find(UUID simulationId) {
        return Optional.ofNullable(worlds.get(simulationId));
    }

    private void runLoop(SimulationWorld world) {
        try {
            while (world.getStatus() == SimulationWorldStatus.RUNNING) {
                tickManager.processTick(world);
                enforceMaxTicks(world);
                if (world.getStatus() == SimulationWorldStatus.COMPLETED) {
                    log.info("Simulation {} completed", world.getSimulationId());
                    break;
                }
                sleep(world.getConfig().getTickInterval());
            }
        } catch (Exception ex) {
            world.setStatus(SimulationWorldStatus.FAILED);
            log.error("Simulation {} failed", world.getSimulationId(), ex);
        } finally {
            activeLoops.remove(world.getSimulationId());
        }
    }

    private void enforceMaxTicks(SimulationWorld world) {
        long maxTicks = world.getConfig().getMaxTicks();
        if (maxTicks > 0 && world.getCurrentTick() >= maxTicks) {
            world.setStatus(SimulationWorldStatus.COMPLETED);
        }
    }

    private void sleep(Duration interval) {
        if (interval == null || interval.isZero() || interval.isNegative()) {
            return;
        }
        try {
            Thread.sleep(interval.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private SimulationWorld resolve(UUID simulationId) {
        SimulationWorld world = worlds.get(simulationId);
        if (world == null) {
            throw new IllegalArgumentException("Simulation not found: " + simulationId);
        }
        return world;
    }

    private void cancelLoop(UUID simulationId) {
        Future<?> future = activeLoops.remove(simulationId);
        if (future != null) {
            future.cancel(true);
        }
    }

    @PreDestroy
    void shutdown() {
        activeLoops.keySet().forEach(this::cancelLoop);
        executorService.shutdownNow();
    }
}

