package com.jakefear.aipublisher.seealso;

/**
 * Types of "See Also" relationships between wiki pages.
 */
public enum SeeAlsoType {
    /**
     * Parent or broader topic (e.g., Programming from Java)
     */
    BROADER("Broader topics", "Parent or more general topics"),

    /**
     * Child or more specific topic (e.g., Spring Boot from Java)
     */
    NARROWER("Narrower topics", "More specific subtopics"),

    /**
     * Related topic at same level (e.g., Python from Java)
     */
    RELATED("Related topics", "Topics at similar level of specificity"),

    /**
     * Tutorial or how-to (e.g., "Getting Started with Java")
     */
    TUTORIAL("Tutorials", "Step-by-step guides and tutorials"),

    /**
     * Reference documentation (e.g., "Java API Reference")
     */
    REFERENCE("References", "Reference documentation and API guides"),

    /**
     * Comparison with similar topic (e.g., "Java vs Python")
     */
    COMPARISON("Comparisons", "Comparisons with similar topics"),

    /**
     * External link to authoritative source
     */
    EXTERNAL("External resources", "External authoritative resources");

    private final String displayName;
    private final String description;

    SeeAlsoType(String displayName, String description) {
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
     * Whether this type should be shown in the main "See Also" section.
     */
    public boolean isMainSection() {
        return this == RELATED || this == BROADER || this == NARROWER;
    }

    /**
     * Whether this type represents internal wiki links.
     */
    public boolean isInternal() {
        return this != EXTERNAL;
    }

    /**
     * Get the appropriate heading for a group of this type.
     */
    public String getSectionHeading() {
        return switch (this) {
            case BROADER -> "Broader Topics";
            case NARROWER -> "Subtopics";
            case RELATED -> "Related Topics";
            case TUTORIAL -> "Tutorials";
            case REFERENCE -> "References";
            case COMPARISON -> "Comparisons";
            case EXTERNAL -> "External Links";
        };
    }
}
