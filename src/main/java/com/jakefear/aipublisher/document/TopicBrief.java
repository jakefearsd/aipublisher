package com.jakefear.aipublisher.document;

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
        List<String> sourceUrls
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
     * Convenience constructor for simple topics.
     */
    public static TopicBrief simple(String topic, String targetAudience, int targetWordCount) {
        return new TopicBrief(topic, targetAudience, targetWordCount, List.of(), List.of(), List.of());
    }
}
