package prototype.lineageruntime.health;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.DoubleSummaryStatistics;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class AnomalyDetector {

    private static final double DEFAULT_Z_SCORE_THRESHOLD = 2.5;
    private static final Duration HEARTBEAT_TOLERANCE = Duration.ofSeconds(30);

    public Optional<HealthAlert> detect(String serviceId, String metricType, Collection<MetricSample> samples) {
        if (samples == null || samples.isEmpty()) {
            return Optional.empty();
        }

        return switch (metricType.toUpperCase()) {
            case "LATENCY" -> detectZScore("High latency spike", serviceId, metricType, samples);
            case "ERROR_RATE" -> detectZScore("Error rate spike", serviceId, metricType, samples);
            case "HEARTBEAT" -> detectHeartbeat(serviceId, metricType, samples);
            default -> Optional.empty();
        };
    }

    private Optional<HealthAlert> detectZScore(
            String description,
            String serviceId,
            String metricType,
            Collection<MetricSample> samples
    ) {
        if (samples.size() < 3) {
            return Optional.empty();
        }

        DoubleSummaryStatistics stats = samples.stream()
                .mapToDouble(MetricSample::value)
                .summaryStatistics();

        double mean = stats.getAverage();
        double variance = samples.stream()
                .mapToDouble(sample -> Math.pow(sample.value() - mean, 2))
                .sum() / samples.size();
        double stdDev = Math.sqrt(variance);
        if (stdDev == 0) {
            return Optional.empty();
        }

        double latest = samples.stream()
                .reduce((first, second) -> second)
                .map(MetricSample::value)
                .orElse(mean);
        double zScore = Math.abs((latest - mean) / stdDev);

        if (zScore >= DEFAULT_Z_SCORE_THRESHOLD) {
            return Optional.of(new HealthAlert(
                    serviceId,
                    metricType,
                    latest,
                    Instant.now(),
                    description + " detected (z=" + String.format("%.2f", zScore) + ")"
            ));
        }
        return Optional.empty();
    }

    private Optional<HealthAlert> detectHeartbeat(
            String serviceId,
            String metricType,
            Collection<MetricSample> samples
    ) {
        Instant now = Instant.now();
        Instant last = samples.stream()
                .reduce((first, second) -> second)
                .map(MetricSample::timestamp)
                .orElse(null);

        if (last == null || Duration.between(last, now).compareTo(HEARTBEAT_TOLERANCE) > 0) {
            return Optional.of(new HealthAlert(
                    serviceId,
                    metricType,
                    0,
                    now,
                    "Missed heartbeat detected"
            ));
        }
        return Optional.empty();
    }
}

