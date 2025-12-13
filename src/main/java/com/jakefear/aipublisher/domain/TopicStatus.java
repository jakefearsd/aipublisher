package com.jakefear.aipublisher.domain;

/**
 * Status of a topic in the discovery process.
 */
public enum TopicStatus {
    /**
     * AI suggested this topic, awaiting user decision.
     */
    PROPOSED("Proposed", "Suggested by AI, awaiting review"),

    /**
     * User accepted this topic for inclusion.
     */
    ACCEPTED("Accepted", "Approved for inclusion in wiki"),

    /**
     * User rejected this topic.
     */
    REJECTED("Rejected", "Not relevant for this wiki"),

    /**
     * User deferred decision to later.
     */
    DEFERRED("Deferred", "Decision postponed, saved to backlog"),

    /**
     * Content has been generated for this topic.
     */
    GENERATED("Generated", "Content has been created"),

    /**
     * Content has been reviewed and published.
     */
    PUBLISHED("Published", "Content is live in wiki");

    private final String displayName;
    private final String description;

    TopicStatus(String displayName, String description) {
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
     * Whether this status represents an active topic (will be generated).
     */
    public boolean isActive() {
        return this == ACCEPTED || this == GENERATED || this == PUBLISHED;
    }

    /**
     * Whether this topic is still pending user decision.
     */
    public boolean isPending() {
        return this == PROPOSED || this == DEFERRED;
    }

    /**
     * Whether content exists for this topic.
     */
    public boolean hasContent() {
        return this == GENERATED || this == PUBLISHED;
    }
}
