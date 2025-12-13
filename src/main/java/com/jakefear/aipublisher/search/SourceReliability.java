package com.jakefear.aipublisher.search;

/**
 * Represents the reliability level of a source.
 */
public enum SourceReliability {
    /**
     * Official documentation, specifications, or primary sources.
     * Examples: docs.oracle.com, kafka.apache.org/documentation
     */
    OFFICIAL("Official documentation or primary source"),

    /**
     * Academic or peer-reviewed sources.
     * Examples: arxiv.org, ACM Digital Library, IEEE
     */
    ACADEMIC("Academic or peer-reviewed source"),

    /**
     * Major tech publishers and recognized experts.
     * Examples: O'Reilly, Manning, Martin Fowler's blog
     */
    AUTHORITATIVE("Authoritative tech publisher or expert"),

    /**
     * Reputable community resources with editorial oversight.
     * Examples: Wikipedia, Stack Overflow (high-voted answers)
     */
    REPUTABLE("Reputable community resource"),

    /**
     * Community-generated content without editorial oversight.
     * Examples: Reddit, Quora, personal blogs
     */
    COMMUNITY("Community-generated content"),

    /**
     * Source reliability cannot be determined.
     */
    UNCERTAIN("Reliability uncertain");

    private final String description;

    SourceReliability(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this source is considered trustworthy for fact verification.
     */
    public boolean isTrustworthy() {
        return this == OFFICIAL || this == ACADEMIC || this == AUTHORITATIVE;
    }

    /**
     * Get a numeric reliability score (0.0 to 1.0).
     */
    public double getScore() {
        return switch (this) {
            case OFFICIAL -> 1.0;
            case ACADEMIC -> 0.95;
            case AUTHORITATIVE -> 0.85;
            case REPUTABLE -> 0.7;
            case COMMUNITY -> 0.5;
            case UNCERTAIN -> 0.3;
        };
    }
}
