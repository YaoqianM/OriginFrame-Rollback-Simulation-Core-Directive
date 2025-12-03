package prototype.lineageruntime.recovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import prototype.ContainerizedSpringBootTest;

class RecoveryWorkflowIntegrationTest extends ContainerizedSpringBootTest {

    @Autowired
    private RecoveryWorkflowOrchestrator orchestrator;

    @Autowired
    private ServiceTopology serviceTopology;

    @Test
    void orchestratorCoordinatesFullRollbackWorkflow() {
        RecoveryExecutionReport report = orchestrator.recover("ingestion-service");

        assertThat(report.success()).isTrue();
        assertThat(report.steps())
                .extracting(WorkflowStepResult::stage)
                .contains(WorkflowStage.DETECT, WorkflowStage.ISOLATE, WorkflowStage.ROLLBACK, WorkflowStage.RECOVER, WorkflowStage.VALIDATE);
        assertThat(report.impactedServices()).isNotEmpty();
        assertThat(report.dependencyActions()).isNotEmpty();
        assertThat(report.finalSnapshot().status().isHealthy()).isTrue();

        assertKafkaTopicsReceivedEvents();
    }

    private void assertKafkaTopicsReceivedEvents() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "recovery-workflow-it");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("recovery-events", "rollback-events"));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            assertThat(records.count()).isGreaterThan(0);
        }
    }
}

