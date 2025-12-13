package com.jakefear.aipublisher.examples;

/**
 * Types of examples that can be included in articles.
 */
public enum ExampleType {

    /**
     * Minimal examples that show basic syntax or usage.
     * Used for reference documentation and quick demonstrations.
     */
    MINIMAL("Minimal", "Basic syntax or usage demonstration"),

    /**
     * Realistic examples that show practical real-world usage.
     * Used for tutorials and guides.
     */
    REALISTIC("Realistic", "Practical real-world usage scenario"),

    /**
     * Progressive examples that build on each other.
     * Used for tutorials to show step-by-step learning.
     */
    PROGRESSIVE("Progressive", "Builds on previous examples"),

    /**
     * Anti-pattern examples showing what NOT to do.
     * Used for best practices and troubleshooting content.
     */
    ANTI_PATTERN("Anti-Pattern", "Shows incorrect approach to avoid"),

    /**
     * Comparison examples showing different approaches.
     * Used for comparison articles.
     */
    COMPARISON("Comparison", "Side-by-side approach comparison"),

    /**
     * Complete examples with full context.
     * Used for reference docs and guides.
     */
    COMPLETE("Complete", "Full working example with all context");

    private final String displayName;
    private final String description;

    ExampleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this type requires explanation of what NOT to do.
     */
    public boolean isNegativeExample() {
        return this == ANTI_PATTERN;
    }

    /**
     * Check if this type builds on previous examples.
     */
    public boolean isProgressive() {
        return this == PROGRESSIVE;
    }

    /**
     * Check if this type requires full context (imports, setup, etc.).
     */
    public boolean requiresFullContext() {
        return this == COMPLETE || this == REALISTIC;
    }
}
