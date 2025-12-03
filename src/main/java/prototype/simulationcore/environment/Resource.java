package prototype.simulationcore.environment;

import java.io.Serial;
import java.io.Serializable;

/**
 * Simple resource representation tracked by the environment.
 */
public record Resource(String type, double quantity, double regenerationRate) implements Serializable {

    @Serial
    private static final long serialVersionUID = 7149565361616656306L;

    public Resource {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("Resource type must be provided.");
        }
        quantity = Math.max(0.0, quantity);
        regenerationRate = Math.max(0.0, regenerationRate);
    }

    public Resource regenerate() {
        return new Resource(type, quantity + regenerationRate, regenerationRate);
    }

    public Resource consume(double amount) {
        double remaining = Math.max(0.0, quantity - Math.max(0.0, amount));
        return new Resource(type, remaining, regenerationRate);
    }
}


