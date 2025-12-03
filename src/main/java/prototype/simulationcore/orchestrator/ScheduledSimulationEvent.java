package prototype.simulationcore.orchestrator;

import prototype.simulationcore.events.SimulationEvent;

record ScheduledSimulationEvent(long tick, long sequence, SimulationEvent event)
        implements Comparable<ScheduledSimulationEvent> {

    @Override
    public int compareTo(ScheduledSimulationEvent other) {
        int tickComparison = Long.compare(this.tick, other.tick);
        if (tickComparison != 0) {
            return tickComparison;
        }
        return Long.compare(this.sequence, other.sequence);
    }
}

