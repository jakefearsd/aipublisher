package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared utility methods for parsing JSON responses from agents.
 * Consolidates common parsing patterns to eliminate duplication.
 */
public final class JsonParsingUtils {

    private JsonParsingUtils() {
        // Utility class - no instantiation
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
     * Get a string value with a default fallback.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @param defaultValue Value to return if field is missing or null
     * @return The string value or default
     */
    public static String getStringOrDefault(JsonNode root, String fieldName, String defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asText(defaultValue);
    }

    /**
     * Get an integer value with a default fallback.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @param defaultValue Value to return if field is missing or invalid
     * @return The integer value or default
     */
    public static int getIntOrDefault(JsonNode root, String fieldName, int defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asInt(defaultValue);
    }

    /**
     * Get a double value with a default fallback.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @param defaultValue Value to return if field is missing or invalid
     * @return The double value or default
     */
    public static double getDoubleOrDefault(JsonNode root, String fieldName, double defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asDouble(defaultValue);
    }

    /**
     * Get a boolean value with a default fallback.
     *
     * @param root The root JSON node
     * @param fieldName The field name to extract
     * @param defaultValue Value to return if field is missing
     * @return The boolean value or default
     */
    public static boolean getBooleanOrDefault(JsonNode root, String fieldName, boolean defaultValue) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return defaultValue;
        }
        return node.asBoolean(defaultValue);
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
