package prototype.lineageruntime.transaction.action;

import java.util.Objects;
import java.util.function.Supplier;
import prototype.lineageruntime.transaction.CompensatingAction;

public class RevertStateAction implements CompensatingAction {

    private final String description;
    private final Supplier<Boolean> revertOperation;

    public RevertStateAction(String description, Supplier<Boolean> revertOperation) {
        this.description = Objects.requireNonNull(description, "description");
        this.revertOperation = Objects.requireNonNull(revertOperation, "revertOperation");
    }

    @Override
    public void execute() {
        boolean success = revertOperation.get();
        if (!success) {
            throw new IllegalStateException("Revert operation reported failure: " + description);
        }
    }

    @Override
    public String description() {
        return description;
    }
}

