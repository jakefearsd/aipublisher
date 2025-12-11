package com.jakefear.aipublisher.document;

/**
 * Recommended action from the fact checker.
 */
public enum RecommendedAction {
    /**
     * Article passes fact-check and can proceed.
     */
    APPROVE,

    /**
     * Article needs revisions to address identified issues.
     */
    REVISE,

    /**
     * Article has serious factual problems and should be rejected.
     */
    REJECT;

    /**
     * Parse a recommended action from a string (case-insensitive).
     */
    public static RecommendedAction fromString(String value) {
        if (value == null || value.isBlank()) {
            return REVISE;
        }
        return switch (value.toUpperCase().trim()) {
            case "APPROVE" -> APPROVE;
            case "REJECT" -> REJECT;
            default -> REVISE;
        };
    }
}
