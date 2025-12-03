package prototype.lineageruntime.lineage.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LineageNotFoundException extends RuntimeException {

    public LineageNotFoundException(UUID agentId) {
        super("No lineage data available for agent %s".formatted(agentId));
    }

    public LineageNotFoundException(String message) {
        super(message);
    }
}

