package com.jakefear.aipublisher.document;

/**
 * Confidence level for fact-checking assessments.
 */
public enum ConfidenceLevel {
    /**
     * Low confidence - significant concerns about factual accuracy.
     */
    LOW(1),

    /**
     * Medium confidence - some concerns but generally acceptable.
     */
    MEDIUM(2),

    /**
     * High confidence - facts are well-supported and verified.
     */
    HIGH(3);

    private final int value;

    ConfidenceLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Check if this confidence level meets or exceeds the required minimum.
     */
    public boolean meetsMinimum(ConfidenceLevel minimum) {
        return this.value >= minimum.value;
    }

    /**
     * Parse a confidence level from a string (case-insensitive).
     */
    public static ConfidenceLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return LOW;
        }
        return switch (value.toUpperCase().trim()) {
            case "HIGH" -> HIGH;
            case "MEDIUM" -> MEDIUM;
            default -> LOW;
        };
    }
}
