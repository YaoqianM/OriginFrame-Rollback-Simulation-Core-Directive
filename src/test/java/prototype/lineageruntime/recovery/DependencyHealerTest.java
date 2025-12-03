package prototype.lineageruntime.recovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DependencyHealerTest {

    @Mock
    private ServiceReconstructor serviceReconstructor;

    @Mock
    private FailoverManager failoverManager;

    private ServiceTopology topology;

    private DependencyHealer dependencyHealer;

    @BeforeEach
    void setup() {
        RecoveryProperties properties = new RecoveryProperties();
        RecoveryProperties.ServiceDefinition storage = new RecoveryProperties.ServiceDefinition();
        storage.setId("storage");
        storage.setVersion("1.0.0");

        RecoveryProperties.ServiceDefinition api = new RecoveryProperties.ServiceDefinition();
        api.setId("api");
        api.setDependencies(List.of("storage"));
        api.setFallback("api-canary");

        RecoveryProperties.ServiceDefinition ui = new RecoveryProperties.ServiceDefinition();
        ui.setId("ui");
        ui.setDependencies(List.of("api"));

        properties.setServices(List.of(storage, api, ui));

        topology = new ServiceTopology(properties);
        dependencyHealer = new DependencyHealer(topology, serviceReconstructor, failoverManager);
    }

    @Test
    void analyzeImpactTraversesDependencyGraph() {
        List<DependencyImpact> impacts = dependencyHealer.analyzeImpact("storage");
        assertThat(impacts).extracting(DependencyImpact::serviceId).containsExactly("api", "ui");
    }

    @Test
    void healDependenciesUsesFallbackWhenAvailable() {
        when(failoverManager.hasFallback("api")).thenReturn(true);
        when(failoverManager.activateFallback("api"))
                .thenReturn(new FailoverAction("api", "api-canary", true, "activated",
                        topology.snapshot("api").withStatus(ServiceStatus.DEGRADED)));
        when(failoverManager.hasFallback("ui")).thenReturn(false);
        ServiceSnapshot uiSnapshot = topology.snapshot("ui").withStatus(ServiceStatus.RESTARTING);
        when(serviceReconstructor.restartService("ui"))
                .thenReturn(new ServiceRecoveryAction("ui", RecoveryActionType.RESTART, 1, true, "restarted", uiSnapshot));

        List<DependencyHealResult> results = dependencyHealer.healDependencies("storage");

        assertThat(results).hasSize(2);
        assertThat(results.get(0).strategy()).isEqualTo(RecoveryActionType.FAILOVER.name());
        assertThat(results.get(1).strategy()).isEqualTo(RecoveryActionType.RESTART.name());
        verify(serviceReconstructor).restartService("ui");
        verify(failoverManager).activateFallback("api");
    }
}


