package prototype.lineageruntime.transaction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SagaCoordinator {

    private static final Logger log = LoggerFactory.getLogger(SagaCoordinator.class);

    private final RollbackManager rollbackManager;

    public SagaCoordinator(RollbackManager rollbackManager) {
        this.rollbackManager = rollbackManager;
    }

    public void runSaga(String sagaName, List<SagaStep> steps) {
        UUID transactionId = rollbackManager.beginTransaction();
        try {
            for (SagaStep step : steps) {
                step.action().call();
                rollbackManager.logOperation(transactionId, step.description(), step.compensatingAction());
            }
            rollbackManager.commit(transactionId);
            log.info("Saga '{}' completed successfully with transaction {}", sagaName, transactionId);
        } catch (Exception ex) {
            log.error("Saga '{}' failed, triggering rollback", sagaName, ex);
            rollbackManager.rollback(transactionId);
            throw new SagaExecutionException("Saga '" + sagaName + "' failed", ex);
        }
    }

    public record SagaStep(String description, Callable<Void> action, CompensatingAction compensatingAction) {
    }

    public static class SagaExecutionException extends RuntimeException {
        public SagaExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

