package com.jakefear.aipublisher.document;

import java.util.List;
import java.util.Objects;

/**
 * Output from the Editor Agent - the final, publication-ready article.
 */
public record FinalArticle(
        /**
         * The final polished content in JSPWiki format.
         */
        String wikiContent,

        /**
         * Article metadata (title, summary, author, timestamps).
         */
        DocumentMetadata metadata,

        /**
         * Summary of edits made during the editing phase.
         */
        String editSummary,

        /**
         * Quality score from 0.0 to 1.0.
         */
        double qualityScore,

        /**
         * Links to existing pages that were added during editing.
         */
        List<String> addedLinks
) {
    public FinalArticle {
        Objects.requireNonNull(wikiContent, "wikiContent must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");

        if (qualityScore < 0.0 || qualityScore > 1.0) {
            throw new IllegalArgumentException("qualityScore must be between 0.0 and 1.0");
        }

        // Ensure immutable list
        addedLinks = addedLinks == null ? List.of() : List.copyOf(addedLinks);
    }

    /**
     * Check if the article meets the minimum quality threshold.
     */
    public boolean meetsQualityThreshold(double minimumScore) {
        return qualityScore >= minimumScore;
    }

    /**
     * Estimate word count of the final content.
     */
    public int estimateWordCount() {
        if (wikiContent == null || wikiContent.isBlank()) {
            return 0;
        }
        return wikiContent.trim().split("\\s+").length;
    }

    /**
     * Get the title from metadata.
     */
    public String getTitle() {
        return metadata.title();
    }

    /**
     * Get the summary from metadata.
     */
    public String getSummary() {
        return metadata.summary();
    }
}
