package prototype.lineageruntime.recovery;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceTopology {

    private static final Logger log = LoggerFactory.getLogger(ServiceTopology.class);

    private final Map<String, ServiceSnapshot> states = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> dependencies = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> dependents = new ConcurrentHashMap<>();
    private final Map<String, String> fallbackRegistry = new ConcurrentHashMap<>();

    public ServiceTopology(RecoveryProperties properties) {
        properties.getServices().forEach(this::registerServiceDefinition);
    }

    public ServiceSnapshot snapshot(String serviceId) {
        return states.computeIfAbsent(serviceId, this::unknownSnapshot);
    }

    public ServiceSnapshot update(String serviceId, java.util.function.UnaryOperator<ServiceSnapshot> updater) {
        return states.compute(serviceId, (id, current) -> {
            ServiceSnapshot base = current == null ? unknownSnapshot(id) : current;
            ServiceSnapshot updated = updater.apply(base);
            log.debug("Service {} transitioned {} -> {}", id, base.status(), updated.status());
            return updated;
        });
    }

    public ServiceSnapshot updateStatus(String serviceId, ServiceStatus status) {
        return update(serviceId, snapshot -> snapshot.withStatus(status));
    }

    public ServiceSnapshot updateInstance(String serviceId, String instanceId, String version, ServiceStatus status) {
        return update(serviceId, snapshot -> snapshot.withInstance(instanceId, version, status));
    }

    public void registerDependencies(String serviceId, Collection<String> dependencyIds) {
        Set<String> serviceDeps = dependencies.computeIfAbsent(serviceId, ignored -> ConcurrentHashMap.newKeySet());
        serviceDeps.clear();
        if (dependencyIds != null) {
            serviceDeps.addAll(dependencyIds);
            dependencyIds.forEach(dep -> dependents
                    .computeIfAbsent(dep, ignored -> ConcurrentHashMap.newKeySet())
                    .add(serviceId));
        }
    }

    public Set<String> dependenciesOf(String serviceId) {
        return dependencies.containsKey(serviceId)
                ? Set.copyOf(dependencies.get(serviceId))
                : Set.of();
    }

    public Set<String> dependentsOf(String serviceId) {
        return dependents.containsKey(serviceId)
                ? Set.copyOf(dependents.get(serviceId))
                : Set.of();
    }

    public Set<String> serviceIds() {
        return Set.copyOf(states.keySet());
    }

    public Collection<ServiceSnapshot> snapshots() {
        return List.copyOf(states.values());
    }

    public void registerFallback(String serviceId, String fallbackServiceId) {
        if (fallbackServiceId != null && !fallbackServiceId.isBlank()) {
            fallbackRegistry.put(serviceId, fallbackServiceId);
        }
    }

    public Optional<String> fallbackOf(String serviceId) {
        return Optional.ofNullable(fallbackRegistry.get(serviceId));
    }

    public ServiceSnapshot markFallback(String serviceId, boolean active) {
        return update(serviceId, snapshot -> snapshot.withFallbackActive(active));
    }

    private void registerServiceDefinition(RecoveryProperties.ServiceDefinition definition) {
        if (definition.getId() == null || definition.getId().isBlank()) {
            return;
        }
        ServiceSnapshot snapshot = new ServiceSnapshot(
                definition.getId(),
                definition.getVersion(),
                null,
                ServiceStatus.UNKNOWN,
                Instant.now(),
                false
        );
        states.put(definition.getId(), snapshot);
        registerDependencies(definition.getId(), definition.getDependencies());
        registerFallback(definition.getId(), definition.getFallback());
    }

    private ServiceSnapshot unknownSnapshot(String serviceId) {
        return new ServiceSnapshot(serviceId, "unknown", null, ServiceStatus.UNKNOWN, Instant.now(), false);
    }
}


