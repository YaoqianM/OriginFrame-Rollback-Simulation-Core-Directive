package prototype.lineageruntime.kafka;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import prototype.lineageruntime.model.LineageRecord;
import prototype.simulationcore.domain.LineageEvent;

@Component
public class EventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EventConsumer.class);

    private final List<LineageEvent> history = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "lineage-events", groupId = "lineage-sim")
    public void consume(LineageEvent event) {
        history.add(event);
        log.info("Consumed lineage event {} prev={} next={}", event.getEventId(),
                event.getPreviousState(), event.getResultingState());
    }

    public List<LineageEvent> getHistory() {
        return List.copyOf(history);
    }

    public List<LineageRecord> getHistoryView() {
        return history.stream()
                .map(LineageRecord::from)
                .toList();
    }
}

