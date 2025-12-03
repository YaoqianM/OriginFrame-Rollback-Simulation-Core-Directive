package prototype.lineageruntime.health;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prototype.lineageruntime.resilience.CircuitBreakerGuard;
import prototype.lineageruntime.resilience.FaultIsolationEvent;
import prototype.lineageruntime.resilience.FaultIsolator;
import prototype.lineageruntime.resilience.RollbackManager;

@Service
public class HealthMonitorService {

    private static final Logger log = LoggerFactory.getLogger(HealthMonitorService.class);
    private static final String LATENCY = "LATENCY";
    private static final String ERROR_RATE = "ERROR_RATE";
    private static final String HEARTBEAT = "HEARTBEAT";

    private final HealthMetricsRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AnomalyDetector anomalyDetector;
    private final FaultIsolator faultIsolator;
    private final RollbackManager rollbackManager;
    private final Duration windowDuration;
    private final Map<MetricKey, Deque<MetricSample>> slidingWindows = new ConcurrentHashMap<>();

    public HealthMonitorService(
            HealthMetricsRepository repository,
            KafkaTemplate<String, Object> kafkaTemplate,
            AnomalyDetector anomalyDetector,
            FaultIsolator faultIsolator,
            RollbackManager rollbackManager,
            @Value("${health.monitor.window-duration:PT5M}") Duration windowDuration
    ) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.anomalyDetector = anomalyDetector;
        this.faultIsolator = faultIsolator;
        this.rollbackManager = rollbackManager;
        this.windowDuration = windowDuration;
    }

    public void recordLatency(String serviceId, double latencyMillis) {
        recordSample(serviceId, LATENCY, latencyMillis);
    }

    public void recordErrorRate(String serviceId, double errorRate) {
        recordSample(serviceId, ERROR_RATE, errorRate);
    }

    public void recordHeartbeat(String serviceId) {
        recordSample(serviceId, HEARTBEAT, 1.0);
    }

    private void recordSample(String serviceId, String metricType, double value) {
        Objects.requireNonNull(serviceId, "serviceId");
        MetricKey key = new MetricKey(serviceId, metricType);
        Deque<MetricSample> window = slidingWindows.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (window) {
            window.addLast(new MetricSample(Instant.now(), value));
            trimWindow(window);
        }
    }

    private void trimWindow(Deque<MetricSample> window) {
        Instant threshold = Instant.now().minus(windowDuration);
        while (!window.isEmpty() && window.peekFirst().timestamp().isBefore(threshold)) {
            window.removeFirst();
        }
    }

    @Transactional
    @CircuitBreakerGuard(serviceId = "health-monitor")
    @Scheduled(fixedDelayString = "${health.monitor.aggregate-interval:5000}")
    public void aggregateAndPublish() {
        slidingWindows.forEach((key, window) -> {
            var samples = snapshotWindow(window);
            MetricSample snapshot = snapshotAverage(samples);
            if (snapshot == null) {
                return;
            }

            HealthMetrics aggregated = new HealthMetrics(
                    key.serviceId,
                    key.metricType,
                    snapshot.value(),
                    snapshot.timestamp()
            );

            repository.save(aggregated);
            kafkaTemplate.send("health-events", key.serviceId, aggregated);

            Optional<HealthAlert> alert = anomalyDetector.detect(
                    key.serviceId,
                    key.metricType,
                    samples
            );
            alert.ifPresent(this::handleAnomaly);
        });
    }

    private void handleAnomaly(HealthAlert alert) {
        kafkaTemplate.send("health-alerts", alert.serviceId(), alert);
        try {
            FaultIsolationEvent event = faultIsolator.isolateService(alert.serviceId(), alert.description());
            rollbackManager.coordinateRollback(event);
        } catch (Exception ex) {
            log.error("Failed to isolate service {} after anomaly {}", alert.serviceId(), alert.description(), ex);
        }
    }

    @SuppressWarnings("SynchronizeOnNonFinalField")
    private java.util.List<MetricSample> snapshotWindow(Deque<MetricSample> window) {
        synchronized (window) {
            return java.util.List.copyOf(window);
        }
    }

    private MetricSample snapshotAverage(java.util.List<MetricSample> samples) {
        if (samples.isEmpty()) {
            return null;
        }
        double sum = 0;
        int count = 0;
        Instant latest = null;
        for (MetricSample sample : samples) {
            sum += sample.value();
            count++;
            latest = sample.timestamp();
        }
        if (count == 0 || latest == null) {
            return null;
        }
        return new MetricSample(latest, sum / count);
    }

    private record MetricKey(String serviceId, String metricType) {
    }

    public Optional<java.time.Instant> latestHeartbeat(String serviceId) {
        MetricKey key = new MetricKey(serviceId, HEARTBEAT);
        Deque<MetricSample> window = slidingWindows.get(key);
        if (window == null) {
            return Optional.empty();
        }

        synchronized (window) {
            MetricSample latest = window.peekLast();
            return latest == null ? Optional.empty() : Optional.of(latest.timestamp());
        }
    }
}

