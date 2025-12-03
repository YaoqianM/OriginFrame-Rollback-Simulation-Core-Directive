package prototype.simulationcore.timeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Component;
import prototype.simulationcore.metrics.SimulationRunRegistry;
import prototype.simulationcore.metrics.SimulationRunState;

@Component
public class TimelineRecorder {

    private final SimulationRunRegistry registry;
    private final ObjectMapper objectMapper;

    public TimelineRecorder(SimulationRunRegistry registry, ObjectMapper objectMapper) {
        this.registry = registry;
        this.objectMapper = objectMapper;
    }

    public void recordEvent(TimelineEvent event) {
        registry.stateFor(event.simulationId()).addTimelineEvent(event);
    }

    public List<TimelineEvent> getTimeline(String simulationId, int startTick, int endTick) {
        return registry.findState(simulationId)
                .map(state -> state.timelineBetween(normalizeStart(startTick), normalizeEnd(endTick)))
                .orElse(List.of());
    }

    public String exportToJson(String simulationId, int startTick, int endTick) {
        List<TimelineEvent> events = getTimeline(simulationId, startTick, endTick);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(events);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize timeline for " + simulationId, e);
        }
    }

    private int normalizeStart(int startTick) {
        return Math.max(0, startTick);
    }

    private int normalizeEnd(int endTick) {
        return endTick <= 0 ? Integer.MAX_VALUE : endTick;
    }
}

