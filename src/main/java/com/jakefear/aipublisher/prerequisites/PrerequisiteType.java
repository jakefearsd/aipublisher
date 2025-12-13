package com.jakefear.aipublisher.prerequisites;

/**
 * Types of prerequisites for an article.
 */
public enum PrerequisiteType {

    /**
     * Hard prerequisite - required knowledge without which the article
     * will not make sense. Reader should read this first.
     */
    HARD("Required", "Must understand this before proceeding"),

    /**
     * Soft prerequisite - helpful background knowledge that will improve
     * understanding but isn't strictly required.
     */
    SOFT("Recommended", "Helpful background knowledge"),

    /**
     * Assumed knowledge - basic concepts assumed known by the target audience.
     * Only mentioned for clarity, not linked.
     */
    ASSUMED("Assumed", "Basic knowledge assumed for this audience");

    private final String displayName;
    private final String description;

    PrerequisiteType(String displayName, String description) {
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
     * Check if this prerequisite type should be prominently displayed.
     */
    public boolean isProminent() {
        return this == HARD;
    }

    /**
     * Check if this prerequisite type should be linked to a wiki page.
     */
    public boolean shouldLink() {
        return this == HARD || this == SOFT;
    }
}
