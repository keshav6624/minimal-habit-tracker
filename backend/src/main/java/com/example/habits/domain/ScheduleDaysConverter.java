package com.example.habits.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps {@code List<Integer>} of ISO day-of-week values (1=Mon .. 7=Sun) to a
 * TEXT column holding a JSON array such as {@code "[1,3,7]"}.
 *
 * <p>Documented deviation from a native array column: TEXT + converter keeps the
 * Flyway DDL portable across PostgreSQL and H2 (no database-specific types).</p>
 */
@Converter
public class ScheduleDaysConverter implements AttributeConverter<List<Integer>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Integer> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            // Cannot happen for a List<Integer>; treat as a mapping failure.
            throw new IllegalStateException("Failed to serialize schedule days", e);
        }
    }

    @Override
    public List<Integer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return MAPPER.readValue(dbData, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, Integer.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to read schedule days: " + dbData, e);
        }
    }
}
