package prototype.visualization.service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import prototype.lineageruntime.kafka.EventConsumer;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.repository.AgentRepository;
import prototype.visualization.model.DashboardMetrics;

/**
 * Produces aggregated dashboard metrics and streams them via Server-Sent Events.
 */
@Service
public class DashboardDataService {

    private static final long STREAM_TIMEOUT_MILLIS = Duration.ofMinutes(10).toMillis();

    private final AgentRepository agentRepository;
    private final EventConsumer eventConsumer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public DashboardDataService(AgentRepository agentRepository,
                                EventConsumer eventConsumer) {
        this.agentRepository = agentRepository;
        this.eventConsumer = eventConsumer;
    }

    public DashboardMetrics snapshot(String simulationId) {
        List<Agent> agents = agentRepository.findAll();
        return DashboardMetrics.from(simulationId, agents, eventConsumer.getHistory());
    }

    public SseEmitter streamMetrics(String simulationId) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        ScheduledFuture<?> scheduled = scheduler.scheduleAtFixedRate(() -> {
            try {
                DashboardMetrics metrics = snapshot(simulationId);
                emitter.send(SseEmitter.event()
                        .id(UUID.randomUUID().toString())
                        .name("metrics")
                        .data(metrics));
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }, 0, 1, TimeUnit.SECONDS);

        Runnable cleanup = () -> scheduled.cancel(true);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ex -> cleanup.run());
        return emitter;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }
}


