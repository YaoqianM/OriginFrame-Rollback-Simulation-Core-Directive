package prototype.simulationcore.domain;

import java.io.Serializable;

/**
 * Minimal 3D coordinate used by the simulation environment.
 */
public record Position(double x, double y, double z) implements Serializable {

    private static final long serialVersionUID = -808174502803142019L;
    private static final Position ORIGIN = new Position(0.0, 0.0, 0.0);

    public static Position origin() {
        return ORIGIN;
    }

    public Position offset(double dx, double dy, double dz) {
        return new Position(x + dx, y + dy, z + dz);
    }

    public double distanceTo(Position other) {
        Position reference = other == null ? ORIGIN : other;
        double dx = x - reference.x;
        double dy = y - reference.y;
        double dz = z - reference.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}


