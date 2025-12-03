package prototype.integration.grid;

import prototype.lineageruntime.recovery.ServiceStatus;
import prototype.simulationcore.infrastructure.VirtualNodeStatus;

public record GridNodeView(
        String nodeId,
        String displayName,
        String serviceId,
        VirtualNodeStatus infrastructureStatus,
        ServiceStatus runtimeStatus,
        double cpuUtilization,
        double memoryUtilization,
        double degradationRatio,
        boolean fallbackActive,
        long lastUpdatedTick
) {
}


