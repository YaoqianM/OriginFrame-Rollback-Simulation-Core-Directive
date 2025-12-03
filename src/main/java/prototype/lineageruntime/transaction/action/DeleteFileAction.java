package prototype.lineageruntime.transaction.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import prototype.lineageruntime.transaction.CompensatingAction;

public class DeleteFileAction implements CompensatingAction {

    private final Path targetFile;

    public DeleteFileAction(Path targetFile) {
        this.targetFile = targetFile;
    }

    @Override
    public void execute() {
        try {
            if (Files.exists(targetFile)) {
                Files.delete(targetFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete " + targetFile, e);
        }
    }

    @Override
    public String description() {
        return "Delete file " + targetFile;
    }
}

