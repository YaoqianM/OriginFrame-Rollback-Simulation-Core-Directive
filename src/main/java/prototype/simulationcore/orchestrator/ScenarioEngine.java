package prototype.simulationcore.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import prototype.simulationcore.events.SimulationEvent;

@Component
public class ScenarioEngine {

    private static final Logger log = LoggerFactory.getLogger(ScenarioEngine.class);

    private final ResourceLoader resourceLoader;
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;
    private final Map<String, ScenarioDefinition> cache = new ConcurrentHashMap<>();

    public ScenarioEngine(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        this.resourceLoader = resourceLoader;
        this.jsonMapper = objectMapper;
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }

    public ScenarioDefinition loadScenario(String scenarioFile) {
        if (scenarioFile == null || scenarioFile.isBlank()) {
            return ScenarioDefinition.empty("unspecified");
        }
        return cache.computeIfAbsent(scenarioFile, this::readScenario);
    }

    public void applyScenario(SimulationWorld world, ScenarioDefinition scenario) {
        world.attachScenario(scenario);
        world.applyInitialEnvironment(scenario.initialState());
        world.getScheduler().clear();
        scenario.events().forEach(eventDefinition -> {
            SimulationEvent event = SimulationEvent.of(
                    eventDefinition.type(),
                    world.getSimulationId(),
                    eventDefinition.tick(),
                    eventDefinition.payload()
            );
            world.getScheduler().scheduleEvent(eventDefinition.tick(), event);
        });
        log.info("Scenario {} applied to simulation {}", scenario.name(), world.getSimulationId());
    }

    public void executeScenarioStep(SimulationWorld world, long tick) {
        world.getScenarioDefinition().ifPresent(scenario -> {
            ScenarioSuccessCriteria criteria = scenario.successCriteria();
            if (criteria != null && criteria.isSatisfied(world, tick)) {
                world.setStatus(SimulationWorldStatus.COMPLETED);
                log.info("Scenario {} satisfied at tick {} for simulation {}", scenario.name(), tick, world.getSimulationId());
            }
        });
    }

    private ScenarioDefinition readScenario(String scenarioFile) {
        Resource resource = resolveResource(scenarioFile);
        if (!resource.exists()) {
            throw new IllegalArgumentException("Scenario file not found: " + scenarioFile);
        }
        try (InputStream inputStream = resource.getInputStream()) {
            if (scenarioFile.endsWith(".yml") || scenarioFile.endsWith(".yaml")) {
                return yamlMapper.readValue(inputStream, ScenarioDefinition.class);
            }
            return jsonMapper.readValue(inputStream, ScenarioDefinition.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to parse scenario " + scenarioFile, ex);
        }
    }

    private Resource resolveResource(String scenarioFile) {
        if (scenarioFile.startsWith("classpath:") || scenarioFile.startsWith("file:")) {
            return resourceLoader.getResource(scenarioFile);
        }
        Resource classpath = resourceLoader.getResource("classpath:scenarios/" + scenarioFile);
        if (classpath.exists()) {
            return classpath;
        }
        return resourceLoader.getResource("file:" + scenarioFile);
    }
}

