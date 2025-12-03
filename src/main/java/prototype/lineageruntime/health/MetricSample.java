package prototype.lineageruntime.health;

import java.time.Instant;

record MetricSample(Instant timestamp, double value) {
}

