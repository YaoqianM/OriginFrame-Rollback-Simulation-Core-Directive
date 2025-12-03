package prototype.simulationcore.policy;

import java.util.Map;
import prototype.simulationcore.domain.Action;
import prototype.simulationcore.domain.AgentState;
import prototype.simulationcore.environment.Environment;

/**
 * Contract for decision making logic that drives an agent.
 */
public interface AgentPolicy {

    Action decide(AgentState state, Environment environment);

    Map<String, Double> getParameters();

    void setParameters(Map<String, Double> parameters);
}


