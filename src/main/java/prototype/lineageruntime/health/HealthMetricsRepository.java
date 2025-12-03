package prototype.lineageruntime.health;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthMetricsRepository extends JpaRepository<HealthMetrics, Long> {

    List<HealthMetrics> findByServiceIdAndMetricTypeAndTimestampBetween(
            String serviceId,
            String metricType,
            Instant start,
            Instant end
    );
}

