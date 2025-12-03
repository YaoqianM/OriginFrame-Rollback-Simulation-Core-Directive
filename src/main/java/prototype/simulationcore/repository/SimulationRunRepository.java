package prototype.simulationcore.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import prototype.simulationcore.world.SimulationRun;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, UUID> {

    Optional<SimulationRun> findByWorldId(UUID worldId);
}


