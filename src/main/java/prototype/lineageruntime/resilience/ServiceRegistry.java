package prototype.lineageruntime.resilience;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class ServiceRegistry {

    private final ConcurrentHashMap<String, ServiceNode> services = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> reverseDependencies = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<String>> isolatedDependencies = new ConcurrentHashMap<>();
    private final ServiceGraphProperties serviceGraphProperties;

    public ServiceRegistry(ServiceGraphProperties serviceGraphProperties) {
        this.serviceGraphProperties = serviceGraphProperties;
    }

    @PostConstruct
    public void bootstrapFromProperties() {
        Map<String, ServiceGraphProperties.ServiceDependencies> configured = serviceGraphProperties.getServices();
        configured.forEach((serviceId, deps) -> registerService(serviceId, deps.getDependencies()));
    }

    public void registerService(String serviceId, Collection<String> dependencies) {
        Objects.requireNonNull(serviceId, "serviceId");
        Set<String> dependencySet = dependencies == null
                ? Set.of()
                : Set.copyOf(dependencies);

        ServiceNode previous = services.put(serviceId, new ServiceNode(serviceId, dependencySet));
        if (previous != null) {
            previous.dependencies().forEach(dep -> reverseDependencies.computeIfPresent(dep, (key, dependents) -> {
                dependents.remove(serviceId);
                return dependents.isEmpty() ? null : dependents;
            }));
        }

        dependencySet.forEach(dependency -> reverseDependencies
                .computeIfAbsent(dependency, key -> ConcurrentHashMap.newKeySet())
                .add(serviceId)
        );
    }

    public void deregisterService(String serviceId) {
        ServiceNode removed = services.remove(serviceId);
        if (removed != null) {
            removed.dependencies().forEach(dep -> reverseDependencies.computeIfPresent(dep, (key, dependents) -> {
                dependents.remove(serviceId);
                return dependents.isEmpty() ? null : dependents;
            }));
        }
        reverseDependencies.values().forEach(dependents -> dependents.remove(serviceId));
        isolatedDependencies.remove(serviceId);
    }

    public Set<String> getDependencies(String serviceId) {
        ServiceNode node = services.get(serviceId);
        if (node == null) {
            return Set.of();
        }
        return node.dependencies();
    }

    public Set<String> getDependents(String serviceId) {
        Set<String> dependents = reverseDependencies.get(serviceId);
        if (dependents == null) {
            return Set.of();
        }
        return Collections.unmodifiableSet(dependents);
    }

    public boolean isDependencyIsolated(String serviceId, String dependencyId) {
        return isolatedDependencies.getOrDefault(serviceId, Collections.emptySet()).contains(dependencyId);
    }

    public void markDependencyIsolated(String serviceId, String dependencyId) {
        isolatedDependencies
                .computeIfAbsent(serviceId, key -> ConcurrentHashMap.newKeySet())
                .add(dependencyId);
    }

    public Set<String> registeredServices() {
        return Collections.unmodifiableSet(services.keySet());
    }

    public boolean hasService(String serviceId) {
        return services.containsKey(serviceId);
    }

    private record ServiceNode(String serviceId, Set<String> dependencies) {
    }
}

