package prototype.simulationcore.infrastructure;

/**
 * Simulated compute resources attached to a {@link VirtualNode}. Tracks utilization and degradation.
 */
public class VirtualResourceProfile {

    private final double cpuCapacityCores;
    private final double memoryCapacityGb;
    private double cpuLoadCores;
    private double memoryLoadGb;
    private double degradationRatio;

    public VirtualResourceProfile(double cpuCapacityCores, double memoryCapacityGb) {
        if (cpuCapacityCores <= 0 || memoryCapacityGb <= 0) {
            throw new IllegalArgumentException("Resource capacities must be positive");
        }
        this.cpuCapacityCores = cpuCapacityCores;
        this.memoryCapacityGb = memoryCapacityGb;
    }

    public synchronized void applyLoad(double cpuDeltaCores, double memoryDeltaGb) {
        cpuLoadCores = clamp(cpuLoadCores + cpuDeltaCores, 0, cpuCapacityCores);
        memoryLoadGb = clamp(memoryLoadGb + memoryDeltaGb, 0, memoryCapacityGb);
    }

    public synchronized void degrade(double percentage) {
        double delta = percentage / 100.0;
        degradationRatio = clamp(degradationRatio + delta, 0, 0.95);
    }

    public synchronized void collapse() {
        cpuLoadCores = cpuCapacityCores;
        memoryLoadGb = memoryCapacityGb;
        degradationRatio = 0.95;
    }

    public synchronized void restore() {
        degradationRatio = 0;
        cpuLoadCores = Math.min(cpuLoadCores, cpuCapacityCores * 0.5);
        memoryLoadGb = Math.min(memoryLoadGb, memoryCapacityGb * 0.5);
    }

    public synchronized double cpuCapacityCores() {
        return cpuCapacityCores;
    }

    public synchronized double memoryCapacityGb() {
        return memoryCapacityGb;
    }

    public synchronized double cpuLoadCores() {
        return cpuLoadCores;
    }

    public synchronized double memoryLoadGb() {
        return memoryLoadGb;
    }

    public synchronized double effectiveCpuCapacity() {
        return cpuCapacityCores * (1 - degradationRatio);
    }

    public synchronized double effectiveMemoryCapacity() {
        return memoryCapacityGb * (1 - degradationRatio);
    }

    public synchronized double degradationRatio() {
        return degradationRatio;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}


