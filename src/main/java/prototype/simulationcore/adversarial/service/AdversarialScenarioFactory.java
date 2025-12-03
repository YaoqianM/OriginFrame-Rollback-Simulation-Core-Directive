package prototype.simulationcore.adversarial.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import prototype.simulationcore.adversarial.AdversarialScenario;
import prototype.simulationcore.adversarial.scenario.CommunicationFailureScenario;
import prototype.simulationcore.adversarial.scenario.EnvironmentShiftScenario;
import prototype.simulationcore.adversarial.scenario.MaliciousAgentScenario;
import prototype.simulationcore.adversarial.scenario.ObstacleInjectionScenario;
import prototype.simulationcore.adversarial.scenario.ResourceScarcityScenario;
import prototype.simulationcore.adversarial.scenario.SensorNoiseScenario;
import prototype.simulationcore.domain.Position;

/**
 * Factory responsible for instantiating scenario implementations.
 */
@Component
public class AdversarialScenarioFactory {

    public AdversarialScenario build(String scenarioType, double severity, Map<String, Double> parameters) {
        String normalized = scenarioType == null ? "" : scenarioType.trim().toLowerCase(Locale.ROOT);
        Map<String, Double> params = parameters == null ? Map.of() : parameters;
        return switch (normalized) {
            case SensorNoiseScenario.TYPE, "sensornoise" ->
                    new SensorNoiseScenario(severity,
                            resolve(params, "maxAmplitude", 10.0),
                            resolve(params, "corruptionProbability", 0.4));
            case EnvironmentShiftScenario.TYPE, "env_shift" ->
                    new EnvironmentShiftScenario(severity,
                            new Position(
                                    resolve(params, "target_dx", 1.0),
                                    resolve(params, "target_dy", 0.5),
                                    resolve(params, "target_dz", 0.0)
                            ),
                            buildDriftParameters(params));
            case ResourceScarcityScenario.TYPE, "scarcity" ->
                    new ResourceScarcityScenario(severity,
                            resolve(params, "depletionRatio", 0.6));
            case ObstacleInjectionScenario.TYPE, "obstacle" ->
                    new ObstacleInjectionScenario(severity,
                            (int) resolve(params, "maxObstacles", 5.0));
            case CommunicationFailureScenario.TYPE, "comms_failure" ->
                    new CommunicationFailureScenario(severity,
                            resolve(params, "dropProbability", 0.8));
            case MaliciousAgentScenario.TYPE, "malicious" ->
                    new MaliciousAgentScenario(severity,
                            resolve(params, "aggressionBias", 0.75));
            default -> throw new IllegalArgumentException("Unsupported scenario type: " + scenarioType);
        };
    }

    public List<AdversarialScenario> defaultScenarioPool() {
        return List.of(
                new SensorNoiseScenario(0.45),
                new EnvironmentShiftScenario(0.55),
                new ResourceScarcityScenario(0.5),
                new ObstacleInjectionScenario(0.6),
                new CommunicationFailureScenario(0.5),
                new MaliciousAgentScenario(0.65)
        );
    }

    public List<AdversarialScenario> fromTypes(List<String> scenarioTypes, double severity, Map<String, Double> parameters) {
        if (scenarioTypes == null || scenarioTypes.isEmpty()) {
            return defaultScenarioPool();
        }
        List<AdversarialScenario> scenarios = new ArrayList<>();
        for (String type : scenarioTypes) {
            scenarios.add(build(type, severity, parameters));
        }
        return scenarios;
    }

    private double resolve(Map<String, Double> parameters, String key, double defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    private Map<String, Double> buildDriftParameters(Map<String, Double> parameters) {
        Map<String, Double> drift = new HashMap<>();
        addIfPresent(drift, parameters, "temperature_drift");
        addIfPresent(drift, parameters, "radiation_drift");
        addIfPresent(drift, parameters, "pressure_drift");
        if (drift.isEmpty()) {
            drift.put("temperature", 5.0);
            drift.put("radiation", 2.0);
        }
        return drift;
    }

    private void addIfPresent(Map<String, Double> drift, Map<String, Double> parameters, String key) {
        if (parameters.containsKey(key)) {
            drift.put(key.replace("_drift", ""), parameters.get(key));
        }
    }
}


