package prototype.simulationcore.evolution.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.simulationcore.evolution.domain.RewardTrajectory;

public interface RewardTrajectoryRepository extends JpaRepository<RewardTrajectory, UUID> {

    Optional<RewardTrajectory> findByAgentId(UUID agentId);

    List<RewardTrajectory> findTop10ByOrderByCumulativeRewardDesc();
}


