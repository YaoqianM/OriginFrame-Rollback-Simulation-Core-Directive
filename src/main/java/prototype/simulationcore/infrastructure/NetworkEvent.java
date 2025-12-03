package prototype.simulationcore.infrastructure;

import java.time.Instant;
import java.util.UUID;

/**
 * Captures network-level events when routing simulated traffic.
 */
public record NetworkEvent(long tick,
                           Instant occurredAt,
                           UUID source,
                           UUID destination,
                           String type,
                           String detail,
                           double latencyMs,
                           boolean delivered) {
}


