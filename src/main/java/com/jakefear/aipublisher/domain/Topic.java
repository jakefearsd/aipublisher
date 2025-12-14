package com.jakefear.aipublisher.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jakefear.aipublisher.content.ContentType;

import java.time.Instant;
import java.util.*;

/**
 * A topic in the knowledge domain universe.
 * Represents a single wiki page to be generated.
 */
public record Topic(
        String id,
        String name,
        String description,
        TopicStatus status,
        ContentType contentType,
        ComplexityLevel complexity,
        Priority priority,
        int estimatedWords,
        Set<String> emphasize,
        Set<String> skip,
        String userNotes,
        boolean isLandingPage,
        boolean isSplitTopic,
        String parentClusterId,
        String category,
        Instant createdAt,
        Instant modifiedAt,
        String addedReason
) {
    /**
     * Compact constructor with validation and normalization.
     */
    public Topic {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        Objects.requireNonNull(status, "status cannot be null");

        // Defaults
        if (description == null) description = "";
        if (contentType == null) contentType = ContentType.CONCEPT;
        if (complexity == null) complexity = ComplexityLevel.INTERMEDIATE;
        if (priority == null) priority = Priority.SHOULD_HAVE;
        if (estimatedWords <= 0) estimatedWords = complexity.getMinWords();
        emphasize = emphasize == null ? Set.of() : Set.copyOf(emphasize);
        skip = skip == null ? Set.of() : Set.copyOf(skip);
        if (userNotes == null) userNotes = "";
        if (category == null) category = "";
        if (createdAt == null) createdAt = Instant.now();
        if (modifiedAt == null) modifiedAt = createdAt;
        if (addedReason == null) addedReason = "";
    }

    /**
     * Create a new proposed topic with minimal information.
     */
    public static Topic proposed(String name, String description) {
        return new Topic(
                generateId(name),
                name,
                description,
                TopicStatus.PROPOSED,
                ContentType.CONCEPT,
                ComplexityLevel.INTERMEDIATE,
                Priority.SHOULD_HAVE,
                1000,
                Set.of(),
                Set.of(),
                "",
                false,
                false,
                null,
                "",
                Instant.now(),
                Instant.now(),
                "AI suggested"
        );
    }

    /**
     * Create builder starting from this topic.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Create a new builder.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Generate a wiki-safe ID from a name.
     */
    public static String generateId(String name) {
        return name.replaceAll("[^a-zA-Z0-9]+", "")
                .replaceAll("\\s+", "");
    }

    /**
     * Get the wiki page name for this topic.
     */
    @JsonIgnore
    public String getWikiPageName() {
        return generateId(name);
    }

    /**
     * Check if this topic is ready for generation.
     */
    @JsonIgnore
    public boolean isReadyForGeneration() {
        return status == TopicStatus.ACCEPTED && priority.shouldGenerate();
    }

    /**
     * Check if user has provided custom guidance.
     */
    @JsonIgnore
    public boolean hasUserGuidance() {
        return !emphasize.isEmpty() || !skip.isEmpty() || !userNotes.isBlank();
    }

    /**
     * Create accepted version of this topic.
     */
    public Topic accept() {
        return toBuilder().status(TopicStatus.ACCEPTED).build();
    }

    /**
     * Create rejected version of this topic.
     */
    public Topic reject() {
        return toBuilder().status(TopicStatus.REJECTED).build();
    }

    /**
     * Create deferred version of this topic.
     */
    public Topic defer() {
        return toBuilder()
                .status(TopicStatus.DEFERRED)
                .priority(Priority.BACKLOG)
                .build();
    }

    /**
     * Builder for Topic.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description = "";
        private TopicStatus status = TopicStatus.PROPOSED;
        private ContentType contentType = ContentType.CONCEPT;
        private ComplexityLevel complexity = ComplexityLevel.INTERMEDIATE;
        private Priority priority = Priority.SHOULD_HAVE;
        private int estimatedWords = 1000;
        private Set<String> emphasize = new HashSet<>();
        private Set<String> skip = new HashSet<>();
        private String userNotes = "";
        private boolean isLandingPage = false;
        private boolean isSplitTopic = false;
        private String parentClusterId = null;
        private String category = "";
        private Instant createdAt = Instant.now();
        private Instant modifiedAt = Instant.now();
        private String addedReason = "";

        public Builder(String name) {
            this.name = name;
            this.id = Topic.generateId(name);
        }

        public Builder(Topic topic) {
            this.id = topic.id;
            this.name = topic.name;
            this.description = topic.description;
            this.status = topic.status;
            this.contentType = topic.contentType;
            this.complexity = topic.complexity;
            this.priority = topic.priority;
            this.estimatedWords = topic.estimatedWords;
            this.emphasize = new HashSet<>(topic.emphasize);
            this.skip = new HashSet<>(topic.skip);
            this.userNotes = topic.userNotes;
            this.isLandingPage = topic.isLandingPage;
            this.isSplitTopic = topic.isSplitTopic;
            this.parentClusterId = topic.parentClusterId;
            this.category = topic.category;
            this.createdAt = topic.createdAt;
            this.modifiedAt = Instant.now();
            this.addedReason = topic.addedReason;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder status(TopicStatus status) {
            this.status = status;
            return this;
        }

        public Builder contentType(ContentType contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder complexity(ComplexityLevel complexity) {
            this.complexity = complexity;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder estimatedWords(int estimatedWords) {
            this.estimatedWords = estimatedWords;
            return this;
        }

        public Builder emphasize(Set<String> emphasize) {
            this.emphasize = new HashSet<>(emphasize);
            return this;
        }

        public Builder addEmphasis(String aspect) {
            this.emphasize.add(aspect);
            return this;
        }

        public Builder skip(Set<String> skip) {
            this.skip = new HashSet<>(skip);
            return this;
        }

        public Builder addSkip(String aspect) {
            this.skip.add(aspect);
            return this;
        }

        public Builder userNotes(String userNotes) {
            this.userNotes = userNotes;
            return this;
        }

        public Builder isLandingPage(boolean isLandingPage) {
            this.isLandingPage = isLandingPage;
            return this;
        }

        public Builder isSplitTopic(boolean isSplitTopic) {
            this.isSplitTopic = isSplitTopic;
            return this;
        }

        public Builder parentClusterId(String parentClusterId) {
            this.parentClusterId = parentClusterId;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder addedReason(String addedReason) {
            this.addedReason = addedReason;
            return this;
        }

        public Topic build() {
            return new Topic(
                    id, name, description, status, contentType, complexity,
                    priority, estimatedWords, emphasize, skip, userNotes,
                    isLandingPage, isSplitTopic, parentClusterId, category,
                    createdAt, modifiedAt, addedReason
            );
        }
    }
}
