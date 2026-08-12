package sorbonne.professional_website.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores small ordered string collections in a single TEXT column.
 * This avoids multiplying collection-table round trips for read-heavy public
 * project metadata such as proof tags and case-study bullet lists.
 */
@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() { };

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Impossible de sérialiser la liste de chaînes.", exception);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return new ArrayList<>();
        try {
            List<String> values = MAPPER.readValue(dbData, STRING_LIST);
            return values == null ? new ArrayList<>() : new ArrayList<>(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Liste JSON persistée invalide.", exception);
        }
    }
}
