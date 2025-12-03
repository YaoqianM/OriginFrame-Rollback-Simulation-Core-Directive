package prototype.simulationcore.infrastructure;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a simulated compute node with resources, services, and graph connections.
 */
public class VirtualNode {

    private final UUID nodeId;
    private final String name;
    private final VirtualNodeType nodeType;
    private volatile VirtualNodeStatus status;
    private final VirtualResourceProfile resources;
    private final List<VirtualService> services = new CopyOnWriteArrayList<>();
    private final Set<UUID> connections = ConcurrentHashMap.newKeySet();

    public VirtualNode(UUID nodeId,
                       String name,
                       VirtualNodeType nodeType,
                       VirtualNodeStatus status,
                       VirtualResourceProfile resources) {
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId");
        this.name = (name == null || name.isBlank()) ? "node-" + nodeId : name;
        this.nodeType = Objects.requireNonNull(nodeType, "nodeType");
        this.status = Objects.requireNonNull(status, "status");
        this.resources = Objects.requireNonNull(resources, "resources");
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public VirtualNodeType getNodeType() {
        return nodeType;
    }

    public VirtualNodeStatus getStatus() {
        return status;
    }

    public void setStatus(VirtualNodeStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public VirtualResourceProfile getResources() {
        return resources;
    }

    public List<VirtualService> getServices() {
        return Collections.unmodifiableList(services);
    }

    public void addService(VirtualService service) {
        services.add(Objects.requireNonNull(service, "service"));
    }

    public Set<UUID> getConnections() {
        return Collections.unmodifiableSet(connections);
    }

    public void addConnection(UUID other) {
        connections.add(other);
    }

    public void removeConnection(UUID other) {
        connections.remove(other);
    }

    public boolean hostsService(String serviceId) {
        return services.stream().anyMatch(s -> s.id().equalsIgnoreCase(serviceId));
    }

    public String primaryServiceId() {
        return services.isEmpty() ? null : services.get(0).id();
    }
}


