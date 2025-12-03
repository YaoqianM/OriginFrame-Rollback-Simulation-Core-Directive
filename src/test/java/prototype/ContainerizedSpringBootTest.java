package prototype;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ContainerizedSpringBootTest {

    private static final MySQLContainer<?> MYSQL;
    private static final KafkaContainer KAFKA;
    private static final boolean DOCKER_AVAILABLE;
    private static final String FALLBACK_KAFKA = "PLAINTEXT://localhost:9092";

    static {
        MySQLContainer<?> mysql = null;
        KafkaContainer kafka = null;
        boolean available = false;
        try {
            mysql = new MySQLContainer<>("mysql:8.0.36")
                    .withDatabaseName("lineage_test")
                    .withUsername("test")
                    .withPassword("test");
            kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
            mysql.start();
            kafka.start();
            available = true;
        } catch (Throwable throwable) {
            available = false;
            if (mysql != null) {
                mysql.close();
            }
            if (kafka != null) {
                kafka.close();
            }
        }
        MYSQL = mysql;
        KAFKA = kafka;
        DOCKER_AVAILABLE = available;
    }

    @BeforeAll
    static void requireDocker() {
        Assumptions.assumeTrue(DOCKER_AVAILABLE, "Docker is required for containerized tests");
    }

    @DynamicPropertySource
    static void registerDatasourceAndKafka(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
            registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        } else {
            registry.add("spring.datasource.url",
                    () -> "jdbc:h2:mem:lineage_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
            registry.add("spring.datasource.username", () -> "sa");
            registry.add("spring.datasource.password", () -> "");
            registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
            registry.add("spring.kafka.bootstrap-servers", () -> FALLBACK_KAFKA);
        }
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    protected static String kafkaBootstrapServers() {
        if (DOCKER_AVAILABLE && KAFKA != null) {
            return KAFKA.getBootstrapServers();
        }
        return FALLBACK_KAFKA;
    }
}

