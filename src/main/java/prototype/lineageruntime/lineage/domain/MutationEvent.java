package prototype.lineageruntime.lineage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MutationEvent implements Serializable {

    @Column(name = "mutation_id", nullable = false, updatable = false)
    private UUID mutationId;

    @Column(name = "mutation_type", length = 128, nullable = false)
    private String type;

    @Column(name = "mutation_description", length = 512)
    private String description;

    @Column(name = "performance_delta")
    private double performanceDelta;

    @Column(name = "safety_delta")
    private double safetyDelta;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "applied_by", length = 128)
    private String appliedBy;

    @Column(name = "violation_linked")
    private boolean violationLinked;

    protected MutationEvent() {
        // for JPA
    }

    private MutationEvent(UUID mutationId,
                          String type,
                          String description,
                          double performanceDelta,
                          double safetyDelta,
                          Instant appliedAt,
                          String appliedBy,
                          boolean violationLinked) {
        this.mutationId = mutationId;
        this.type = type;
        this.description = description;
        this.performanceDelta = performanceDelta;
        this.safetyDelta = safetyDelta;
        this.appliedAt = appliedAt;
        this.appliedBy = appliedBy;
        this.violationLinked = violationLinked;
    }

    public static MutationEvent of(String type,
                                   String description,
                                   double performanceDelta,
                                   double safetyDelta,
                                   String appliedBy,
                                   boolean violationLinked) {
        Objects.requireNonNull(type, "type");
        return new MutationEvent(
                UUID.randomUUID(),
                type,
                description,
                performanceDelta,
                safetyDelta,
                Instant.now(),
                appliedBy,
                violationLinked
        );
    }

    public UUID getMutationId() {
        return mutationId;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public double getPerformanceDelta() {
        return performanceDelta;
    }

    public double getSafetyDelta() {
        return safetyDelta;
    }

    public Instant getAppliedAt() {
        return appliedAt;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public boolean isViolationLinked() {
        return violationLinked;
    }
}

