package prototype.simulationcore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import prototype.simulationcore.world.config.WorldConfig;

/**
 * Request payload for creating a new simulation world. Any nested {@link WorldConfig} properties are
 * optional overrides of the defaults defined in YAML.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SimulationCreateRequest(
        String name,
        WorldConfig config
) {
}


