package prototype.simulationcore.world;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_runs")
public class SimulationRun implements Serializable {

    @Serial
    private static final long serialVersionUID = 4936353115142458015L;

    @Id
    @GeneratedValue
    @Column(name = "run_id", nullable = false, updatable = false)
    private UUID runId;

    @Column(name = "world_id", nullable = false, unique = true)
    private UUID worldId;

    @Column(name = "world_name", nullable = false)
    private String worldName;

    @Lob
    @Column(name = "world_config", columnDefinition = "LONGTEXT")
    private String worldConfigJson;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "total_ticks")
    private long totalTicks;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorldStatus status = WorldStatus.CREATED;

    @Lob
    @Column(name = "result_summary", columnDefinition = "LONGTEXT")
    private String resultSummary;

    public UUID getRunId() {
        return runId;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldConfigJson() {
        return worldConfigJson;
    }

    public void setWorldConfigJson(String worldConfigJson) {
        this.worldConfigJson = worldConfigJson;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public long getTotalTicks() {
        return totalTicks;
    }

    public void setTotalTicks(long totalTicks) {
        this.totalTicks = totalTicks;
    }

    public WorldStatus getStatus() {
        return status;
    }

    public void setStatus(WorldStatus status) {
        this.status = status;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public void recordTick(long tick) {
        this.totalTicks = Math.max(this.totalTicks, tick);
    }

    public void markCompleted(long finalTick, String summary) {
        this.status = WorldStatus.COMPLETED;
        this.totalTicks = finalTick;
        this.resultSummary = summary;
        this.endTime = Instant.now();
    }
}


