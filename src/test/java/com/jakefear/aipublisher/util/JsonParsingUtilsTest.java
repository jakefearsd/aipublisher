package com.jakefear.aipublisher.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JsonParsingUtils.
 */
@DisplayName("JsonParsingUtils")
class JsonParsingUtilsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Nested
    @DisplayName("cleanJsonResponse")
    class CleanJsonResponse {

        @Test
        @DisplayName("Returns {} for null input")
        void returnsEmptyObjectForNull() {
            assertEquals("{}", JsonParsingUtils.cleanJsonResponse(null));
        }

        @Test
        @DisplayName("Returns trimmed JSON for clean input")
        void returnsTrimmedForCleanInput() {
            assertEquals("{\"key\": \"value\"}",
                    JsonParsingUtils.cleanJsonResponse("  {\"key\": \"value\"}  "));
        }

        @Test
        @DisplayName("Removes ```json prefix")
        void removesJsonCodeBlockPrefix() {
            String input = "```json\n{\"key\": \"value\"}\n```";
            assertEquals("{\"key\": \"value\"}",
                    JsonParsingUtils.cleanJsonResponse(input));
        }

        @Test
        @DisplayName("Removes plain ``` prefix")
        void removesPlainCodeBlockPrefix() {
            String input = "```\n{\"key\": \"value\"}\n```";
            assertEquals("{\"key\": \"value\"}",
                    JsonParsingUtils.cleanJsonResponse(input));
        }

        @Test
        @DisplayName("Removes only suffix when no prefix")
        void removesOnlySuffix() {
            String input = "{\"key\": \"value\"}```";
            assertEquals("{\"key\": \"value\"}",
                    JsonParsingUtils.cleanJsonResponse(input));
        }
    }

    @Nested
    @DisplayName("parseJson")
    class ParseJson {

        @Test
        @DisplayName("Parses clean JSON")
        void parsesCleanJson() throws JsonProcessingException {
            JsonNode node = JsonParsingUtils.parseJson("{\"name\": \"test\"}");
            assertEquals("test", node.get("name").asText());
        }

        @Test
        @DisplayName("Parses JSON wrapped in code blocks")
        void parsesWrappedJson() throws JsonProcessingException {
            String input = "```json\n{\"name\": \"test\"}\n```";
            JsonNode node = JsonParsingUtils.parseJson(input);
            assertEquals("test", node.get("name").asText());
        }

        @Test
        @DisplayName("Parses null to empty object")
        void parsesNullToEmptyObject() throws JsonProcessingException {
            JsonNode node = JsonParsingUtils.parseJson(null);
            assertTrue(node.isEmpty());
        }

        @Test
        @DisplayName("Uses custom ObjectMapper")
        void usesCustomMapper() throws JsonProcessingException {
            ObjectMapper customMapper = new ObjectMapper();
            JsonNode node = JsonParsingUtils.parseJson("{\"key\": 123}", customMapper);
            assertEquals(123, node.get("key").asInt());
        }
    }

    @Nested
    @DisplayName("getStringOrDefault")
    class GetStringOrDefault {

        @Test
        @DisplayName("Returns value when field exists")
        void returnsValueWhenExists() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": \"test\"}");
            assertEquals("test", JsonParsingUtils.getStringOrDefault(node, "name", "default"));
        }

        @Test
        @DisplayName("Returns default when field is missing")
        void returnsDefaultWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": \"value\"}");
            assertEquals("default", JsonParsingUtils.getStringOrDefault(node, "name", "default"));
        }

        @Test
        @DisplayName("Returns default when field is null")
        void returnsDefaultWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": null}");
            assertEquals("default", JsonParsingUtils.getStringOrDefault(node, "name", "default"));
        }
    }

    @Nested
    @DisplayName("getIntOrDefault")
    class GetIntOrDefault {

        @Test
        @DisplayName("Returns value when field exists")
        void returnsValueWhenExists() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"count\": 42}");
            assertEquals(42, JsonParsingUtils.getIntOrDefault(node, "count", 0));
        }

        @Test
        @DisplayName("Returns default when field is missing")
        void returnsDefaultWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": 10}");
            assertEquals(0, JsonParsingUtils.getIntOrDefault(node, "count", 0));
        }

        @Test
        @DisplayName("Returns default when field is null")
        void returnsDefaultWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"count\": null}");
            assertEquals(-1, JsonParsingUtils.getIntOrDefault(node, "count", -1));
        }
    }

    @Nested
    @DisplayName("getDoubleOrDefault")
    class GetDoubleOrDefault {

        @Test
        @DisplayName("Returns value when field exists")
        void returnsValueWhenExists() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"score\": 0.85}");
            assertEquals(0.85, JsonParsingUtils.getDoubleOrDefault(node, "score", 0.0), 0.001);
        }

        @Test
        @DisplayName("Returns default when field is missing")
        void returnsDefaultWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": 0.5}");
            assertEquals(0.5, JsonParsingUtils.getDoubleOrDefault(node, "score", 0.5), 0.001);
        }

        @Test
        @DisplayName("Returns default when field is null")
        void returnsDefaultWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"score\": null}");
            assertEquals(1.0, JsonParsingUtils.getDoubleOrDefault(node, "score", 1.0), 0.001);
        }
    }

    @Nested
    @DisplayName("getBooleanOrDefault")
    class GetBooleanOrDefault {

        @Test
        @DisplayName("Returns value when field exists")
        void returnsValueWhenExists() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"active\": true}");
            assertTrue(JsonParsingUtils.getBooleanOrDefault(node, "active", false));
        }

        @Test
        @DisplayName("Returns default when field is missing")
        void returnsDefaultWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": false}");
            assertTrue(JsonParsingUtils.getBooleanOrDefault(node, "active", true));
        }

        @Test
        @DisplayName("Returns default when field is null")
        void returnsDefaultWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"active\": null}");
            assertFalse(JsonParsingUtils.getBooleanOrDefault(node, "active", false));
        }
    }

    @Nested
    @DisplayName("getRequiredString")
    class GetRequiredString {

        @Test
        @DisplayName("Returns value when field exists")
        void returnsValueWhenExists() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": \"test\"}");
            assertEquals("test", JsonParsingUtils.getRequiredString(node, "name"));
        }

        @Test
        @DisplayName("Throws when field is missing")
        void throwsWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": \"value\"}");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> JsonParsingUtils.getRequiredString(node, "name"));
            assertTrue(ex.getMessage().contains("name"));
        }

        @Test
        @DisplayName("Throws when field is null")
        void throwsWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": null}");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> JsonParsingUtils.getRequiredString(node, "name"));
            assertTrue(ex.getMessage().contains("name"));
        }
    }

    @Nested
    @DisplayName("parseStringArray")
    class ParseStringArray {

        @Test
        @DisplayName("Parses array of strings")
        void parsesArrayOfStrings() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"items\": [\"one\", \"two\", \"three\"]}");
            List<String> result = JsonParsingUtils.parseStringArray(node, "items");
            assertEquals(List.of("one", "two", "three"), result);
        }

        @Test
        @DisplayName("Returns empty list when field is missing")
        void returnsEmptyWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": []}");
            List<String> result = JsonParsingUtils.parseStringArray(node, "items");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Skips blank values")
        void skipsBlankValues() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"items\": [\"one\", \"\", \"  \", \"two\"]}");
            List<String> result = JsonParsingUtils.parseStringArray(node, "items");
            assertEquals(List.of("one", "two"), result);
        }

        @Test
        @DisplayName("Returns empty list for non-array field")
        void returnsEmptyForNonArray() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"items\": \"not an array\"}");
            List<String> result = JsonParsingUtils.parseStringArray(node, "items");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("parseStringMap")
    class ParseStringMap {

        @Test
        @DisplayName("Parses object to map")
        void parsesObjectToMap() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"glossary\": {\"API\": \"Application Programming Interface\", \"REST\": \"Representational State Transfer\"}}");
            Map<String, String> result = JsonParsingUtils.parseStringMap(node, "glossary");
            assertEquals(2, result.size());
            assertEquals("Application Programming Interface", result.get("API"));
            assertEquals("Representational State Transfer", result.get("REST"));
        }

        @Test
        @DisplayName("Returns empty map when field is missing")
        void returnsEmptyWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": {}}");
            Map<String, String> result = JsonParsingUtils.parseStringMap(node, "glossary");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Returns empty map for non-object field")
        void returnsEmptyForNonObject() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"glossary\": [\"not\", \"an\", \"object\"]}");
            Map<String, String> result = JsonParsingUtils.parseStringMap(node, "glossary");
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("hasRequiredField")
    class HasRequiredField {

        @Test
        @DisplayName("Returns true for non-blank text field")
        void returnsTrueForNonBlankText() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": \"value\"}");
            assertTrue(JsonParsingUtils.hasRequiredField(node, "name"));
        }

        @Test
        @DisplayName("Returns false for blank text field")
        void returnsFalseForBlankText() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": \"   \"}");
            assertFalse(JsonParsingUtils.hasRequiredField(node, "name"));
        }

        @Test
        @DisplayName("Returns true for non-empty array")
        void returnsTrueForNonEmptyArray() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"items\": [\"one\"]}");
            assertTrue(JsonParsingUtils.hasRequiredField(node, "items"));
        }

        @Test
        @DisplayName("Returns false for empty array")
        void returnsFalseForEmptyArray() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"items\": []}");
            assertFalse(JsonParsingUtils.hasRequiredField(node, "items"));
        }

        @Test
        @DisplayName("Returns false when field is missing")
        void returnsFalseWhenMissing() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"other\": \"value\"}");
            assertFalse(JsonParsingUtils.hasRequiredField(node, "name"));
        }

        @Test
        @DisplayName("Returns false when field is null")
        void returnsFalseWhenNull() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"name\": null}");
            assertFalse(JsonParsingUtils.hasRequiredField(node, "name"));
        }

        @Test
        @DisplayName("Returns true for numeric field")
        void returnsTrueForNumericField() throws JsonProcessingException {
            JsonNode node = mapper.readTree("{\"count\": 42}");
            assertTrue(JsonParsingUtils.hasRequiredField(node, "count"));
        }
    }
}
