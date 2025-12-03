package prototype.simulationcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import prototype.simulationcore.policy.AbstractAgentPolicy;

@Schema(name = "AgentPolicy", description = "Summary of the policy driving agent decisions.")
public record AgentPolicyDto(
        @Schema(description = "Unique policy identifier.", example = "6c3f3d3a-5e2b-4a0f-8f7d-3a2b1c0d9e8f")
        UUID policyId,
        @Schema(description = "Simplified policy type.", example = "Neural")
        String type,
        @Schema(description = "Key/value map of tunable policy parameters.")
        Map<String, Double> parameters
) {

    public static AgentPolicyDto from(AbstractAgentPolicy policy) {
        if (policy == null) {
            return null;
        }
        String type = Optional.of(policy.getClass().getSimpleName())
                .map(name -> name.replace("Policy", ""))
                .orElse("Unknown");
        return new AgentPolicyDto(policy.getPolicyId(), type, policy.getParameters());
    }
}
