package prototype.simulationcore.safety.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import prototype.simulationcore.service.RollbackService;

@Component
public class CriticalViolationRollbackListener {

    private static final Logger log = LoggerFactory.getLogger(CriticalViolationRollbackListener.class);

    private final RollbackService rollbackService;

    public CriticalViolationRollbackListener(RollbackService rollbackService) {
        this.rollbackService = rollbackService;
    }

    @EventListener
    public void onCriticalViolation(CriticalSafetyViolationEvent event) {
        log.warn("Critical safety violation detected for agent {}. Triggering rollback.",
                event.agentId());
        rollbackService.rollback();
    }
}

