package prototype.lineageruntime.lineage.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import prototype.lineageruntime.lineage.support.JsonMapConverter;

@Entity
@Table(name = "agent_lineages")
public class AgentLineage {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Column(name = "lineage_id", nullable = false)
    private UUID lineageId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "generation")
    private int generation;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "agent_lineage_mutations", joinColumns = @JoinColumn(name = "agent_id"))
    private List<MutationEvent> mutationsApplied = new ArrayList<>();

    @Column(name = "performance_score")
    private double performanceScore;

    @Column(name = "safety_score")
    private double safetyScore;

    @Column(name = "survived_generations")
    private int survivedGenerations;

    @Column(name = "elimination_reason")
    private String eliminationReason;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AgentLineage() {
        // for JPA
    }

    private AgentLineage(UUID lineageId, UUID agentId, UUID parentId, int generation) {
        this.lineageId = Objects.requireNonNull(lineageId, "lineageId");
        this.agentId = Objects.requireNonNull(agentId, "agentId");
        this.parentId = parentId;
        this.generation = generation;
    }

    public static AgentLineage create(UUID lineageId, UUID agentId, UUID parentId, int generation) {
        return new AgentLineage(lineageId, agentId, parentId, generation);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getLineageId() {
        return lineageId;
    }

    public UUID getParentId() {
        return parentId;
    }

    public void setParentId(UUID parentId) {
        this.parentId = parentId;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public List<MutationEvent> getMutationsApplied() {
        return mutationsApplied;
    }

    public void setMutationsApplied(List<MutationEvent> mutationsApplied) {
        this.mutationsApplied = new ArrayList<>(mutationsApplied == null ? List.of() : mutationsApplied);
    }

    public void addMutation(MutationEvent event) {
        if (event != null) {
            this.mutationsApplied.add(event);
        }
    }

    public double getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(double performanceScore) {
        this.performanceScore = performanceScore;
    }

    public double getSafetyScore() {
        return safetyScore;
    }

    public void setSafetyScore(double safetyScore) {
        this.safetyScore = safetyScore;
    }

    public int getSurvivedGenerations() {
        return survivedGenerations;
    }

    public void setSurvivedGenerations(int survivedGenerations) {
        this.survivedGenerations = survivedGenerations;
    }

    public String getEliminationReason() {
        return eliminationReason;
    }

    public void setEliminationReason(String eliminationReason) {
        this.eliminationReason = eliminationReason;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = new HashMap<>(metadata == null ? Map.of() : metadata);
    }

    public void mergeMetadata(Map<String, Object> additional) {
        if (additional == null || additional.isEmpty()) {
            return;
        }
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        metadata.putAll(additional);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isEliminated() {
        return eliminationReason != null && !eliminationReason.isBlank();
    }
}

