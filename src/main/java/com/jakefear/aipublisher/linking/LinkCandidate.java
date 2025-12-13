package com.jakefear.aipublisher.linking;

import java.util.Objects;

/**
 * A potential link candidate with context for evaluation.
 */
public record LinkCandidate(
        /**
         * The target page name (CamelCase format for wiki).
         */
        String targetPage,

        /**
         * The text that would be linked.
         */
        String anchorText,

        /**
         * The position in the document (character offset).
         */
        int position,

        /**
         * Whether this is the first mention of this term.
         */
        boolean firstMention,

        /**
         * The surrounding context (sentence or paragraph).
         */
        String context,

        /**
         * Relevance score based on how well the link fits the content.
         */
        double relevanceScore
) {
    public LinkCandidate {
        Objects.requireNonNull(targetPage, "targetPage must not be null");
        Objects.requireNonNull(anchorText, "anchorText must not be null");

        if (targetPage.isBlank()) {
            throw new IllegalArgumentException("targetPage must not be blank");
        }
        if (anchorText.isBlank()) {
            throw new IllegalArgumentException("anchorText must not be blank");
        }
        if (position < 0) {
            throw new IllegalArgumentException("position must be non-negative");
        }
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("relevanceScore must be between 0.0 and 1.0");
        }

        context = context == null ? "" : context;
    }

    /**
     * Create a simple link candidate.
     */
    public static LinkCandidate create(String targetPage, String anchorText, int position) {
        return new LinkCandidate(targetPage, anchorText, position, true, "", 0.5);
    }

    /**
     * Create a link candidate with first-mention flag.
     */
    public static LinkCandidate withFirstMention(String targetPage, String anchorText, int position, boolean firstMention) {
        double score = firstMention ? 0.8 : 0.3;
        return new LinkCandidate(targetPage, anchorText, position, firstMention, "", score);
    }

    /**
     * Check if this is a high-value link (should be included).
     */
    public boolean isHighValue() {
        return relevanceScore >= 0.6 && firstMention;
    }

    /**
     * Generate the JSPWiki link syntax.
     */
    public String toWikiLink() {
        if (anchorText.equalsIgnoreCase(targetPage) ||
            anchorText.replaceAll("\\s+", "").equalsIgnoreCase(targetPage)) {
            return "[" + targetPage + "]";
        }
        return "[" + anchorText + "|" + targetPage + "]";
    }
}
