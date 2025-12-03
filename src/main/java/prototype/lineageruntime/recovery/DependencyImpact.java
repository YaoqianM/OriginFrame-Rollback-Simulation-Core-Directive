package prototype.lineageruntime.recovery;

import java.util.List;

public record DependencyImpact(
        String serviceId,
        DependencyImpactType type,
        List<String> blockedBy
) {
}


