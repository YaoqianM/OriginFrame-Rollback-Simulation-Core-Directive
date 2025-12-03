package prototype.lineageruntime.recovery;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "recovery")
public class RecoveryProperties {

    /**
     * Describes the logical services that Project A should protect. Populated from configuration.
     */
    private List<ServiceDefinition> services = new ArrayList<>();

    private final BackoffProperties backoff = new BackoffProperties();

    private final HealthProbeProperties health = new HealthProbeProperties();

    public List<ServiceDefinition> getServices() {
        return Collections.unmodifiableList(services);
    }

    public void setServices(List<ServiceDefinition> services) {
        this.services = services == null ? new ArrayList<>() : new ArrayList<>(services);
    }

    public BackoffProperties getBackoff() {
        return backoff;
    }

    public HealthProbeProperties getHealth() {
        return health;
    }

    public static class ServiceDefinition {

        private String id;
        private String version = "1.0.0";
        private List<String> dependencies = List.of();
        private String fallback;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        }

        public String getFallback() {
            return fallback;
        }

        public void setFallback(String fallback) {
            this.fallback = fallback;
        }
    }

    public static class BackoffProperties {

        private Duration initial = Duration.ofSeconds(1);
        private Duration max = Duration.ofSeconds(30);
        private int attempts = 5;

        public Duration getInitial() {
            return initial;
        }

        public void setInitial(Duration initial) {
            this.initial = initial;
        }

        public Duration getMax() {
            return max;
        }

        public void setMax(Duration max) {
            this.max = max;
        }

        public int getAttempts() {
            return attempts;
        }

        public void setAttempts(int attempts) {
            this.attempts = attempts;
        }
    }

    public static class HealthProbeProperties {

        private Duration maxStaleness = Duration.ofSeconds(15);

        public Duration getMaxStaleness() {
            return maxStaleness;
        }

        public void setMaxStaleness(Duration maxStaleness) {
            this.maxStaleness = maxStaleness;
        }
    }
}


