package prototype.simulationcore.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.simulationcore.safety.Severity;
import prototype.simulationcore.safety.domain.SafetyViolation;

public interface SafetyViolationRepository extends JpaRepository<SafetyViolation, UUID> {

    List<SafetyViolation> findByAgentId(UUID agentId);

    List<SafetyViolation> findBySeverity(Severity severity);

    List<SafetyViolation> findByAgentIdAndSeverity(UUID agentId, Severity severity);
}

