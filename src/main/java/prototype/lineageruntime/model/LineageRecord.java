package prototype.lineageruntime.model;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import prototype.simulationcore.domain.LineageEvent;

@Schema(name = "LineageRecord", description = "Projected lineage event exposed via the public API.")
public record LineageRecord(
        @Schema(description = "Event identifier emitted by Kafka.", example = "9f2e2ab0-4f0a-4a51-8e6c-a6f2c4d8b9bc")
        String eventId,
        @Schema(description = "Identifier of the agent or lineage this event belongs to.", example = "agent-1")
        String lineageId,
        @Schema(description = "Energy value prior to the transition.", example = "36.5")
        double previousEnergy,
        @Schema(description = "Energy value after the transition.", example = "37.5")
        double resultingEnergy,
        @Schema(description = "When the event was observed.", example = "2025-05-05T10:05:00Z")
        Instant timestamp
) {

    public static LineageRecord from(LineageEvent event) {
        return new LineageRecord(
                event.getEventId(),
                event.getAgentId(),
                event.getPreviousState().energy(),
                event.getResultingState().energy(),
                event.getTimestamp()
        );
    }
}

