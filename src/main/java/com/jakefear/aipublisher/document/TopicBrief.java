package com.jakefear.aipublisher.document;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Objects;

/**
 * Initial input from the user defining what article to create.
 * This is the starting point for the publishing pipeline.
 */
public record TopicBrief(
        /**
         * The main topic or title for the article.
         */
        String topic,

        /**
         * Description of the target audience (e.g., "developers new to event streaming").
         */
        String targetAudience,

        /**
         * Target word count for the article.
         */
        int targetWordCount,

        /**
         * Specific sections that must be included in the article.
         */
        List<String> requiredSections,

        /**
         * Known related wiki pages that should be linked.
         */
        List<String> relatedPages,

        /**
         * Optional source URLs for research.
         */
        List<String> sourceUrls,

        /**
         * The type of content to generate (e.g., CONCEPT, TUTORIAL, COMPARISON).
         * If null, will be auto-detected from the topic.
         */
        ContentType contentType,

        /**
         * Optional domain context for the content (e.g., "e-commerce", "microservices").
         */
        String domainContext,

        /**
         * Optional specific goal or outcome for the content.
         */
        String specificGoal
) {
    /**
     * Compact constructor with validation.
     */
    public TopicBrief {
        Objects.requireNonNull(topic, "topic must not be null");
        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        if (targetWordCount < 0) {
            throw new IllegalArgumentException("targetWordCount must be non-negative");
        }
        // Ensure immutable lists and handle nulls
        requiredSections = requiredSections == null ? List.of() : List.copyOf(requiredSections);
        relatedPages = relatedPages == null ? List.of() : List.copyOf(relatedPages);
        sourceUrls = sourceUrls == null ? List.of() : List.copyOf(sourceUrls);
    }

    /**
     * Convenience constructor for simple topics (backwards compatible).
     */
    public static TopicBrief simple(String topic, String targetAudience, int targetWordCount) {
        return new TopicBrief(topic, targetAudience, targetWordCount, List.of(), List.of(), List.of(), null, null, null);
    }

    /**
     * Convenience constructor with content type.
     */
    public static TopicBrief withType(String topic, String targetAudience, int targetWordCount, ContentType contentType) {
        return new TopicBrief(topic, targetAudience, targetWordCount, List.of(), List.of(), List.of(), contentType, null, null);
    }

    /**
     * Builder for more complex TopicBrief construction.
     */
    public static Builder builder(String topic) {
        return new Builder(topic);
    }

    public static class Builder {
        private final String topic;
        private String targetAudience = "general readers";
        private int targetWordCount = 800;
        private List<String> requiredSections = List.of();
        private List<String> relatedPages = List.of();
        private List<String> sourceUrls = List.of();
        private ContentType contentType;
        private String domainContext;
        private String specificGoal;

        private Builder(String topic) {
            this.topic = topic;
        }

        public Builder targetAudience(String audience) {
            this.targetAudience = audience;
            return this;
        }

        public Builder targetWordCount(int wordCount) {
            this.targetWordCount = wordCount;
            return this;
        }

        public Builder requiredSections(List<String> sections) {
            this.requiredSections = sections;
            return this;
        }

        public Builder relatedPages(List<String> pages) {
            this.relatedPages = pages;
            return this;
        }

        public Builder sourceUrls(List<String> urls) {
            this.sourceUrls = urls;
            return this;
        }

        public Builder contentType(ContentType type) {
            this.contentType = type;
            return this;
        }

        public Builder domainContext(String context) {
            this.domainContext = context;
            return this;
        }

        public Builder specificGoal(String goal) {
            this.specificGoal = goal;
            return this;
        }

        public TopicBrief build() {
            return new TopicBrief(topic, targetAudience, targetWordCount,
                    requiredSections, relatedPages, sourceUrls,
                    contentType, domainContext, specificGoal);
        }
    }
}
