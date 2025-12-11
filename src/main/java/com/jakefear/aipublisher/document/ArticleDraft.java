package com.jakefear.aipublisher.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Output from the Writer Agent containing the article draft.
 */
public record ArticleDraft(
        /**
         * The article content in JSPWiki Markdown format.
         */
        String markdownContent,

        /**
         * One-paragraph summary for page metadata.
         */
        String summary,

        /**
         * List of internal wiki page links used in the article.
         */
        List<String> internalLinks,

        /**
         * Suggested categories/tags for the article.
         */
        List<String> categories,

        /**
         * Additional metadata key-value pairs.
         */
        Map<String, String> metadata
) {
    public ArticleDraft {
        Objects.requireNonNull(markdownContent, "markdownContent must not be null");
        if (markdownContent.isBlank()) {
            throw new IllegalArgumentException("markdownContent must not be blank");
        }

        // Ensure immutable collections
        internalLinks = internalLinks == null ? List.of() : List.copyOf(internalLinks);
        categories = categories == null ? List.of() : List.copyOf(categories);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    /**
     * Check if the draft has valid content to proceed.
     */
    public boolean isValid() {
        return !markdownContent.isBlank() && summary != null && !summary.isBlank();
    }

    /**
     * Estimate word count of the markdown content.
     */
    public int estimateWordCount() {
        if (markdownContent == null || markdownContent.isBlank()) {
            return 0;
        }
        // Simple word count: split on whitespace
        return markdownContent.trim().split("\\s+").length;
    }
}
