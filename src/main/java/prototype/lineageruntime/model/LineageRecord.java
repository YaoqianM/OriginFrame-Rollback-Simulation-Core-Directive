package prototype.lineageruntime.model;

import java.time.Instant;
import prototype.simulationcore.domain.LineageEvent;

public record LineageRecord(
        String eventId,
        String lineageId,
        int previousState,
        int resultingState,
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

