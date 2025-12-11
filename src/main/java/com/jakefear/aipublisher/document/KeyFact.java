package com.jakefear.aipublisher.document;

import java.util.Objects;

/**
 * A key fact identified during research, with optional source reference.
 */
public record KeyFact(
        /**
         * The factual statement.
         */
        String fact,

        /**
         * Index of the source citation that supports this fact, or -1 if unsourced.
         */
        int sourceIndex
) {
    public KeyFact {
        Objects.requireNonNull(fact, "fact must not be null");
    }

    /**
     * Create a key fact without a source reference.
     */
    public static KeyFact unsourced(String fact) {
        return new KeyFact(fact, -1);
    }

    /**
     * Create a key fact with a source reference.
     */
    public static KeyFact withSource(String fact, int sourceIndex) {
        return new KeyFact(fact, sourceIndex);
    }

    /**
     * Check if this fact has a source reference.
     */
    public boolean hasSource() {
        return sourceIndex >= 0;
    }
}
