package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.domain.ComplexityLevel;

/**
 * Cost profile that controls resource usage during domain discovery and content generation.
 * Bundles multiple parameters into user-friendly presets that balance coverage vs. cost.
 *
 * @param name Display name for the profile
 * @param description Brief description of the profile's purpose
 * @param maxExpansionRounds Maximum number of topic expansion rounds
 * @param topicsPerRound How many topics to expand per round
 * @param suggestionsPerTopic Target number of suggestions per topic expansion
 * @param maxComplexity Maximum complexity level for generated content
 * @param wordCountMultiplier Multiplier applied to default word counts (0.6 = 60% of normal)
 * @param skipGapAnalysis Whether to skip the gap analysis phase
 * @param relationshipDepth How thoroughly to analyze topic relationships
 * @param autoAcceptThreshold Relevance threshold for auto-accepting suggestions in skip mode
 */
public record CostProfile(
        String name,
        String description,
        int maxExpansionRounds,
        int topicsPerRound,
        int suggestionsPerTopic,
        ComplexityLevel maxComplexity,
        double wordCountMultiplier,
        boolean skipGapAnalysis,
        RelationshipDepth relationshipDepth,
        double autoAcceptThreshold
) {
    /**
     * MINIMAL - Quick overview with minimal resource usage.
     * Best for: Prototyping, testing ideas, small personal wikis.
     * Estimated cost: $0.50-2 discovery, $5-15 content generation.
     */
    public static final CostProfile MINIMAL = new CostProfile(
            "Minimal",
            "Quick overview, ~5-10 topics, minimal depth. Best for prototyping.",
            1,      // maxExpansionRounds
            2,      // topicsPerRound
            4,      // suggestionsPerTopic (3-5)
            ComplexityLevel.INTERMEDIATE,
            0.6,    // wordCountMultiplier
            true,   // skipGapAnalysis
            RelationshipDepth.CORE_ONLY,
            0.9     // autoAcceptThreshold
    );

    /**
     * BALANCED - Good coverage with moderate resource usage.
     * Best for: Most wikis, documentation projects, team knowledge bases.
     * Estimated cost: $2-5 discovery, $30-75 content generation.
     */
    public static final CostProfile BALANCED = new CostProfile(
            "Balanced",
            "Good coverage, ~15-30 topics, moderate depth. Best for most projects.",
            3,      // maxExpansionRounds
            3,      // topicsPerRound
            7,      // suggestionsPerTopic (5-10)
            ComplexityLevel.ADVANCED,
            1.0,    // wordCountMultiplier
            false,  // skipGapAnalysis
            RelationshipDepth.IMPORTANT,
            0.8     // autoAcceptThreshold
    );

    /**
     * COMPREHENSIVE - Full coverage with thorough analysis.
     * Best for: Enterprise documentation, complete technical references.
     * Estimated cost: $5-15 discovery, $100-250 content generation.
     */
    public static final CostProfile COMPREHENSIVE = new CostProfile(
            "Comprehensive",
            "Full coverage, ~40-80 topics, thorough depth. Best for enterprise docs.",
            5,      // maxExpansionRounds
            5,      // topicsPerRound
            12,     // suggestionsPerTopic (8-15)
            ComplexityLevel.EXPERT,
            1.5,    // wordCountMultiplier
            false,  // skipGapAnalysis
            RelationshipDepth.ALL,
            0.7     // autoAcceptThreshold
    );

    /**
     * Get all predefined profiles.
     */
    public static CostProfile[] values() {
        return new CostProfile[]{MINIMAL, BALANCED, COMPREHENSIVE};
    }

    /**
     * Get a profile by name (case-insensitive).
     *
     * @param name The profile name
     * @return The matching profile, or null if not found
     */
    public static CostProfile fromName(String name) {
        if (name == null) return null;
        String normalized = name.trim().toUpperCase();
        return switch (normalized) {
            case "MINIMAL", "MIN", "1" -> MINIMAL;
            case "BALANCED", "BAL", "2" -> BALANCED;
            case "COMPREHENSIVE", "COMP", "FULL", "3" -> COMPREHENSIVE;
            default -> null;
        };
    }

    /**
     * Get the prompt range for suggestions (e.g., "3-5" for MINIMAL).
     */
    public String getSuggestionsRange() {
        int min = Math.max(suggestionsPerTopic - 2, 2);
        int max = suggestionsPerTopic + 2;
        return min + "-" + max;
    }

    /**
     * Calculate adjusted word count for a given complexity level.
     */
    public int getAdjustedWordCount(ComplexityLevel complexity) {
        // Cap at maxComplexity
        ComplexityLevel effectiveComplexity = complexity.getLevel() > maxComplexity.getLevel()
                ? maxComplexity
                : complexity;

        int baseWords = effectiveComplexity.getMinWords();
        return (int) (baseWords * wordCountMultiplier);
    }

    /**
     * Get estimated topic count range for this profile.
     */
    public String getEstimatedTopicRange() {
        int min = topicsPerRound * maxExpansionRounds;
        int max = topicsPerRound * maxExpansionRounds * suggestionsPerTopic / 2;
        return min + "-" + max;
    }

    /**
     * Get the cost estimate description.
     */
    public String getCostEstimate() {
        return switch (name) {
            case "Minimal" -> "$0.50-2 discovery, $5-15 content";
            case "Balanced" -> "$2-5 discovery, $30-75 content";
            case "Comprehensive" -> "$5-15 discovery, $100-250 content";
            default -> "varies";
        };
    }

    /**
     * Format for display in menus.
     */
    public String toDisplayString() {
        return String.format("%s - %s (~%s topics, %s)",
                name.toUpperCase(),
                description.split("\\.")[0],
                getEstimatedTopicRange(),
                getCostEstimate());
    }

    @Override
    public String toString() {
        return name;
    }
}
