package com.jakefear.aipublisher.domain;

/**
 * Complexity level for topics, indicating required reader expertise.
 */
public enum ComplexityLevel {
    /**
     * No prior knowledge required.
     */
    BEGINNER(1, "Beginner", "No prior knowledge required"),

    /**
     * Basic familiarity with the domain expected.
     */
    INTERMEDIATE(2, "Intermediate", "Basic familiarity expected"),

    /**
     * Solid understanding of fundamentals required.
     */
    ADVANCED(3, "Advanced", "Strong foundation required"),

    /**
     * Deep expertise required.
     */
    EXPERT(4, "Expert", "Deep expertise required");

    private final int level;
    private final String displayName;
    private final String description;

    ComplexityLevel(int level, String displayName, String description) {
        this.level = level;
        this.displayName = displayName;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this level is at least as complex as another.
     */
    public boolean isAtLeast(ComplexityLevel other) {
        return this.level >= other.level;
    }

    /**
     * Check if this level is simpler than another.
     */
    public boolean isSimplerThan(ComplexityLevel other) {
        return this.level < other.level;
    }

    /**
     * Get suggested word count range for this complexity.
     */
    public int getMinWords() {
        return switch (this) {
            case BEGINNER -> 500;
            case INTERMEDIATE -> 800;
            case ADVANCED -> 1200;
            case EXPERT -> 1500;
        };
    }

    /**
     * Get suggested word count range for this complexity.
     */
    public int getMaxWords() {
        return switch (this) {
            case BEGINNER -> 1200;
            case INTERMEDIATE -> 2000;
            case ADVANCED -> 3000;
            case EXPERT -> 5000;
        };
    }
}
