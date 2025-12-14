package com.jakefear.aipublisher.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for parsing JSON responses from AI models.
 * <p>
 * AI models sometimes wrap JSON in markdown code blocks (```json...```)
 * or include other formatting. This class provides methods to clean and
 * parse such responses safely.
 */
public final class JsonParsingUtils {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private JsonParsingUtils() {
        // Utility class, not instantiable
    }

    /**
     * Clean a JSON response that might be wrapped in markdown code blocks.
     * <p>
     * Handles common cases:
     * - ```json ... ```
     * - ``` ... ```
     * - Leading/trailing whitespace
     *
     * @param response The raw response from an AI model
     * @return Cleaned JSON string, or "{}" if input is null
     */
    public static String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }

        String cleaned = response.trim();

        // Remove markdown code block wrapper if present
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }

    /**
     * Clean and parse a JSON response into a JsonNode.
     *
     * @param response The raw response from an AI model
     * @return Parsed JsonNode
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static JsonNode parseJson(String response) throws JsonProcessingException {
        return parseJson(response, DEFAULT_MAPPER);
    }

    /**
     * Clean and parse a JSON response using a custom ObjectMapper.
     *
     * @param response The raw response from an AI model
     * @param mapper Custom ObjectMapper to use
     * @return Parsed JsonNode
     * @throws JsonProcessingException if JSON parsing fails
     */
    public static JsonNode parseJson(String response, ObjectMapper mapper) throws JsonProcessingException {
        String cleaned = cleanJsonResponse(response);
        return mapper.readTree(cleaned);
    }

    /**
     * Get a string from a JSON node, with a default if missing or null.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return Field value or default
     */
    public static String getStringOrDefault(JsonNode node, String field, String defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asText(defaultValue);
    }

    /**
     * Get an int from a JSON node, with a default if missing or null.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return Field value or default
     */
    public static int getIntOrDefault(JsonNode node, String field, int defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asInt(defaultValue);
    }

    /**
     * Get a double from a JSON node, with a default if missing or null.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return Field value or default
     */
    public static double getDoubleOrDefault(JsonNode node, String field, double defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asDouble(defaultValue);
    }

    /**
     * Get a boolean from a JSON node, with a default if missing or null.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return Field value or default
     */
    public static boolean getBooleanOrDefault(JsonNode node, String field, boolean defaultValue) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            return defaultValue;
        }
        return fieldNode.asBoolean(defaultValue);
    }

    /**
     * Get a required string from a JSON node.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @return Field value
     * @throws IllegalArgumentException if field is missing or null
     */
    public static String getRequiredString(JsonNode node, String field) {
        JsonNode fieldNode = node.get(field);
        if (fieldNode == null || fieldNode.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return fieldNode.asText();
    }

    /**
     * Parse a JSON array field into a list of strings.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @return List of strings, empty if field is missing or not an array
     */
    public static List<String> parseStringArray(JsonNode root, String fieldName) {
        List<String> items = new ArrayList<>();
        JsonNode arrayNode = root.get(fieldName);
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode itemNode : arrayNode) {
                String item = itemNode.asText();
                if (item != null && !item.isBlank()) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    /**
     * Parse a JSON object field into a string-to-string map.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @return Map of string keys to string values, empty if field is missing
     */
    public static Map<String, String> parseStringMap(JsonNode root, String fieldName) {
        Map<String, String> result = new LinkedHashMap<>();
        JsonNode mapNode = root.get(fieldName);
        if (mapNode != null && mapNode.isObject()) {
            mapNode.fields().forEachRemaining(entry ->
                    result.put(entry.getKey(), entry.getValue().asText()));
        }
        return result;
    }

    /**
     * Check if a required field exists and is not blank.
     *
     * @param root The root JSON node
     * @param fieldName The field name to check
     * @return true if field exists and has non-blank content
     */
    public static boolean hasRequiredField(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return false;
        }
        if (node.isTextual()) {
            return !node.asText().isBlank();
        }
        if (node.isArray()) {
            return !node.isEmpty();
        }
        return true;
    }
}
