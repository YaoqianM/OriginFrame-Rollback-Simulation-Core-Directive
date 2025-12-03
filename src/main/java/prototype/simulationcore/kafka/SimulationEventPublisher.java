package prototype.simulationcore.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import prototype.simulationcore.events.SimulationEvent;

@Component
public class SimulationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(SimulationEventPublisher.class);

    private final KafkaTemplate<String, SimulationEvent> kafkaTemplate;
    private final String topic;

    public SimulationEventPublisher(KafkaTemplate<String, SimulationEvent> kafkaTemplate,
                                    @Value("${simulation.events.topic:simulation-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(SimulationEvent event) {
        kafkaTemplate.send(topic, event.getSimulationId().toString(), event);
        log.debug("Published simulation event {} [{}] to {}", event.getEventId(), event.getType(), topic);
    }
}

