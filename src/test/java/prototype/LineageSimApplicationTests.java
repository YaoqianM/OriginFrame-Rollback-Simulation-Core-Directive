package prototype;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import prototype.simulationcore.service.ReplayService;
import prototype.simulationcore.service.RollbackService;
import prototype.simulationcore.service.SimulationService;
import prototype.lineageruntime.recovery.DependencyHealer;
import prototype.lineageruntime.recovery.FailoverManager;
import prototype.lineageruntime.recovery.RecoveryWorkflowOrchestrator;
import prototype.lineageruntime.recovery.ServiceReconstructor;

class LineageSimApplicationTests extends ContainerizedSpringBootTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private SimulationService simulationService;

    @Autowired
    private RollbackService rollbackService;

    @Autowired
    private ReplayService replayService;

    @Autowired
    private RecoveryWorkflowOrchestrator recoveryWorkflowOrchestrator;

    @Autowired
    private ServiceReconstructor serviceReconstructor;

    @Autowired
    private DependencyHealer dependencyHealer;

    @Autowired
    private FailoverManager failoverManager;

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

    @Test
    void recoveryPipelineIsPresent() {
        assertThat(recoveryWorkflowOrchestrator).isNotNull();
        assertThat(serviceReconstructor).isNotNull();
        assertThat(dependencyHealer).isNotNull();
        assertThat(failoverManager).isNotNull();
    }
}

