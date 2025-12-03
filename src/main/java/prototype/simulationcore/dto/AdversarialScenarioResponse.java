package prototype.simulationcore.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.UUID;
import prototype.simulationcore.adversarial.model.StressTestReport;

@JsonInclude(Include.NON_NULL)
public class AdversarialScenarioResponse {

    private UUID scenarioId;
    private String status;
    private StressTestReport stressTestReport;

    public static AdversarialScenarioResponse injected(UUID scenarioId) {
        AdversarialScenarioResponse response = new AdversarialScenarioResponse();
        response.setScenarioId(scenarioId);
        response.setStatus("INJECTED");
        return response;
    }

    public static AdversarialScenarioResponse scheduled(UUID scenarioId) {
        AdversarialScenarioResponse response = new AdversarialScenarioResponse();
        response.setScenarioId(scenarioId);
        response.setStatus("SCHEDULED");
        return response;
    }

    public static AdversarialScenarioResponse randomized(UUID scenarioId) {
        AdversarialScenarioResponse response = new AdversarialScenarioResponse();
        response.setScenarioId(scenarioId);
        response.setStatus(scenarioId == null ? "SKIPPED" : "RANDOMIZED");
        return response;
    }

    public static AdversarialScenarioResponse stressReport(StressTestReport report) {
        AdversarialScenarioResponse response = new AdversarialScenarioResponse();
        response.setStatus("STRESS_TEST_COMPLETED");
        response.setStressTestReport(report);
        return response;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public StressTestReport getStressTestReport() {
        return stressTestReport;
    }

    public void setStressTestReport(StressTestReport stressTestReport) {
        this.stressTestReport = stressTestReport;
    }
}


