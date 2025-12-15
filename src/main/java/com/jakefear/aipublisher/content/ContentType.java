package com.jakefear.aipublisher.content;

/**
 * Types of content that can be generated, each with distinct structure and purpose.
 *
 * <h2>Adding a New ContentType</h2>
 * <p>When adding a new value to this enum, you <b>MUST</b> also update:</p>
 * <ol>
 *   <li>{@link ContentTypeTemplate} - Add a template entry to {@code TEMPLATES}</li>
 *   <li>{@link com.jakefear.aipublisher.cli.strategy.ContentTypeQuestionStrategy} - Add the type
 *       to an existing strategy's {@code getApplicableTypes()} or create a new strategy</li>
 * </ol>
 *
 * <p>Both locations have fail-fast validation that will cause startup failures if a
 * ContentType is missing, ensuring you cannot forget to update them.</p>
 *
 * @see ContentTypeTemplate
 * @see com.jakefear.aipublisher.cli.strategy.ContentTypeQuestionStrategyRegistry
 */
public enum ContentType {

    /**
     * Concept: Explains what something is.
     * Structure: Definition → Context → Details → Examples
     */
    CONCEPT("Concept", "Explains what something is and why it matters",
            800, 1500, true),

    /**
     * Tutorial: Teaches how to do something step by step.
     * Structure: Goal → Prerequisites → Steps → Verification → Next Steps
     */
    TUTORIAL("Tutorial", "Step-by-step guide to accomplish a task",
            1000, 2000, true),

    /**
     * Reference: Provides lookup information.
     * Structure: Organized data, tables, specifications
     */
    REFERENCE("Reference", "Lookup information for quick reference",
            400, 1200, false),

    /**
     * Guide: Provides decision support and best practices.
     * Structure: Context → Options → Trade-offs → Recommendations
     */
    GUIDE("Guide", "Decision support with options and trade-offs",
            1500, 2500, true),

    /**
     * Comparison: Helps choose between options.
     * Structure: Criteria → Analysis → Summary Table → Recommendation
     */
    COMPARISON("Comparison", "Analyzes alternatives to help choose",
            1000, 1500, true),

    /**
     * Troubleshooting: Helps solve problems.
     * Structure: Symptoms → Causes → Solutions → Prevention
     */
    TROUBLESHOOTING("Troubleshooting", "Problem diagnosis and solutions",
            500, 1000, false),

    /**
     * Overview: Introduces a topic area.
     * Structure: Big Picture → Components → Where to Go Next
     */
    OVERVIEW("Overview", "High-level introduction to a topic area",
            600, 1000, true),

    /**
     * Definition: Brief explanation of a term or concept.
     * Structure: Term → Definition → Context → See Also
     * Used for stub pages to fill gaps in wiki coverage.
     */
    DEFINITION("Definition", "Brief definition of a term or concept",
            100, 250, false);

    private final String displayName;
    private final String description;
    private final int minWordCount;
    private final int maxWordCount;
    private final boolean requiresExamples;

    ContentType(String displayName, String description, int minWordCount, int maxWordCount, boolean requiresExamples) {
        this.displayName = displayName;
        this.description = description;
        this.minWordCount = minWordCount;
        this.maxWordCount = maxWordCount;
        this.requiresExamples = requiresExamples;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getMinWordCount() {
        return minWordCount;
    }

    public int getMaxWordCount() {
        return maxWordCount;
    }

    public int getDefaultWordCount() {
        return (minWordCount + maxWordCount) / 2;
    }

    public boolean requiresExamples() {
        return requiresExamples;
    }

    /**
     * Parse content type from string, case-insensitive.
     */
    public static ContentType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (ContentType type : values()) {
                if (type.displayName.equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
            return null;
        }
    }
}
