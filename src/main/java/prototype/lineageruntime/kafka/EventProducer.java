package prototype.lineageruntime.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import prototype.simulationcore.domain.LineageEvent;

@Component
public class EventProducer {

    private static final String TOPIC = "lineage-events";

    private final KafkaTemplate<String, LineageEvent> kafkaTemplate;

    public EventProducer(KafkaTemplate<String, LineageEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(LineageEvent event) {
        kafkaTemplate.send(TOPIC, event.getAgentId(), event);
    }
}

