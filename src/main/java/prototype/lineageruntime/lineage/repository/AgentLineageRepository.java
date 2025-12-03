package prototype.lineageruntime.lineage.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.lineageruntime.lineage.domain.AgentLineage;

public interface AgentLineageRepository extends JpaRepository<AgentLineage, UUID> {

    List<AgentLineage> findByParentId(UUID parentId);

    List<AgentLineage> findByLineageId(UUID lineageId);

    List<AgentLineage> findByGeneration(int generation);

    List<AgentLineage> findByAgentIdIn(Collection<UUID> agentIds);
}

