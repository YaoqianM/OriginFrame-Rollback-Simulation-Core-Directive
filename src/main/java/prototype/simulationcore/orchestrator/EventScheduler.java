package prototype.simulationcore.orchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import prototype.simulationcore.events.SimulationEvent;

/**
 * Priority queue based scheduler that keeps per-world timed events sorted by tick.
 */
public class EventScheduler {

    private final PriorityBlockingQueue<ScheduledSimulationEvent> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequence = new AtomicLong();

    public void scheduleEvent(long tick, SimulationEvent event) {
        queue.offer(new ScheduledSimulationEvent(tick, sequence.incrementAndGet(), event));
    }

    public List<SimulationEvent> drainDueEvents(long upToTick) {
        List<SimulationEvent> due = new ArrayList<>();
        while (true) {
            ScheduledSimulationEvent head = queue.peek();
            if (head == null || head.tick() > upToTick) {
                break;
            }
            ScheduledSimulationEvent scheduled = queue.poll();
            if (scheduled != null) {
                due.add(scheduled.event());
            }
        }
        return due;
    }

    public void clear() {
        queue.clear();
    }
}

