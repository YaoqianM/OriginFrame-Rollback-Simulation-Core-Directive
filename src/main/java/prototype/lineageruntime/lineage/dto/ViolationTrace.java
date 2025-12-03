package prototype.lineageruntime.lineage.dto;

import java.util.UUID;
import prototype.lineageruntime.lineage.domain.MutationEvent;

public record ViolationTrace(
        UUID agentId,
        MutationEvent originMutation,
        String violationReason
) {
}

