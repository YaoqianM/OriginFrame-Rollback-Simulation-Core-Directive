package prototype.lineageruntime.transaction;

public interface CompensatingAction {

    void execute();

    String description();
}

