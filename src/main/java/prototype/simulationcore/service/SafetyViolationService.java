package prototype.simulationcore.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import prototype.simulationcore.repository.SafetyViolationRepository;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.domain.SafetyViolation;

@Service
public class SafetyViolationService {

    private final SafetyViolationRepository repository;

    public SafetyViolationService(SafetyViolationRepository repository) {
        this.repository = repository;
    }

    public List<SafetyViolation> findViolations(UUID agentId, Severity severity) {
        if (agentId != null && severity != null) {
            return repository.findByAgentIdAndSeverity(agentId, severity);
        }
        if (agentId != null) {
            return repository.findByAgentId(agentId);
        }
        if (severity != null) {
            return repository.findBySeverity(severity);
        }
        return repository.findAll();
    }
}

