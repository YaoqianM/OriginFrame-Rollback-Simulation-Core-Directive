package prototype.simulationcore.orchestrator;

public record ScenarioSuccessCriteria(
        Long targetTick,
        Integer maxConstraintViolations
) {

    public boolean isSatisfied(SimulationWorld world, long tick) {
        boolean meetsTick = targetTick == null || tick >= targetTick;
        boolean meetsConstraints = maxConstraintViolations == null
                || world.getConstraintViolationCount() <= maxConstraintViolations;
        return meetsTick && meetsConstraints;
    }
}

