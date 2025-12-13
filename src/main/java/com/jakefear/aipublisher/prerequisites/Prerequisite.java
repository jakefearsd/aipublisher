package com.jakefear.aipublisher.prerequisites;

import java.util.Objects;

/**
 * A single prerequisite for an article.
 */
public record Prerequisite(
        /**
         * The topic or concept that is a prerequisite.
         */
        String topic,

        /**
         * The type of prerequisite (HARD, SOFT, ASSUMED).
         */
        PrerequisiteType type,

        /**
         * The wiki page name to link to (if available).
         */
        String wikiPage,

        /**
         * Brief description of why this is a prerequisite.
         */
        String reason
) {
    public Prerequisite {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(type, "type must not be null");

        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }

        reason = reason == null ? "" : reason;
    }

    /**
     * Create a hard prerequisite with a wiki page link.
     */
    public static Prerequisite hard(String topic, String wikiPage, String reason) {
        return new Prerequisite(topic, PrerequisiteType.HARD, wikiPage, reason);
    }

    /**
     * Create a hard prerequisite without a wiki page link.
     */
    public static Prerequisite hard(String topic, String reason) {
        return new Prerequisite(topic, PrerequisiteType.HARD, null, reason);
    }

    /**
     * Create a soft prerequisite with a wiki page link.
     */
    public static Prerequisite soft(String topic, String wikiPage, String reason) {
        return new Prerequisite(topic, PrerequisiteType.SOFT, wikiPage, reason);
    }

    /**
     * Create a soft prerequisite without a wiki page link.
     */
    public static Prerequisite soft(String topic, String reason) {
        return new Prerequisite(topic, PrerequisiteType.SOFT, null, reason);
    }

    /**
     * Create an assumed knowledge prerequisite.
     */
    public static Prerequisite assumed(String topic) {
        return new Prerequisite(topic, PrerequisiteType.ASSUMED, null, "");
    }

    /**
     * Check if this prerequisite has a wiki page link.
     */
    public boolean hasWikiPage() {
        return wikiPage != null && !wikiPage.isBlank();
    }

    /**
     * Generate JSPWiki link if wiki page exists, otherwise just the topic name.
     */
    public String toWikiText() {
        if (hasWikiPage()) {
            if (topic.replaceAll("\\s+", "").equalsIgnoreCase(wikiPage)) {
                return "[" + wikiPage + "]";
            }
            return "[" + topic + "|" + wikiPage + "]";
        }
        return topic;
    }
}
