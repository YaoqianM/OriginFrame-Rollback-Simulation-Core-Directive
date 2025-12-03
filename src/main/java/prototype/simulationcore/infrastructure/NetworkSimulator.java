package prototype.simulationcore.infrastructure;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.health.HealthMonitorService;

/**
 * Routes synthetic messages across the {@link VirtualNetwork} while injecting latency and loss.
 * Integrates with Project A health metrics for observability.
 */
@Service
public class NetworkSimulator {

    private static final Logger log = LoggerFactory.getLogger(NetworkSimulator.class);
    private static final int EVENT_WINDOW = 200;

    private final VirtualNetwork virtualNetwork;
    private final FaultInjector faultInjector;
    private final HealthMonitorService healthMonitor;
    private final AtomicLong ticks = new AtomicLong();
    private final Deque<NetworkEvent> events = new ArrayDeque<>();

    public NetworkSimulator(VirtualNetwork virtualNetwork,
                            FaultInjector faultInjector,
                            HealthMonitorService healthMonitor) {
        this.virtualNetwork = virtualNetwork;
        this.faultInjector = faultInjector;
        this.healthMonitor = healthMonitor;
    }

    public RouteResult routeMessage(UUID source, UUID destination, String payload) {
        long tick = ticks.incrementAndGet();
        faultInjector.processTick(tick);

        VirtualNode sourceNode = virtualNetwork.findNode(source)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source node " + source));
        VirtualNode destinationNode = virtualNetwork.findNode(destination)
                .orElseThrow(() -> new IllegalArgumentException("Unknown destination node " + destination));

        boolean dropped = virtualNetwork.simulatePacketLoss(source, destination);
        if (dropped) {
            recordEvent(new NetworkEvent(
                    tick,
                    Instant.now(),
                    source,
                    destination,
                    "PACKET_LOSS",
                    "Packet dropped between " + sourceNode.getName() + " and " + destinationNode.getName(),
                    0,
                    false
            ));
            recordErrorRate(sourceNode);
            recordErrorRate(destinationNode);
            return new RouteResult(false, Double.POSITIVE_INFINITY, tick, "Packet dropped");
        }

        double latency = virtualNetwork.simulateLatency(source, destination);
        recordEvent(new NetworkEvent(
                tick,
                Instant.now(),
                source,
                destination,
                "DELIVERED",
                "Message delivered in " + latency + "ms",
                latency,
                true
        ));
        recordLatency(sourceNode, latency);
        recordLatency(destinationNode, latency);
        return new RouteResult(true, latency, tick, payload);
    }

    public List<NetworkEvent> recentEvents() {
        return List.copyOf(events);
    }

    private void recordLatency(VirtualNode node, double latency) {
        node.getServices().forEach(service -> {
            healthMonitor.recordLatency(service.id(), latency);
            healthMonitor.recordHeartbeat(service.id());
        });
    }

    private void recordErrorRate(VirtualNode node) {
        node.getServices().forEach(service -> healthMonitor.recordErrorRate(service.id(), 1.0));
    }

    private void recordEvent(NetworkEvent event) {
        synchronized (events) {
            events.addLast(event);
            if (events.size() > EVENT_WINDOW) {
                events.removeFirst();
            }
        }
        log.debug("Network event {} -> {} {}", event.source(), event.destination(), event.type());
    }

    public record RouteResult(boolean delivered, double latencyMs, long tick, String payloadEcho) {
    }
}


