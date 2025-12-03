package prototype.lineageruntime.transaction;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaction_logs")
public class TransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "transaction_operations", joinColumns = @JoinColumn(name = "transaction_log_id"))
    @Column(name = "operation_description")
    private List<String> operationSequence = new ArrayList<>();

    @Column(name = "compensating_actions", columnDefinition = "TEXT")
    @jakarta.persistence.Convert(converter = StringListConverter.class)
    private List<String> compensatingActions = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private Long version;

    protected TransactionLog() {
    }

    public TransactionLog(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public List<String> getOperationSequence() {
        return operationSequence;
    }

    public List<String> getCompensatingActions() {
        return compensatingActions;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void appendOperation(String operation) {
        operationSequence.add(operation);
    }

    public void appendCompensatingAction(String actionDescription) {
        compensatingActions.add(actionDescription);
    }

    public void markCommitted() {
        this.status = TransactionStatus.COMMITTED;
        this.completedAt = Instant.now();
    }

    public void markRolledBack() {
        this.status = TransactionStatus.ROLLED_BACK;
        this.completedAt = Instant.now();
    }
}

