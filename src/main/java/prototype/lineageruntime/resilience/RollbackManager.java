package prototype.lineageruntime.resilience;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import prototype.simulationcore.service.RollbackService;

@Component
public class RollbackManager {

    private static final Logger log = LoggerFactory.getLogger(RollbackManager.class);

    private final RollbackService rollbackService;
    private final Set<String> activeRollbacks = ConcurrentHashMap.newKeySet();

    public RollbackManager(RollbackService rollbackService) {
        this.rollbackService = rollbackService;
    }

    public void coordinateRollback(FaultIsolationEvent event) {
        if (event.cascadePrevented()) {
            log.debug("Skipping rollback for {} because isolation was suppressed", event.serviceId());
            return;
        }
        if (!activeRollbacks.add(event.serviceId())) {
            log.debug("Rollback already active for {}", event.serviceId());
            return;
        }

        try {
            rollbackService.rollback();
        } finally {
            activeRollbacks.remove(event.serviceId());
        }
    }
}

