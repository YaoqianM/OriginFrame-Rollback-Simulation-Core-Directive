package prototype.lineageruntime.checkpoint;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "runtime.checkpointing")
public class CheckpointProperties {

    private int retentionDays = 7;
    private Duration retentionSweepInterval = Duration.ofHours(1);
    private Critical critical = new Critical();
    private NonCritical nonCritical = new NonCritical();

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public Duration getRetentionSweepInterval() {
        return retentionSweepInterval;
    }

    public void setRetentionSweepInterval(Duration retentionSweepInterval) {
        this.retentionSweepInterval = retentionSweepInterval;
    }

    public Critical getCritical() {
        return critical;
    }

    public void setCritical(Critical critical) {
        this.critical = critical;
    }

    public NonCritical getNonCritical() {
        return nonCritical;
    }

    public void setNonCritical(NonCritical nonCritical) {
        this.nonCritical = nonCritical;
    }

    public static class Critical {

        private int operationInterval = 10;
        private Set<String> services = Collections.emptySet();

        public int getOperationInterval() {
            return operationInterval;
        }

        public void setOperationInterval(int operationInterval) {
            this.operationInterval = operationInterval;
        }

        public Set<String> getServices() {
            return services;
        }

        public void setServices(Set<String> services) {
            this.services = services == null ? Collections.emptySet() : new HashSet<>(services);
        }
    }

    public static class NonCritical {

        private Duration interval = Duration.ofMinutes(5);
        private Duration pollInterval = Duration.ofSeconds(30);
        private Set<String> services = Collections.emptySet();

        public Duration getInterval() {
            return interval;
        }

        public void setInterval(Duration interval) {
            this.interval = interval;
        }

        public Duration getPollInterval() {
            return pollInterval;
        }

        public void setPollInterval(Duration pollInterval) {
            this.pollInterval = pollInterval;
        }

        public Set<String> getServices() {
            return services;
        }

        public void setServices(Set<String> services) {
            this.services = services == null ? Collections.emptySet() : new HashSet<>(services);
        }
    }
}

