package prototype.simulationcore.world;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import prototype.simulationcore.domain.Position;

/**
 * Lightweight representation of a virtual compute or storage node in the world infrastructure.
 */
public class VirtualNode implements Serializable {

    @Serial
    private static final long serialVersionUID = 2969495842068424525L;

    private final UUID nodeId;
    private final String name;
    private final Position position;
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();
    private volatile VirtualNodeStatus status = VirtualNodeStatus.ONLINE;

    public VirtualNode(String name, Position position) {
        this(UUID.randomUUID(), name, position, VirtualNodeStatus.ONLINE, Map.of());
    }

    public VirtualNode(UUID nodeId,
                       String name,
                       Position position,
                       VirtualNodeStatus status,
                       Map<String, Object> metadata) {
        this.nodeId = Objects.requireNonNullElseGet(nodeId, UUID::randomUUID);
        this.name = Objects.requireNonNullElse(name, "virtual-node");
        this.position = position == null ? Position.origin() : position;
        this.status = status == null ? VirtualNodeStatus.ONLINE : status;
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    public UUID getNodeId() {
        return nodeId;
    }

    public String getName() {
        return name;
    }

    public Position getPosition() {
        return position;
    }

    public VirtualNodeStatus getStatus() {
        return status;
    }

    public void updateStatus(VirtualNodeStatus newStatus) {
        if (newStatus != null) {
            status = newStatus;
        }
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    public void putMetadata(String key, Object value) {
        if (key != null && !key.isBlank()) {
            metadata.put(key, value);
        }
    }

    public VirtualNodeState snapshot() {
        return new VirtualNodeState(nodeId, name, position, status, Map.copyOf(metadata));
    }
}


