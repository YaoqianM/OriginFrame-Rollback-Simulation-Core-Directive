package prototype;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import prototype.integration.config.GridDemoProperties;
import prototype.lineageruntime.checkpoint.CheckpointProperties;
import prototype.lineageruntime.recovery.RecoveryProperties;
import prototype.simulationcore.safety.SafetyProperties;
import prototype.simulationcore.world.config.WorldConfig;

@EnableScheduling
@EnableConfigurationProperties({
        CheckpointProperties.class,
        RecoveryProperties.class,
        WorldConfig.class,
        GridDemoProperties.class,
        SafetyProperties.class
})
@SpringBootApplication
public class LineageSimApplication {

    public static void main(String[] args) {
        SpringApplication.run(LineageSimApplication.class, args);
    }
}

