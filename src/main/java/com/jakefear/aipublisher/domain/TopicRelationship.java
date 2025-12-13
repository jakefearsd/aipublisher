package com.jakefear.aipublisher.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * A relationship between two topics in the knowledge domain.
 */
public record TopicRelationship(
        String id,
        String sourceTopicId,
        String targetTopicId,
        RelationshipType type,
        RelationshipStatus status,
        double confidence,
        String userNote,
        Instant createdAt,
        Instant modifiedAt
) {
    /**
     * Status of a relationship in the curation process.
     */
    public enum RelationshipStatus {
        SUGGESTED("Suggested", "AI suggested, awaiting confirmation"),
        CONFIRMED("Confirmed", "User confirmed this relationship"),
        REJECTED("Rejected", "User rejected this relationship"),
        MODIFIED("Modified", "User changed the relationship type");

        private final String displayName;
        private final String description;

        RelationshipStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        public boolean isActive() {
            return this == CONFIRMED || this == MODIFIED;
        }
    }

    /**
     * Compact constructor with validation.
     */
    public TopicRelationship {
        Objects.requireNonNull(sourceTopicId, "sourceTopicId cannot be null");
        Objects.requireNonNull(targetTopicId, "targetTopicId cannot be null");
        Objects.requireNonNull(type, "type cannot be null");

        if (sourceTopicId.equals(targetTopicId)) {
            throw new IllegalArgumentException("Topic cannot relate to itself");
        }

        // Generate ID if not provided
        if (id == null || id.isBlank()) {
            id = generateId(sourceTopicId, targetTopicId, type);
        }

        // Defaults
        if (status == null) status = RelationshipStatus.SUGGESTED;
        if (confidence < 0 || confidence > 1) confidence = 0.5;
        if (userNote == null) userNote = "";
        if (createdAt == null) createdAt = Instant.now();
        if (modifiedAt == null) modifiedAt = createdAt;
    }

    /**
     * Create a suggested relationship.
     */
    public static TopicRelationship suggested(
            String sourceTopicId,
            String targetTopicId,
            RelationshipType type,
            double confidence) {
        return new TopicRelationship(
                null,
                sourceTopicId,
                targetTopicId,
                type,
                RelationshipStatus.SUGGESTED,
                confidence,
                "",
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Create a confirmed relationship.
     */
    public static TopicRelationship confirmed(
            String sourceTopicId,
            String targetTopicId,
            RelationshipType type) {
        return new TopicRelationship(
                null,
                sourceTopicId,
                targetTopicId,
                type,
                RelationshipStatus.CONFIRMED,
                1.0,
                "",
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Generate a unique ID for this relationship.
     */
    public static String generateId(String sourceId, String targetId, RelationshipType type) {
        return sourceId + "_" + type.name() + "_" + targetId;
    }

    /**
     * Confirm this relationship.
     */
    public TopicRelationship confirm() {
        return new TopicRelationship(
                id, sourceTopicId, targetTopicId, type,
                RelationshipStatus.CONFIRMED, 1.0, userNote,
                createdAt, Instant.now()
        );
    }

    /**
     * Confirm with a different type.
     */
    public TopicRelationship confirmAs(RelationshipType newType) {
        return new TopicRelationship(
                generateId(sourceTopicId, targetTopicId, newType),
                sourceTopicId, targetTopicId, newType,
                RelationshipStatus.MODIFIED, 1.0, userNote,
                createdAt, Instant.now()
        );
    }

    /**
     * Reject this relationship.
     */
    public TopicRelationship reject() {
        return new TopicRelationship(
                id, sourceTopicId, targetTopicId, type,
                RelationshipStatus.REJECTED, confidence, userNote,
                createdAt, Instant.now()
        );
    }

    /**
     * Add a user note.
     */
    public TopicRelationship withNote(String note) {
        return new TopicRelationship(
                id, sourceTopicId, targetTopicId, type,
                status, confidence, note,
                createdAt, Instant.now()
        );
    }

    /**
     * Check if this relationship is active (should be used).
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * Check if this relationship implies source should be generated before target.
     */
    public boolean impliesOrdering() {
        return type.impliesOrdering() && isActive();
    }

    /**
     * Get a human-readable description.
     */
    public String describe() {
        return sourceTopicId + " " + type.getDisplayName().toLowerCase() + " " + targetTopicId;
    }
}
