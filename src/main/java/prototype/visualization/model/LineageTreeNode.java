package prototype.visualization.model;

import java.util.List;
import java.util.UUID;
import prototype.simulationcore.domain.Agent;
import prototype.simulationcore.dto.AgentStateDto;

/**
 * Node representation for a lineage tree.
 */
public record LineageTreeNode(
        UUID agentId,
        UUID parentId,
        int generation,
        double fitness,
        AgentStateDto state,
        List<LineageTreeNode> children
) {

    public LineageTreeNode {
        children = children == null ? List.of() : List.copyOf(children);
    }

    public static LineageTreeNode from(Agent agent, List<LineageTreeNode> children) {
        if (agent == null) {
            return null;
        }
        return new LineageTreeNode(
                agent.getAgentId(),
                agent.getParentId(),
                agent.getGeneration(),
                agent.getFitness(),
                AgentStateDto.from(agent.getState()),
                children
        );
    }
}


