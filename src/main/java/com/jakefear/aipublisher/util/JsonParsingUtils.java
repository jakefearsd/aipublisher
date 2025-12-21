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
     * - Literal newlines inside JSON strings (escaped to \n)
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

        cleaned = cleaned.trim();

        // Escape literal newlines inside JSON strings
        // Some LLMs put actual newlines in string values instead of \n
        cleaned = escapeNewlinesInJsonStrings(cleaned);

        return cleaned;
    }

    /**
     * Escape literal newlines that appear inside JSON string values.
     * <p>
     * JSON strings cannot contain literal newlines - they must be escaped as \n.
     * Some LLMs produce invalid JSON with actual newlines in strings.
     * This method fixes that by escaping newlines within quoted strings.
     *
     * @param json The JSON string to fix
     * @return JSON with properly escaped newlines in strings
     */
    public static String escapeNewlinesInJsonStrings(String json) {
        if (json == null || json.isEmpty()) {
            return json;
        }

        StringBuilder result = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                // Previous char was backslash, this char is escaped
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escaped = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                result.append(c);
                continue;
            }

            if (inString && c == '\n') {
                // Literal newline inside string - escape it
                result.append("\\n");
            } else if (inString && c == '\r') {
                // Literal carriage return inside string - escape it
                result.append("\\r");
            } else if (inString && c == '\t') {
                // Literal tab inside string - escape it
                result.append("\\t");
            } else {
                result.append(c);
            }
        }

        return result.toString();
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
     * Get a multi-line string from a JSON node, normalizing escaped newlines.
     * <p>
     * Some LLMs produce literal "\n" text instead of actual newlines when asked
     * to include multi-line content in JSON. This method converts those to real newlines.
     *
     * @param node The parent node
     * @param field Field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return Field value with normalized newlines
     */
    public static String getMultilineString(JsonNode node, String field, String defaultValue) {
        String value = getStringOrDefault(node, field, defaultValue);
        return normalizeNewlines(value);
    }

    /**
     * Normalize escaped newlines in a string.
     * Converts literal "\n" (backslash-n) to actual newline characters.
     *
     * @param text The text to normalize
     * @return Text with normalized newlines
     */
    public static String normalizeNewlines(String text) {
        if (text == null) {
            return null;
        }
        // Replace literal \n (two characters) with actual newline
        // Also handle \r\n for Windows-style line endings
        return text.replace("\\r\\n", "\n")
                   .replace("\\n", "\n")
                   .replace("\\r", "\r");
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
