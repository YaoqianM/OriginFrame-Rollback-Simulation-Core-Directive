package prototype.lineageruntime.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ServiceTopologyTest {

    @Test
    void registersDependenciesAndFallbacksFromProperties() {
        RecoveryProperties properties = new RecoveryProperties();
        RecoveryProperties.ServiceDefinition ingestion = new RecoveryProperties.ServiceDefinition();
        ingestion.setId("ingestion");
        ingestion.setVersion("1.0.0");
        ingestion.setDependencies(List.of("storage"));
        ingestion.setFallback("ingestion-canary");

        RecoveryProperties.ServiceDefinition storage = new RecoveryProperties.ServiceDefinition();
        storage.setId("storage");
        storage.setVersion("2.0.0");
        storage.setDependencies(List.of());

        properties.setServices(List.of(ingestion, storage));

        ServiceTopology topology = new ServiceTopology(properties);

        ServiceSnapshot ingestionSnapshot = topology.snapshot("ingestion");
        assertThat(ingestionSnapshot.version()).isEqualTo("1.0.0");
        assertThat(topology.dependenciesOf("ingestion")).containsExactly("storage");
        assertThat(topology.dependentsOf("storage")).containsExactly("ingestion");
        assertThat(topology.fallbackOf("ingestion")).contains("ingestion-canary");
    }

    @Test
    void updateInstanceChangesStatusAndInstanceId() {
        RecoveryProperties properties = new RecoveryProperties();
        properties.setServices(List.of());
        ServiceTopology topology = new ServiceTopology(properties);

        ServiceSnapshot snapshot = topology.updateInstance("analytics", "analytics-1", "3.0.0", ServiceStatus.RESTARTING);
        assertThat(snapshot.instanceId()).isEqualTo("analytics-1");
        assertThat(snapshot.status()).isEqualTo(ServiceStatus.RESTARTING);

        ServiceSnapshot healthy = topology.updateStatus("analytics", ServiceStatus.HEALTHY);
        assertThat(healthy.status()).isEqualTo(ServiceStatus.HEALTHY);
        assertThat(healthy.instanceId()).isEqualTo("analytics-1");
    }
}


