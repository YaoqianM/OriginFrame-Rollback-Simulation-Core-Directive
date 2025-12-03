package prototype.simulationcore.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.simulationcore.policy.AbstractAgentPolicy;

public interface AgentPolicyRepository extends JpaRepository<AbstractAgentPolicy, UUID> {

    Optional<AbstractAgentPolicy> findTopByOrderByCreatedAtAsc();
}


