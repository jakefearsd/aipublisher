package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.domain.RelationshipType;

import java.util.Objects;

/**
 * A suggested relationship between topics from the AI discovery process.
 */
public record RelationshipSuggestion(
        String sourceTopicName,
        String targetTopicName,
        RelationshipType suggestedType,
        double confidence,
        String rationale
) {
    /**
     * Compact constructor with validation.
     */
    public RelationshipSuggestion {
        Objects.requireNonNull(sourceTopicName, "sourceTopicName cannot be null");
        Objects.requireNonNull(targetTopicName, "targetTopicName cannot be null");
        Objects.requireNonNull(suggestedType, "suggestedType cannot be null");

        if (sourceTopicName.equals(targetTopicName)) {
            throw new IllegalArgumentException("Cannot create self-referential relationship");
        }

        if (confidence < 0 || confidence > 1) confidence = 0.5;
        if (rationale == null) rationale = "";
    }

    /**
     * Create a simple relationship suggestion.
     */
    public static RelationshipSuggestion simple(
            String source,
            String target,
            RelationshipType type) {
        return new RelationshipSuggestion(source, target, type, 0.5, "");
    }

    /**
     * Create a relationship suggestion with confidence.
     */
    public static RelationshipSuggestion withConfidence(
            String source,
            String target,
            RelationshipType type,
            double confidence) {
        return new RelationshipSuggestion(source, target, type, confidence, "");
    }

    /**
     * Create a full relationship suggestion.
     */
    public static RelationshipSuggestion full(
            String source,
            String target,
            RelationshipType type,
            double confidence,
            String rationale) {
        return new RelationshipSuggestion(source, target, type, confidence, rationale);
    }

    /**
     * Get a human-readable description of this relationship.
     */
    public String describe() {
        return String.format("\"%s\" %s \"%s\"",
                sourceTopicName,
                suggestedType.getDisplayName().toLowerCase(),
                targetTopicName);
    }

    /**
     * Get a short display format.
     */
    public String toDisplayString() {
        String confidenceIndicator = confidence >= 0.8 ? "●" :
                confidence >= 0.5 ? "◐" : "○";
        return String.format("%s %s ──[%s]──> %s",
                confidenceIndicator,
                sourceTopicName,
                suggestedType.name(),
                targetTopicName);
    }

    /**
     * Check if this is a high-confidence suggestion.
     */
    public boolean isHighConfidence() {
        return confidence >= 0.8;
    }

    /**
     * Check if this relationship implies generation ordering.
     */
    public boolean impliesOrdering() {
        return suggestedType.impliesOrdering();
    }
}
