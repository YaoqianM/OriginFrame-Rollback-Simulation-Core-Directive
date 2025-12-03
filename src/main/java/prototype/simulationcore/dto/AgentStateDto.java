package prototype.simulationcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.domain.Position;

@Schema(name = "AgentState", description = "Telemetry describing the agent's current physical and internal state.")
public record AgentStateDto(
        @Schema(description = "3D coordinates of the agent inside the environment.")
        Position position,
        @Schema(description = "Current energy budget.", example = "37.5")
        double energy,
        @Schema(description = "Available resources collected by the agent.", example = "12.0")
        double resources,
        @Schema(description = "Latest sensor readings captured by the agent.")
        Map<String, Double> sensorReadings,
        @Schema(description = "Opaque internal state maintained by the policy.")
        Map<String, Double> internalState
) {

    public static AgentStateDto from(AgentState state) {
        if (state == null) {
            state = AgentState.initial();
        }
        return new AgentStateDto(
                state.position(),
                state.energy(),
                state.resources(),
                state.sensorReadings(),
                state.internalState()
        );
    }
}
