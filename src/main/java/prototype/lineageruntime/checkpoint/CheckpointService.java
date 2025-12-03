package prototype.lineageruntime.checkpoint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckpointService {

    private static final Logger log = LoggerFactory.getLogger(CheckpointService.class);

    private final StateCheckpointRepository repository;
    private final ServiceStateRegistry registry;

    public CheckpointService(StateCheckpointRepository repository, ServiceStateRegistry registry) {
        this.repository = repository;
        this.registry = registry;
    }

    @Transactional
    public StateCheckpoint createCheckpoint(String serviceId) {
        return createCheckpoint(serviceId, CheckpointType.PERIODIC);
    }

    @Transactional
    public StateCheckpoint createCheckpoint(String serviceId, CheckpointType checkpointType) {
        ServiceStateAdapter adapter = registry.getRequiredAdapter(serviceId);
        String snapshot = adapter.captureSnapshot();
        StateCheckpoint checkpoint = new StateCheckpoint(
                serviceId,
                snapshot,
                checkpointType,
                Instant.now()
        );
        StateCheckpoint saved = repository.save(checkpoint);
        log.info("Created {} checkpoint {} for service {}", checkpointType, saved.getCheckpointId(), serviceId);
        return saved;
    }

    @Transactional
    public StateCheckpoint restoreFromCheckpoint(UUID checkpointId) {
        StateCheckpoint checkpoint = repository.findById(checkpointId)
                .orElseThrow(() -> new CheckpointNotFoundException(checkpointId));
        ServiceStateAdapter adapter = registry.getRequiredAdapter(checkpoint.getServiceId());
        adapter.restoreFromSnapshot(checkpoint.getStateSnapshot());
        log.info("Restored service {} from checkpoint {}", checkpoint.getServiceId(), checkpointId);
        return checkpoint;
    }

    @Transactional(readOnly = true)
    public Optional<StateCheckpoint> getLatestCheckpoint(String serviceId) {
        return repository.findFirstByServiceIdOrderByTimestampDesc(serviceId);
    }

    @Transactional(readOnly = true)
    public List<StateCheckpoint> getRecentCheckpoints(String serviceId, int limit) {
        int size = Math.max(1, limit);
        Pageable page = PageRequest.of(0, size);
        return repository.findByServiceIdOrderByTimestampDesc(serviceId, page);
    }

    @Transactional
    public void pruneOldCheckpoints(int retentionDays) {
        if (retentionDays <= 0) {
            return;
        }
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        repository.deleteByTimestampBefore(cutoff);
        log.debug("Pruned checkpoints older than {}", cutoff);
    }
}

