package com.jakefear.aipublisher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates content for unexpected foreign characters in English text.
 * Detects hallucinated foreign text (Chinese, Russian, Arabic, etc.)
 * mixed with English content.
 */
public class LanguageValidator {

    // Patterns for common hallucination languages
    private static final Pattern CHINESE = Pattern.compile("[\\u4E00-\\u9FFF]+");
    private static final Pattern CYRILLIC = Pattern.compile("[\\u0400-\\u04FF]+");
    private static final Pattern ARABIC = Pattern.compile("[\\u0600-\\u06FF]+");
    private static final Pattern KOREAN = Pattern.compile("[\\uAC00-\\uD7AF]+");
    private static final Pattern HEBREW = Pattern.compile("[\\u0590-\\u05FF]+");
    private static final Pattern JAPANESE = Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF]+");

    /**
     * Result of validating content for foreign characters.
     */
    public record ValidationResult(
            boolean valid,
            List<ForeignTextIssue> issues
    ) {
        public String getSummary() {
            if (valid) return "No foreign characters detected";
            StringBuilder sb = new StringBuilder();
            sb.append(issues.size()).append(" foreign text fragment(s) found: ");
            for (ForeignTextIssue issue : issues) {
                sb.append(issue.language()).append(" (\"")
                        .append(truncate(issue.text(), 20)).append("\"); ");
            }
            return sb.toString();
        }

        private static String truncate(String s, int max) {
            return s.length() <= max ? s : s.substring(0, max) + "...";
        }
    }

    /**
     * A single foreign text issue found in the content.
     */
    public record ForeignTextIssue(String language, String text, int position) {}

    /**
     * Validate content for foreign characters based on expected language.
     *
     * @param content The content to validate
     * @param expectedLanguage The expected language code (e.g., "en" for English)
     * @return ValidationResult with any issues found
     */
    public static ValidationResult validate(String content, String expectedLanguage) {
        if (content == null || content.isBlank()) {
            return new ValidationResult(true, List.of());
        }

        // For English content, flag any non-Latin scripts
        if ("en".equalsIgnoreCase(expectedLanguage) || expectedLanguage == null) {
            return validateEnglish(content);
        }

        return new ValidationResult(true, List.of());
    }

    private static ValidationResult validateEnglish(String content) {
        List<ForeignTextIssue> issues = new ArrayList<>();

        checkScript(content, CHINESE, "Chinese", issues);
        checkScript(content, CYRILLIC, "Cyrillic/Russian", issues);
        checkScript(content, ARABIC, "Arabic", issues);
        checkScript(content, KOREAN, "Korean", issues);
        checkScript(content, HEBREW, "Hebrew", issues);
        checkScript(content, JAPANESE, "Japanese", issues);

        return new ValidationResult(issues.isEmpty(), issues);
    }

    private static void checkScript(String content, Pattern pattern, String language,
                                    List<ForeignTextIssue> issues) {
        Matcher m = pattern.matcher(content);
        while (m.find()) {
            issues.add(new ForeignTextIssue(language, m.group(), m.start()));
        }
    }

    /**
     * Remove foreign text from content, replacing with empty string.
     *
     * @param content The content to clean
     * @return Content with foreign characters removed
     */
    public static String removeForeignText(String content) {
        if (content == null) return null;

        String result = content;
        result = CHINESE.matcher(result).replaceAll("");
        result = CYRILLIC.matcher(result).replaceAll("");
        result = ARABIC.matcher(result).replaceAll("");
        result = KOREAN.matcher(result).replaceAll("");
        result = HEBREW.matcher(result).replaceAll("");
        result = JAPANESE.matcher(result).replaceAll("");

        // Clean up resulting double spaces
        result = result.replaceAll("\\s{2,}", " ");

        return result;
    }
}
