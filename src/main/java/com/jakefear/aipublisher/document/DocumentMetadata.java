package com.jakefear.aipublisher.document;

import java.time.Instant;
import java.util.Objects;

/**
 * Metadata for a published wiki article.
 */
public record DocumentMetadata(
        /**
         * The article title.
         */
        String title,

        /**
         * Summary/description for the page.
         */
        String summary,

        /**
         * Author attribution.
         */
        String author,

        /**
         * When the article was created.
         */
        Instant createdAt,

        /**
         * When the article was last updated.
         */
        Instant updatedAt
) {
    public DocumentMetadata {
        Objects.requireNonNull(title, "title must not be null");
        if (author == null || author.isBlank()) {
            author = "AI Publisher";
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    /**
     * Create metadata with just title and summary.
     */
    public static DocumentMetadata create(String title, String summary) {
        return new DocumentMetadata(title, summary, "AI Publisher", Instant.now(), Instant.now());
    }

    /**
     * Create updated metadata with new timestamp.
     */
    public DocumentMetadata withUpdatedTimestamp() {
        return new DocumentMetadata(title, summary, author, createdAt, Instant.now());
    }
}
