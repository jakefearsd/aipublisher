package com.jakefear.aipublisher.discovery;

/**
 * Controls how deeply relationship analysis is performed.
 */
public enum RelationshipDepth {
    /**
     * Only analyze relationships between core/high-priority topics.
     * Fastest, lowest cost.
     */
    CORE_ONLY("Core Only", "Only core topic relationships"),

    /**
     * Analyze relationships for important topics (relevance >= 0.7).
     * Good balance of coverage and cost.
     */
    IMPORTANT("Important", "Relationships for important topics"),

    /**
     * Analyze all topic relationships.
     * Most thorough, highest cost.
     */
    ALL("All", "Complete relationship analysis");

    private final String displayName;
    private final String description;

    RelationshipDepth(String displayName, String description) {
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
     * Get the minimum relevance score for topics to be included in relationship analysis.
     */
    public double getMinRelevanceThreshold() {
        return switch (this) {
            case CORE_ONLY -> 0.9;
            case IMPORTANT -> 0.7;
            case ALL -> 0.0;
        };
    }
}
