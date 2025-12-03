package prototype.simulationcore.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.policy.AbstractAgentPolicy;
import prototype.simulationcore.policy.AgentPolicy;

@Schema(name = "Agent", description = "Aggregated view of the digital agent with state and policy metadata.")
public record AgentDto(
        @Schema(description = "Unique identifier for the agent.", example = "8c6e6d3e-61a2-4f34-8af8-7a89c0a15f75")
        UUID agentId,
        @Schema(description = "Identifier of the parent agent, if this agent was spawned.", example = "e1c9b1f5-3b2a-4f1f-8f9d-4b9c932d9dab")
        UUID parentId,
        @Schema(description = "Evolutionary generation counter.", example = "42")
        int generation,
        @Schema(description = "Fitness score assigned by the policy.", example = "0.91")
        double fitness,
        @Schema(description = "Number of safety boundary violations recorded for the agent.", example = "0")
        int safetyViolations,
        @Schema(description = "Creation timestamp.", example = "2025-05-05T10:00:00Z")
        Instant createdAt,
        @Schema(description = "Current snapshot of the agent state.")
        AgentStateDto state,
        @Schema(description = "Active policy definition driving the agent.")
        AgentPolicyDto policy
) {

    public static AgentDto from(Agent agent) {
        if (agent == null) {
            return null;
        }
        AgentPolicy policy = agent.getPolicy();
        AgentPolicyDto policyDto = null;
        if (policy instanceof AbstractAgentPolicy concrete) {
            policyDto = AgentPolicyDto.from(concrete);
        }
        return new AgentDto(
                agent.getAgentId(),
                agent.getParentId(),
                agent.getGeneration(),
                agent.getFitness(),
                agent.getSafetyViolations(),
                agent.getCreatedAt(),
                AgentStateDto.from(agent.getState()),
                policyDto
        );
    }
}
