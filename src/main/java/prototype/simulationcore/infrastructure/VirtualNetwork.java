package prototype.simulationcore.infrastructure;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * In-memory representation of the virtual infrastructure graph. Tracks nodes, links, latency and faults.
 */
@Component
public class VirtualNetwork {

    private static final Logger log = LoggerFactory.getLogger(VirtualNetwork.class);

    private final Map<UUID, VirtualNode> nodes = new ConcurrentHashMap<>();
    private final Map<LinkKey, VirtualLink> links = new ConcurrentHashMap<>();

    public VirtualNode registerNode(VirtualNode node) {
        nodes.put(node.getNodeId(), node);
        return node;
    }

    public Optional<VirtualNode> findNode(UUID nodeId) {
        return Optional.ofNullable(nodes.get(nodeId));
    }

    public Collection<VirtualNode> nodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    public Collection<VirtualLink> links() {
        return Collections.unmodifiableCollection(links.values());
    }

    public void connect(UUID left, UUID right, long baseLatencyMs, double packetLossProbability) {
        if (left.equals(right)) {
            return;
        }
        VirtualNode leftNode = requireNode(left);
        VirtualNode rightNode = requireNode(right);

        LinkKey key = LinkKey.of(left, right);
        links.computeIfAbsent(key, ignored -> new VirtualLink(left, right, baseLatencyMs, packetLossProbability));
        leftNode.addConnection(right);
        rightNode.addConnection(left);
        log.debug("Connected {} <-> {} with latency {}ms", leftNode.getName(), rightNode.getName(), baseLatencyMs);
    }

    public double simulateLatency(UUID source, UUID destination) {
        VirtualLink link = linkBetween(source, destination)
                .orElseThrow(() -> new IllegalArgumentException("No link between nodes"));
        if (link.isPartitioned() || isFailed(source) || isFailed(destination)) {
            return Double.POSITIVE_INFINITY;
        }
        double multiplier = 1.0;
        if (isDegraded(source)) {
            multiplier += 0.5;
        }
        if (isDegraded(destination)) {
            multiplier += 0.5;
        }
        return (link.getBaseLatencyMs() + link.getInjectedLatencyMs()) * multiplier;
    }

    public boolean simulatePacketLoss(UUID source, UUID destination) {
        VirtualLink link = linkBetween(source, destination)
                .orElseThrow(() -> new IllegalArgumentException("No link between nodes"));
        if (link.isPartitioned() || isFailed(source) || isFailed(destination)) {
            return true;
        }
        double probability = link.getPacketLossProbability();
        if (isDegraded(source)) {
            probability += 0.05;
        }
        if (isDegraded(destination)) {
            probability += 0.05;
        }
        probability = Math.min(0.95, probability);
        return ThreadLocalRandom.current().nextDouble() < probability;
    }

    public void injectLatency(UUID source, UUID destination, long additionalMs) {
        linkBetween(source, destination)
                .ifPresent(link -> link.setInjectedLatencyMs(Math.max(additionalMs, 0)));
    }

    public void partition(Set<UUID> group1, Set<UUID> group2, boolean partitioned) {
        links.forEach((key, link) -> {
            boolean affects = (group1.contains(link.getLeft()) && group2.contains(link.getRight()))
                    || (group1.contains(link.getRight()) && group2.contains(link.getLeft()));
            if (affects) {
                link.setPartitioned(partitioned);
            }
        });
    }

    private VirtualNode requireNode(UUID nodeId) {
        VirtualNode node = nodes.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Node " + nodeId + " not registered");
        }
        return node;
    }

    private Optional<VirtualLink> linkBetween(UUID left, UUID right) {
        return Optional.ofNullable(links.get(LinkKey.of(left, right)));
    }

    private boolean isFailed(UUID nodeId) {
        return nodes.containsKey(nodeId) && nodes.get(nodeId).getStatus() == VirtualNodeStatus.FAILED;
    }

    private boolean isDegraded(UUID nodeId) {
        return nodes.containsKey(nodeId) && nodes.get(nodeId).getStatus() == VirtualNodeStatus.DEGRADED;
    }

    private record LinkKey(UUID left, UUID right) {
        static LinkKey of(UUID a, UUID b) {
            if (a.compareTo(b) < 0) {
                return new LinkKey(a, b);
            }
            return new LinkKey(b, a);
        }
    }

    public static final class VirtualLink {

        private final UUID left;
        private final UUID right;
        private final long baseLatencyMs;
        private volatile long injectedLatencyMs;
        private volatile double packetLossProbability;
        private volatile boolean partitioned;

        private VirtualLink(UUID left, UUID right, long baseLatencyMs, double packetLossProbability) {
            this.left = left;
            this.right = right;
            this.baseLatencyMs = baseLatencyMs;
            this.packetLossProbability = packetLossProbability;
        }

        public UUID getLeft() {
            return left;
        }

        public UUID getRight() {
            return right;
        }

        public long getBaseLatencyMs() {
            return baseLatencyMs;
        }

        public long getInjectedLatencyMs() {
            return injectedLatencyMs;
        }

        public void setInjectedLatencyMs(long injectedLatencyMs) {
            this.injectedLatencyMs = injectedLatencyMs;
        }

        public double getPacketLossProbability() {
            return packetLossProbability;
        }

        public void setPacketLossProbability(double packetLossProbability) {
            this.packetLossProbability = packetLossProbability;
        }

        public boolean isPartitioned() {
            return partitioned;
        }

        public void setPartitioned(boolean partitioned) {
            this.partitioned = partitioned;
        }
    }
}


