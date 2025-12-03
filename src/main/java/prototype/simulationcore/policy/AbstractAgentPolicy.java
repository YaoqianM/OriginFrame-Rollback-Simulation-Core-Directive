package prototype.simulationcore.policy;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Base JPA entity for all policy implementations.
 */
@Entity
@Table(name = "agent_policies")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "policy_type")
public abstract class AbstractAgentPolicy implements AgentPolicy {

    @Id
    @GeneratedValue
    @Column(name = "policy_id", nullable = false, updatable = false)
    private UUID policyId;

    @ElementCollection
    @CollectionTable(name = "agent_policy_parameters", joinColumns = @JoinColumn(name = "policy_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private Map<String, Double> parameters = new HashMap<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public UUID getPolicyId() {
        return policyId;
    }

    @Override
    public Map<String, Double> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public void setParameters(Map<String, Double> parameters) {
        this.parameters.clear();
        if (parameters != null) {
            this.parameters.putAll(parameters);
        }
    }

    protected double parameterOrDefault(String key, double defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    @PrePersist
    void stampCreated() {
        createdAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}


