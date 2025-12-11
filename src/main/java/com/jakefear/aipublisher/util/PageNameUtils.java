package com.jakefear.aipublisher.util;

/**
 * Utility methods for generating and formatting wiki page names.
 * Consolidates string manipulation logic for page name generation.
 */
public final class PageNameUtils {

    private PageNameUtils() {
        // Utility class - no instantiation
    }

    /**
     * Convert a string to CamelCase for use as a wiki page name.
     * Removes spaces, hyphens, underscores and non-alphanumeric characters,
     * capitalizing the first letter of each word.
     *
     * @param input The input string (e.g., "apache kafka", "My-Topic_Name")
     * @return CamelCase page name (e.g., "ApacheKafka", "MyTopicName"),
     *         or empty string if input is null/blank
     */
    public static String toCamelCase(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;

        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '_') {
                capitalizeNext = true;
            } else if (Character.isLetterOrDigit(c)) {
                if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
            // Skip non-alphanumeric characters (except separators which set capitalizeNext)
        }

        return result.toString();
    }

    /**
     * Convert a string to CamelCase with a default fallback for invalid input.
     *
     * @param input The input string
     * @param defaultValue The value to return if input is null/blank
     * @return CamelCase page name or the default value
     */
    public static String toCamelCaseOrDefault(String input, String defaultValue) {
        String result = toCamelCase(input);
        return result.isEmpty() ? defaultValue : result;
    }
}
