package prototype.lineageruntime.health;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnomalyDetectorTest {

    private AnomalyDetector detector;

    @BeforeEach
    void setUp() {
        detector = new AnomalyDetector();
    }

    @Test
    void detectsHighLatencyAnomalyWhenZScoreExceedsThreshold() {
        List<MetricSample> samples = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 7; i >= 1; i--) {
            samples.add(new MetricSample(now.minusSeconds(i * 5L), 50));
        }
        samples.add(new MetricSample(now, 250));

        Optional<HealthAlert> alert = detector.detect("sim-core", "LATENCY", samples);

        assertThat(alert).isPresent();
        assertThat(alert.get().description()).contains("High latency spike");
    }

    @Test
    void detectsErrorRateSpike() {
        List<MetricSample> samples = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 6; i >= 1; i--) {
            samples.add(new MetricSample(now.minusSeconds(i * 5L), 0.01));
        }
        samples.add(new MetricSample(now, 0.5));

        Optional<HealthAlert> alert = detector.detect("sim-core", "ERROR_RATE", samples);

        assertThat(alert).isPresent();
        assertThat(alert.get().description()).contains("Error rate spike");
    }

    @Test
    void detectsMissedHeartbeatWhenLastSampleIsStale() {
        List<MetricSample> samples = List.of(
                new MetricSample(Instant.now().minusSeconds(120), 1.0)
        );

        Optional<HealthAlert> alert = detector.detect("sim-core", "HEARTBEAT", samples);

        assertThat(alert).isPresent();
        assertThat(alert.get().description()).contains("Missed heartbeat");
    }

    @Test
    void noAlertWhenSamplesAreStable() {
        List<MetricSample> samples = List.of(
                new MetricSample(Instant.now().minusSeconds(6), 50),
                new MetricSample(Instant.now().minusSeconds(4), 52),
                new MetricSample(Instant.now().minusSeconds(2), 51)
        );

        Optional<HealthAlert> alert = detector.detect("sim-core", "LATENCY", samples);

        assertThat(alert).isEmpty();
    }

    @Test
    void ignoresInsufficientSamplesForZScoreComputation() {
        List<MetricSample> samples = List.of(
                new MetricSample(Instant.now().minusSeconds(4), 50),
                new MetricSample(Instant.now().minusSeconds(2), 200)
        );

        Optional<HealthAlert> alert = detector.detect("sim-core", "LATENCY", samples);

        assertThat(alert).isEmpty();
    }

    @Test
    void zeroVarianceSamplesDoNotTriggerAlerts() {
        List<MetricSample> samples = List.of(
                new MetricSample(Instant.now().minusSeconds(6), 42),
                new MetricSample(Instant.now().minusSeconds(4), 42),
                new MetricSample(Instant.now().minusSeconds(2), 42)
        );

        Optional<HealthAlert> alert = detector.detect("sim-core", "ERROR_RATE", samples);

        assertThat(alert).isEmpty();
    }

    @Test
    void unknownMetricTypesAreIgnored() {
        List<MetricSample> samples = List.of(
                new MetricSample(Instant.now().minusSeconds(6), 1),
                new MetricSample(Instant.now().minusSeconds(4), 2),
                new MetricSample(Instant.now().minusSeconds(2), 3)
        );

        Optional<HealthAlert> alert = detector.detect("sim-core", "cpu", samples);

        assertThat(alert).isEmpty();
    }
}

