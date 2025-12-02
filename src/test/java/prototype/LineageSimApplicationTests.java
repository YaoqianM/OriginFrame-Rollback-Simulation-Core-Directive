package prototype;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import prototype.simulationcore.service.ReplayService;
import prototype.simulationcore.service.RollbackService;
import prototype.simulationcore.service.SimulationService;

@SpringBootTest
class LineageSimApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private RollbackService rollbackService;

    @Autowired
    private ReplayService replayService;

    @Test
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }

    @Test
    void coreServicesArePresent() {
        assertThat(simulationService).isNotNull();
        assertThat(rollbackService).isNotNull();
        assertThat(replayService).isNotNull();
    }
}

