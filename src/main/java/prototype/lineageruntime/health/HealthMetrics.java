package prototype.lineageruntime.health;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "health_metrics", indexes = {
        @Index(name = "idx_health_metrics_service_type_time", columnList = "service_id, metric_type, timestamp")
})
public class HealthMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "service_id", nullable = false)
    private String serviceId;

    @Column(name = "metric_type", nullable = false)
    private String metricType;

    @Column(name = "metric_value", nullable = false)
    private double value;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    protected HealthMetrics() {
    }

    public HealthMetrics(String serviceId, String metricType, double value, Instant timestamp) {
        this.serviceId = Objects.requireNonNull(serviceId, "serviceId");
        this.metricType = Objects.requireNonNull(metricType, "metricType");
        this.value = value;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public Long getId() {
        return id;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getMetricType() {
        return metricType;
    }

    public double getValue() {
        return value;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}

