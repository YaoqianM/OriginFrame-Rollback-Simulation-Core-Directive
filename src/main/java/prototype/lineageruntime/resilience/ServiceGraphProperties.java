package prototype.lineageruntime.resilience;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resilience.service-graph")
public class ServiceGraphProperties {

    private Map<String, ServiceDependencies> services = new HashMap<>();

    public Map<String, ServiceDependencies> getServices() {
        return services;
    }

    public void setServices(Map<String, ServiceDependencies> services) {
        this.services = services != null ? services : new HashMap<>();
    }

    public static class ServiceDependencies {
        private List<String> dependencies = new ArrayList<>();

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies != null ? dependencies : new ArrayList<>();
        }
    }
}

