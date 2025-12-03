package prototype.lineageruntime.transaction;

import java.util.Deque;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("transactionRollbackManager")
public class RollbackManager {

    private static final Logger log = LoggerFactory.getLogger(RollbackManager.class);

    private final TransactionLogRepository repository;
    private final ConcurrentMap<UUID, Deque<CompensatingAction>> actionRegistry = new ConcurrentHashMap<>();

    public RollbackManager(TransactionLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public UUID beginTransaction() {
        UUID transactionId = UUID.randomUUID();
        repository.save(new TransactionLog(transactionId));
        actionRegistry.put(transactionId, new ConcurrentLinkedDeque<>());
        log.info("Started transaction {}", transactionId);
        return transactionId;
    }

    @Transactional
    public void logOperation(UUID transactionId, String operation, CompensatingAction action) {
        TransactionLog logEntry = getLog(transactionId);
        logEntry.appendOperation(operation);
        if (action != null) {
            logEntry.appendCompensatingAction(action.description());
            actionRegistry.computeIfAbsent(transactionId, id -> new ConcurrentLinkedDeque<>()).add(action);
        }
        repository.save(logEntry);
        log.debug("Logged operation '{}' for transaction {}", operation, transactionId);
    }

    @Transactional
    public void commit(UUID transactionId) {
        TransactionLog logEntry = getLog(transactionId);
        logEntry.markCommitted();
        repository.save(logEntry);
        actionRegistry.remove(transactionId);
        log.info("Committed transaction {}", transactionId);
    }

    @Transactional
    public void rollback(UUID transactionId) {
        TransactionLog logEntry = getLog(transactionId);
        Deque<CompensatingAction> actions = actionRegistry.get(transactionId);
        if (actions != null) {
            while (!actions.isEmpty()) {
                CompensatingAction action = actions.removeLast();
                try {
                    action.execute();
                } catch (Exception ex) {
                    log.error("Compensating action '{}' failed for transaction {}", action.description(), transactionId, ex);
                }
            }
        }
        logEntry.markRolledBack();
        repository.save(logEntry);
        actionRegistry.remove(transactionId);
        log.warn("Rolled back transaction {}", transactionId);
    }

    public Optional<TransactionLog> find(UUID transactionId) {
        return repository.findByTransactionId(transactionId);
    }

    private TransactionLog getLog(UUID transactionId) {
        return repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown transaction " + transactionId));
    }
}

