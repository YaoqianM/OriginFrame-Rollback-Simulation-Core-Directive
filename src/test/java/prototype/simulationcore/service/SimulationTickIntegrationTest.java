package prototype.simulationcore.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import prototype.ContainerizedSpringBootTest;
import prototype.lineageruntime.kafka.EventConsumer;

class SimulationTickIntegrationTest extends ContainerizedSpringBootTest {

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private EventConsumer eventConsumer;

    @Test
    void simulationStepEmitsLineageEvents() {
        int ticks = 3;
        for (int i = 0; i < ticks; i++) {
            simulationService.step();
        }

        Awaitility.await().atMost(Duration.ofSeconds(10))
                .until(() -> eventConsumer.getHistory().size() >= ticks);

        assertThat(eventConsumer.getHistory()).hasSizeGreaterThanOrEqualTo(ticks);
    }
}

