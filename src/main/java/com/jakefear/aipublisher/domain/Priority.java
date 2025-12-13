package com.jakefear.aipublisher.domain;

/**
 * Priority level for topic generation ordering.
 */
public enum Priority {
    /**
     * Core topics that must be generated first.
     * Other topics depend on these.
     */
    MUST_HAVE(1, "Must Have", "Core topic, generate first"),

    /**
     * Important topics that should be generated.
     * Enhance the core content.
     */
    SHOULD_HAVE(2, "Should Have", "Important, generate second"),

    /**
     * Supplementary topics to generate if resources allow.
     * Nice additions but not essential.
     */
    NICE_TO_HAVE(3, "Nice to Have", "Supplementary, generate last"),

    /**
     * Topics saved for future consideration.
     * Not planned for current generation cycle.
     */
    BACKLOG(4, "Backlog", "Saved for future");

    private final int order;
    private final String displayName;
    private final String description;

    Priority(int order, String displayName, String description) {
        this.order = order;
        this.displayName = displayName;
        this.description = description;
    }

    public int getOrder() {
        return order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Whether this priority level should be generated in current cycle.
     */
    public boolean shouldGenerate() {
        return this != BACKLOG;
    }

    /**
     * Check if this priority is higher (more important) than another.
     */
    public boolean isHigherThan(Priority other) {
        return this.order < other.order;
    }
}
