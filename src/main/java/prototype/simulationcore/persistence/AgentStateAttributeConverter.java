package prototype.simulationcore.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import prototype.simulationcore.domain.AgentState;

/**
 * Persists complex {@link AgentState} snapshots as JSON blobs.
 */
@Converter(autoApply = true)
public class AgentStateAttributeConverter implements AttributeConverter<AgentState, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(AgentState attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize agent state", e);
        }
    }

    @Override
    public AgentState convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return AgentState.initial();
        }
        try {
            return MAPPER.readValue(dbData, AgentState.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to deserialize agent state", e);
        }
    }
}


