package com.jakefear.aipublisher.discovery;

/**
 * Phases in the interactive domain discovery process.
 */
public enum DiscoveryPhase {
    /**
     * Initial phase - gathering seed topics.
     */
    SEED_INPUT("Seed Input", "Provide initial topics to explore"),

    /**
     * Setting scope boundaries before expansion.
     */
    SCOPE_SETUP("Scope Setup", "Define assumed knowledge, exclusions, and focus areas"),

    /**
     * Expanding topics from seeds.
     */
    TOPIC_EXPANSION("Topic Expansion", "Discover and curate related topics"),

    /**
     * Mapping relationships between topics.
     */
    RELATIONSHIP_MAPPING("Relationship Mapping", "Define how topics relate to each other"),

    /**
     * Analyzing for gaps in coverage.
     */
    GAP_ANALYSIS("Gap Analysis", "Identify missing topics and connections"),

    /**
     * Calibrating depth for each topic.
     */
    DEPTH_CALIBRATION("Depth Calibration", "Set word counts and detail levels"),

    /**
     * Prioritizing topics for generation.
     */
    PRIORITIZATION("Prioritization", "Assign generation priorities"),

    /**
     * Final review before saving.
     */
    REVIEW("Review", "Review and finalize the topic universe"),

    /**
     * Session complete.
     */
    COMPLETE("Complete", "Discovery session finished");

    private final String displayName;
    private final String description;

    DiscoveryPhase(String displayName, String description) {
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
     * Get the next phase in the workflow.
     */
    public DiscoveryPhase next() {
        return switch (this) {
            case SEED_INPUT -> SCOPE_SETUP;
            case SCOPE_SETUP -> TOPIC_EXPANSION;
            case TOPIC_EXPANSION -> RELATIONSHIP_MAPPING;
            case RELATIONSHIP_MAPPING -> GAP_ANALYSIS;
            case GAP_ANALYSIS -> DEPTH_CALIBRATION;
            case DEPTH_CALIBRATION -> PRIORITIZATION;
            case PRIORITIZATION -> REVIEW;
            case REVIEW, COMPLETE -> COMPLETE;
        };
    }

    /**
     * Get the previous phase in the workflow.
     */
    public DiscoveryPhase previous() {
        return switch (this) {
            case SEED_INPUT, COMPLETE -> SEED_INPUT;
            case SCOPE_SETUP -> SEED_INPUT;
            case TOPIC_EXPANSION -> SCOPE_SETUP;
            case RELATIONSHIP_MAPPING -> TOPIC_EXPANSION;
            case GAP_ANALYSIS -> RELATIONSHIP_MAPPING;
            case DEPTH_CALIBRATION -> GAP_ANALYSIS;
            case PRIORITIZATION -> DEPTH_CALIBRATION;
            case REVIEW -> PRIORITIZATION;
        };
    }

    /**
     * Whether this phase can be skipped.
     */
    public boolean isSkippable() {
        return this == SCOPE_SETUP || this == DEPTH_CALIBRATION;
    }

    /**
     * Whether this phase is terminal.
     */
    public boolean isTerminal() {
        return this == COMPLETE;
    }
}
