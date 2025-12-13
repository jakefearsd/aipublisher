package com.jakefear.aipublisher.seealso;

import java.util.Objects;

/**
 * A single entry in a "See Also" section.
 */
public record SeeAlsoEntry(
        String title,
        SeeAlsoType type,
        String wikiPage,
        String description,
        double relevanceScore
) {
    /**
     * Compact constructor with validation.
     */
    public SeeAlsoEntry {
        Objects.requireNonNull(title, "title cannot be null");
        if (title.isBlank()) {
            throw new IllegalArgumentException("title cannot be blank");
        }
        Objects.requireNonNull(type, "type cannot be null");
        if (relevanceScore < 0.0 || relevanceScore > 1.0) {
            throw new IllegalArgumentException("relevanceScore must be between 0.0 and 1.0");
        }
        // Normalize nulls
        description = description == null ? "" : description.trim();
    }

    /**
     * Create an internal wiki link entry.
     */
    public static SeeAlsoEntry internal(String title, SeeAlsoType type, String wikiPage, String description) {
        return new SeeAlsoEntry(title, type, wikiPage, description, 0.5);
    }

    /**
     * Create an internal wiki link entry with relevance score.
     */
    public static SeeAlsoEntry internal(String title, SeeAlsoType type, String wikiPage, String description, double relevance) {
        return new SeeAlsoEntry(title, type, wikiPage, description, relevance);
    }

    /**
     * Create a related topic entry.
     */
    public static SeeAlsoEntry related(String title, String wikiPage, String description) {
        return new SeeAlsoEntry(title, SeeAlsoType.RELATED, wikiPage, description, 0.5);
    }

    /**
     * Create a broader topic entry.
     */
    public static SeeAlsoEntry broader(String title, String wikiPage, String description) {
        return new SeeAlsoEntry(title, SeeAlsoType.BROADER, wikiPage, description, 0.5);
    }

    /**
     * Create a narrower topic entry.
     */
    public static SeeAlsoEntry narrower(String title, String wikiPage, String description) {
        return new SeeAlsoEntry(title, SeeAlsoType.NARROWER, wikiPage, description, 0.5);
    }

    /**
     * Create a tutorial reference entry.
     */
    public static SeeAlsoEntry tutorial(String title, String wikiPage, String description) {
        return new SeeAlsoEntry(title, SeeAlsoType.TUTORIAL, wikiPage, description, 0.5);
    }

    /**
     * Create an external link entry.
     */
    public static SeeAlsoEntry external(String title, String url, String description) {
        return new SeeAlsoEntry(title, SeeAlsoType.EXTERNAL, url, description, 0.5);
    }

    /**
     * Check if this entry has a wiki page link.
     */
    public boolean hasWikiPage() {
        return wikiPage != null && !wikiPage.isBlank();
    }

    /**
     * Check if this entry has a description.
     */
    public boolean hasDescription() {
        return !description.isBlank();
    }

    /**
     * Generate JSPWiki syntax for this entry.
     */
    public String toWikiText() {
        StringBuilder sb = new StringBuilder();

        // Generate the link
        if (hasWikiPage()) {
            if (type == SeeAlsoType.EXTERNAL) {
                // External link format
                sb.append("[").append(title).append("|").append(wikiPage).append("]");
            } else {
                // Internal wiki link
                String normalizedPage = wikiPage.replaceAll("\\s+", "");
                String normalizedTitle = title.replaceAll("\\s+", "");
                if (normalizedTitle.equalsIgnoreCase(normalizedPage)) {
                    sb.append("[").append(wikiPage).append("]");
                } else {
                    sb.append("[").append(title).append("|").append(wikiPage).append("]");
                }
            }
        } else {
            sb.append(title);
        }

        // Add description if present
        if (hasDescription()) {
            sb.append(" - ").append(description);
        }

        return sb.toString();
    }

    /**
     * Create a copy with a different relevance score.
     */
    public SeeAlsoEntry withRelevance(double relevance) {
        return new SeeAlsoEntry(title, type, wikiPage, description, relevance);
    }
}
