package com.jakefear.aipublisher.cli.strategy;

/**
 * Represents a single question to ask during content type configuration.
 * Part of the Strategy Pattern for content-type-specific question handling.
 *
 * @param prompt The question text to display to the user
 * @param defaultValue Default value shown in brackets, or null for no default
 * @param target Where to store the answer (determines which field gets populated)
 */
public record ContentTypeQuestion(
        String prompt,
        String defaultValue,
        QuestionTarget target
) {
    /**
     * Create a question targeting the specific goal field.
     */
    public static ContentTypeQuestion forGoal(String prompt) {
        return new ContentTypeQuestion(prompt, null, QuestionTarget.SPECIFIC_GOAL);
    }

    /**
     * Create a question targeting the domain context field.
     */
    public static ContentTypeQuestion forContext(String prompt, String defaultValue) {
        return new ContentTypeQuestion(prompt, defaultValue, QuestionTarget.DOMAIN_CONTEXT);
    }

    /**
     * Create a question targeting the domain context field with no default.
     */
    public static ContentTypeQuestion forContext(String prompt) {
        return forContext(prompt, null);
    }

    /**
     * Create a question that adds to required sections.
     *
     * @param prompt The question text
     * @param sectionPrefix Prefix added before the answer (e.g., "Prerequisites: ")
     */
    public static ContentTypeQuestion forSection(String prompt, String sectionPrefix) {
        return new ContentTypeQuestion(prompt, sectionPrefix, QuestionTarget.REQUIRED_SECTION);
    }

    /**
     * Get the section prefix when target is REQUIRED_SECTION.
     * For REQUIRED_SECTION questions, the defaultValue field stores the prefix.
     */
    public String getSectionPrefix() {
        if (target == QuestionTarget.REQUIRED_SECTION) {
            return defaultValue != null ? defaultValue : "";
        }
        return "";
    }

    /**
     * Get the display default for prompts (null for REQUIRED_SECTION since defaultValue is the prefix).
     */
    public String getDisplayDefault() {
        if (target == QuestionTarget.REQUIRED_SECTION) {
            return null;
        }
        return defaultValue;
    }
}
