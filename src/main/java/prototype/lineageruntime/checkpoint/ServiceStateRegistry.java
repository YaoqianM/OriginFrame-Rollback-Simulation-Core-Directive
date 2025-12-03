package prototype.lineageruntime.checkpoint;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ServiceStateRegistry {

    private static final Logger log = LoggerFactory.getLogger(ServiceStateRegistry.class);

    private final Map<String, ServiceStateAdapter> adapters = new ConcurrentHashMap<>();

    public ServiceStateRegistry(java.util.List<ServiceStateAdapter> discoveredAdapters) {
        for (ServiceStateAdapter adapter : discoveredAdapters) {
            register(adapter);
        }
    }

    public void register(ServiceStateAdapter adapter) {
        Objects.requireNonNull(adapter, "adapter");
        ServiceStateAdapter existing = adapters.put(adapter.serviceId(), adapter);
        if (existing != null) {
            log.warn("Service state adapter for {} replaced previous instance {}", adapter.serviceId(), existing.getClass().getName());
        } else {
            log.info("Registered service state adapter {} for {}", adapter.getClass().getSimpleName(), adapter.serviceId());
        }
    }

    public ServiceStateAdapter getRequiredAdapter(String serviceId) {
        ServiceStateAdapter adapter = adapters.get(serviceId);
        if (adapter == null) {
            throw new ServiceAdapterNotFoundException(serviceId);
        }
        return adapter;
    }

    public Collection<String> getRegisteredServiceIds() {
        return Collections.unmodifiableSet(adapters.keySet());
    }
}

