package prototype.simulationcore.evolution.selection;

import java.util.List;
import prototype.simulationcore.domain.Agent;

@FunctionalInterface
public interface SelectionStrategy {

    List<Agent> select(List<Agent> population, int survivorCount);
}


