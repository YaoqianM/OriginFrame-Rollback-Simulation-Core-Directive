package prototype.simulationcore.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.environment.DefaultEnvironment;
import prototype.simulationcore.environment.Environment;
import prototype.simulationcore.persistence.AgentStateAttributeConverter;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.AgentPolicy;

@Entity
@Table(name = "agents")
public class Agent {

    @Id
    @GeneratedValue
    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "generation")
    private int generation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private AbstractAgentPolicy policy;

    @Lob
    @Column(name = "state_payload", nullable = false)
    @Convert(converter = AgentStateAttributeConverter.class)
    private AgentState state = AgentState.initial();

    @Column(name = "fitness")
    private double fitness;

    @Column(name = "safety_violations")
    private int safetyViolations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static Agent bootstrap(AbstractAgentPolicy policy) {
        Agent agent = new Agent();
        agent.setPolicy(policy);
        agent.state = AgentState.initial();
        agent.generation = 0;
        return agent;
    }

    public UUID getAgentId() {
        return agentId;
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

    public AgentPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(AbstractAgentPolicy policy) {
        this.policy = policy;
    }

    public AgentState getState() {
        return state;
    }

    public void setState(AgentState state) {
        this.state = state == null ? AgentState.initial() : state;
    }

    public AgentState replaceState(AgentState newState) {
        setState(newState);
        return this.state;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }

    public int getSafetyViolations() {
        return safetyViolations;
    }

    public void recordSafetyViolation() {
        safetyViolations++;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AgentState snapshotState() {
        return state.snapshot();
    }

    public Action decide(Environment environment) {
        Environment resolved = environment == null ? new DefaultEnvironment(state) : environment;
        if (policy == null) {
            return Action.WAIT;
        }
        return policy.decide(state, resolved);
    }

    public void adjustFitness(double delta) {
        fitness += delta;
    }

    public void incrementGeneration() {
        generation++;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}

