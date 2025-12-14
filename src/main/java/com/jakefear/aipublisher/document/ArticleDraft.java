package com.jakefear.aipublisher.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Output from the Writer Agent containing the article draft.
 */
public record ArticleDraft(
        /**
         * The article content in JSPWiki format.
         */
        String wikiContent,

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
        Objects.requireNonNull(wikiContent, "wikiContent must not be null");
        if (wikiContent.isBlank()) {
            throw new IllegalArgumentException("wikiContent must not be blank");
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
        return !wikiContent.isBlank() && summary != null && !summary.isBlank();
    }

    /**
     * Estimate word count of the wiki content.
     */
    public int estimateWordCount() {
        if (wikiContent == null || wikiContent.isBlank()) {
            return 0;
        }
        // Simple word count: split on whitespace
        return wikiContent.trim().split("\\s+").length;
    }
}
