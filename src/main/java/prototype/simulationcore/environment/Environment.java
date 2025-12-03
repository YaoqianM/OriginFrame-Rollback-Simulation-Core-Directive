package prototype.simulationcore.environment;

import java.util.Map;
import prototype.simulationcore.domain.Position;

/**
 * Describes the observable portion of the world used by policies for decision making.
 */
public interface Environment {

    Position getTargetPosition();

    double readSignal(String key);

    Map<String, Double> snapshotSensors();
}


