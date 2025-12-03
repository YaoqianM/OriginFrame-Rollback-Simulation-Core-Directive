package prototype.simulationcore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.List;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AdversarialScenarioRequest {

    public enum Operation {
        INJECT,
        SCHEDULE,
        RANDOM,
        STRESS_TEST
    }

    private String simulationId;
    private String scenarioType;
    private double severity = 0.5;
    private Operation operation = Operation.INJECT;
    private Long tickNumber;
    private Double probability;
    private Integer iterations;
    private List<String> scenarioPool;
    private Map<String, Double> parameters = Map.of();

    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public String getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(String scenarioType) {
        this.scenarioType = scenarioType;
    }

    public double getSeverity() {
        return severity;
    }

    public void setSeverity(double severity) {
        this.severity = severity;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public Long getTickNumber() {
        return tickNumber;
    }

    public void setTickNumber(Long tickNumber) {
        this.tickNumber = tickNumber;
    }

    public Double getProbability() {
        return probability;
    }

    public void setProbability(Double probability) {
        this.probability = probability;
    }

    public Integer getIterations() {
        return iterations;
    }

    public void setIterations(Integer iterations) {
        this.iterations = iterations;
    }

    public List<String> getScenarioPool() {
        return scenarioPool;
    }

    public void setScenarioPool(List<String> scenarioPool) {
        this.scenarioPool = scenarioPool;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Double> parameters) {
        this.parameters = parameters == null ? Map.of() : parameters;
    }
}


