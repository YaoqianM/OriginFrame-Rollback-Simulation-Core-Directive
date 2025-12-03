package prototype.simulationcore.infrastructure;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import prototype.lineageruntime.health.HealthMonitorService;
import prototype.lineageruntime.recovery.RecoveryProperties;

/**
 * Bootstraps the virtual infrastructure and provides topology snapshots for the API.
 */
@Service
public class VirtualInfrastructureService {

    private static final Logger log = LoggerFactory.getLogger(VirtualInfrastructureService.class);

    private final VirtualNetwork virtualNetwork;
    private final NetworkSimulator networkSimulator;
    private final HealthMonitorService healthMonitorService;
    private final RecoveryProperties recoveryProperties;
    private final Map<String, UUID> serviceToNode = new HashMap<>();

    public VirtualInfrastructureService(VirtualNetwork virtualNetwork,
                                        NetworkSimulator networkSimulator,
                                        HealthMonitorService healthMonitorService,
                                        RecoveryProperties recoveryProperties) {
        this.virtualNetwork = virtualNetwork;
        this.networkSimulator = networkSimulator;
        this.healthMonitorService = healthMonitorService;
        this.recoveryProperties = recoveryProperties;
    }

    @PostConstruct
    public void bootstrap() {
        if (!virtualNetwork.nodes().isEmpty()) {
            return;
        }
        log.info("Bootstrapping virtual infrastructure from recovery properties");
        recoveryProperties.getServices().forEach(def -> registerServiceNode(
                def.getId(),
                def.getVersion(),
                inferType(def.getId()),
                "Simulated service " + def.getId()
        ));

        // Ensure dependencies referenced in config exist as nodes too.
        recoveryProperties.getServices().forEach(def -> def.getDependencies()
                .forEach(dep -> registerServiceNode(dep, "1.0.0", inferType(dep), "Dependency service " + dep)));

        // Register fallback endpoints if defined.
        recoveryProperties.getServices().forEach(def -> {
            if (def.getFallback() != null && !def.getFallback().isBlank()) {
                registerServiceNode(def.getFallback(), "1.0.0", VirtualNodeType.SERVER, "Fallback endpoint");
            }
        });

        registerControlPlane();
        wireDependencies();
    }

    public InfrastructureTopologyView currentTopology() {
        List<InfrastructureTopologyView.NodeView> nodeViews = virtualNetwork.nodes().stream()
                .map(this::toView)
                .toList();
        List<InfrastructureTopologyView.LinkView> linkViews = virtualNetwork.links().stream()
                .map(link -> new InfrastructureTopologyView.LinkView(
                        link.getLeft(),
                        link.getRight(),
                        link.getBaseLatencyMs(),
                        link.getInjectedLatencyMs(),
                        link.getPacketLossProbability(),
                        link.isPartitioned()))
                .toList();
        return new InfrastructureTopologyView(nodeViews, linkViews, networkSimulator.recentEvents());
    }

    public UUID nodeIdForService(String serviceId) {
        return serviceToNode.get(serviceId);
    }

    @Scheduled(fixedDelayString = "${simulation.infrastructure.heartbeat-interval:5000}")
    public void emitBackgroundHeartbeats() {
        virtualNetwork.nodes().forEach(node ->
                node.getServices().forEach(service -> healthMonitorService.recordHeartbeat(service.id())));
    }

    private InfrastructureTopologyView.NodeView toView(VirtualNode node) {
        VirtualResourceProfile resources = node.getResources();
        return new InfrastructureTopologyView.NodeView(
                node.getNodeId(),
                node.getName(),
                node.getNodeType(),
                node.getStatus(),
                resources.cpuCapacityCores(),
                resources.cpuLoadCores(),
                resources.memoryCapacityGb(),
                resources.memoryLoadGb(),
                resources.degradationRatio(),
                node.getServices(),
                node.getConnections()
        );
    }

    private void registerServiceNode(String serviceId, String version, VirtualNodeType type, String description) {
        if (serviceId == null || serviceId.isBlank() || serviceToNode.containsKey(serviceId)) {
            return;
        }
        VirtualNode node = new VirtualNode(
                UUID.nameUUIDFromBytes(serviceId.getBytes()),
                serviceId + "-node",
                type,
                VirtualNodeStatus.HEALTHY,
                createResourceProfile()
        );
        node.addService(new VirtualService(serviceId, version, description, null));
        virtualNetwork.registerNode(node);
        serviceToNode.put(serviceId, node.getNodeId());
        log.info("Registered virtual node {} ({})", node.getName(), type);
    }

    private void wireDependencies() {
        Set<String> alreadyWired = new HashSet<>();
        recoveryProperties.getServices().forEach(def -> {
            UUID source = serviceToNode.get(def.getId());
            if (source == null) {
                return;
            }
            def.getDependencies().forEach(dep -> {
                UUID target = serviceToNode.get(dep);
                if (target == null) {
                    return;
                }
                String connectionKey = def.getId() + "->" + dep;
                if (alreadyWired.add(connectionKey)) {
                    virtualNetwork.connect(
                            source,
                            target,
                            ThreadLocalRandom.current().nextLong(10, 80),
                            ThreadLocalRandom.current().nextDouble(0.01, 0.05)
                    );
                }
            });
        });
    }

    private void registerControlPlane() {
        registerServiceNode("health-monitor", "1.0.0", VirtualNodeType.SERVER, "Project A health monitor");
        registerServiceNode("recovery-orchestrator", "1.0.0", VirtualNodeType.SERVER, "Project A recovery orchestrator");
        connectIfPresent("health-monitor", "recovery-orchestrator");
        recoveryProperties.getServices().forEach(def -> connectIfPresent("health-monitor", def.getId()));
    }

    private void connectIfPresent(String leftService, String rightService) {
        UUID left = serviceToNode.get(leftService);
        UUID right = serviceToNode.get(rightService);
        if (left == null || right == null) {
            return;
        }
        virtualNetwork.connect(
                left,
                right,
                ThreadLocalRandom.current().nextLong(5, 25),
                ThreadLocalRandom.current().nextDouble(0.0, 0.01)
        );
    }

    private VirtualNodeType inferType(String serviceId) {
        String id = serviceId == null ? "" : serviceId.toLowerCase();
        if (id.contains("storage") || id.contains("db")) {
            return VirtualNodeType.DATABASE;
        }
        if (id.contains("gateway") || id.contains("server")) {
            return VirtualNodeType.SERVER;
        }
        return VirtualNodeType.SERVICE;
    }

    private VirtualResourceProfile createResourceProfile() {
        double cpu = ThreadLocalRandom.current().nextDouble(4, 16);
        double memory = ThreadLocalRandom.current().nextDouble(8, 64);
        VirtualResourceProfile profile = new VirtualResourceProfile(cpu, memory);
        profile.applyLoad(cpu * ThreadLocalRandom.current().nextDouble(0.2, 0.5),
                memory * ThreadLocalRandom.current().nextDouble(0.2, 0.5));
        return profile;
    }
}


