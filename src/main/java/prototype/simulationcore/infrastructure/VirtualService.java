package prototype.simulationcore.infrastructure;

import java.time.Instant;
import java.util.Objects;

/**
 * Lightweight description of a logical service hosted on a {@link VirtualNode}.
 */
public record VirtualService(String id, String version, String description, Instant registeredAt) {

    public VirtualService {
        Objects.requireNonNull(id, "id");
        version = (version == null || version.isBlank()) ? "1.0.0" : version;
        description = description == null ? "" : description;
        registeredAt = registeredAt == null ? Instant.now() : registeredAt;
    }
}


