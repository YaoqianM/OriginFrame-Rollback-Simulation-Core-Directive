package prototype.simulationcore.infrastructure;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO describing the current state of the virtual infrastructure graph.
 */
public record InfrastructureTopologyView(List<NodeView> nodes,
                                          List<LinkView> connections,
                                          List<NetworkEvent> networkEvents) {

    public record NodeView(UUID nodeId,
                           String name,
                           VirtualNodeType type,
                           VirtualNodeStatus status,
                           double cpuCapacity,
                           double cpuLoad,
                           double memoryCapacity,
                           double memoryLoad,
                           double degradationRatio,
                           List<VirtualService> services,
                           Set<UUID> connections) {
    }

    public record LinkView(UUID left,
                           UUID right,
                           long baseLatencyMs,
                           long injectedLatencyMs,
                           double packetLossProbability,
                           boolean partitioned) {
    }
}


