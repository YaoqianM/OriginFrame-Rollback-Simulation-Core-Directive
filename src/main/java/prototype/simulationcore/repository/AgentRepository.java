package prototype.simulationcore.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.simulationcore.domain.Agent;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    Optional<Agent> findTopByOrderByCreatedAtAsc();
}


