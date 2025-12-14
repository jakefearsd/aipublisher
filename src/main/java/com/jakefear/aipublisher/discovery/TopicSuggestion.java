package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.domain.ComplexityLevel;

import java.util.Objects;

/**
 * A suggested topic from the AI discovery process.
 * Contains the AI's analysis and recommendations before user curation.
 */
public record TopicSuggestion(
        String name,
        String description,
        String category,
        ContentType suggestedContentType,
        ComplexityLevel suggestedComplexity,
        int suggestedWordCount,
        double relevanceScore,
        String rationale,
        String sourceContext
) {
    /**
     * Compact constructor with validation.
     */
    public TopicSuggestion {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        // Defaults
        if (description == null) description = "";
        if (category == null) category = "";
        if (suggestedContentType == null) suggestedContentType = ContentType.CONCEPT;
        if (suggestedComplexity == null) suggestedComplexity = ComplexityLevel.INTERMEDIATE;
        if (suggestedWordCount <= 0) suggestedWordCount = suggestedComplexity.getMinWords();
        if (relevanceScore < 0 || relevanceScore > 1) relevanceScore = 0.5;
        if (rationale == null) rationale = "";
        if (sourceContext == null) sourceContext = "";
    }

    /**
     * Create a simple suggestion with minimal info.
     */
    public static TopicSuggestion simple(String name, String description) {
        return new TopicSuggestion(
                name, description, "", ContentType.CONCEPT,
                ComplexityLevel.INTERMEDIATE, 1000, 0.5, "", ""
        );
    }

    /**
     * Create a suggestion with full AI analysis.
     */
    public static TopicSuggestion analyzed(
            String name,
            String description,
            String category,
            ContentType contentType,
            ComplexityLevel complexity,
            double relevance,
            String rationale) {
        return new TopicSuggestion(
                name, description, category, contentType, complexity,
                complexity.getMinWords(), relevance, rationale, ""
        );
    }

    /**
     * Create a builder for this suggestion.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Get a relevance indicator for display.
     */
    public String getRelevanceIndicator() {
        int bars = (int) (relevanceScore * 10);
        return "█".repeat(bars) + "░".repeat(10 - bars);
    }

    /**
     * Get a short summary for display.
     */
    public String getSummary() {
        return String.format("%s (%s, %s, ~%d words)",
                name,
                suggestedContentType.getDisplayName(),
                suggestedComplexity.getDisplayName(),
                suggestedWordCount);
    }

    /**
     * Builder for TopicSuggestion.
     */
    public static class Builder {
        private final String name;
        private String description = "";
        private String category = "";
        private ContentType suggestedContentType = ContentType.CONCEPT;
        private ComplexityLevel suggestedComplexity = ComplexityLevel.INTERMEDIATE;
        private int suggestedWordCount = 1000;
        private double relevanceScore = 0.5;
        private String rationale = "";
        private String sourceContext = "";

        public Builder(String name) {
            this.name = name;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.suggestedContentType = contentType;
            return this;
        }

        public Builder complexity(ComplexityLevel complexity) {
            this.suggestedComplexity = complexity;
            this.suggestedWordCount = complexity.getMinWords();
            return this;
        }

        public Builder wordCount(int wordCount) {
            this.suggestedWordCount = wordCount;
            return this;
        }

        public Builder relevance(double relevance) {
            this.relevanceScore = relevance;
            return this;
        }

        public Builder rationale(String rationale) {
            this.rationale = rationale;
            return this;
        }

        public Builder sourceContext(String context) {
            this.sourceContext = context;
            return this;
        }

        public TopicSuggestion build() {
            return new TopicSuggestion(
                    name, description, category, suggestedContentType,
                    suggestedComplexity, suggestedWordCount, relevanceScore,
                    rationale, sourceContext
            );
        }
    }
}
