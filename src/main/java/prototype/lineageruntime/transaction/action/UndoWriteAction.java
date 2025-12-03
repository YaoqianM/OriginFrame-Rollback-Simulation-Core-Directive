package prototype.lineageruntime.transaction.action;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import prototype.lineageruntime.transaction.CompensatingAction;

public class UndoWriteAction implements CompensatingAction {

    private final Path targetFile;
    private final byte[] previousContent;

    public UndoWriteAction(Path targetFile, String previousContent) {
        this.targetFile = targetFile;
        this.previousContent = previousContent == null ? new byte[0] : previousContent.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void execute() {
        try {
            Files.write(targetFile, previousContent);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to restore contents for " + targetFile, e);
        }
    }

    @Override
    public String description() {
        return "Undo write to " + targetFile;
    }
}

