package prototype.simulationcore.infrastructure;

/**
 * REST response for infrastructure mutations (failures, degradations, etc.).
 */
public record InfrastructureMutationResponse(String simulationId,
                                             FaultInjector.InfrastructureFaultImpact impact,
                                             InfrastructureTopologyView topology) {
}


