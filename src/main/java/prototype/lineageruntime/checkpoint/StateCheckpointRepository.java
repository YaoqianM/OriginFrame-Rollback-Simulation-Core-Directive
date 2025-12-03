package prototype.lineageruntime.checkpoint;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StateCheckpointRepository extends JpaRepository<StateCheckpoint, UUID> {

    Optional<StateCheckpoint> findFirstByServiceIdOrderByTimestampDesc(String serviceId);

    java.util.List<StateCheckpoint> findByServiceIdOrderByTimestampDesc(String serviceId, Pageable pageable);

    void deleteByTimestampBefore(Instant timestamp);
}

