package com.jakefear.aipublisher.document;

import java.util.Objects;

/**
 * A source citation with reliability assessment.
 */
public record SourceCitation(
        /**
         * Description or URL of the source.
         */
        String description,

        /**
         * Assessed reliability of the source.
         */
        ConfidenceLevel reliability
) {
    public SourceCitation {
        Objects.requireNonNull(description, "description must not be null");
        if (reliability == null) {
            reliability = ConfidenceLevel.MEDIUM;
        }
    }

    /**
     * Create a high-reliability source citation.
     */
    public static SourceCitation highReliability(String description) {
        return new SourceCitation(description, ConfidenceLevel.HIGH);
    }

    /**
     * Create a medium-reliability source citation.
     */
    public static SourceCitation mediumReliability(String description) {
        return new SourceCitation(description, ConfidenceLevel.MEDIUM);
    }
}
