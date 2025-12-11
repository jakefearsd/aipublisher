package com.jakefear.aipublisher.document;

import java.util.Objects;

/**
 * A claim that has been verified by the fact checker.
 */
public record VerifiedClaim(
        /**
         * The factual claim that was verified.
         */
        String claim,

        /**
         * Verification status.
         */
        String status,

        /**
         * Index of the source that supports this claim.
         */
        int sourceIndex
) {
    public VerifiedClaim {
        Objects.requireNonNull(claim, "claim must not be null");
        if (status == null || status.isBlank()) {
            status = "VERIFIED";
        }
    }

    /**
     * Create a verified claim with source reference.
     */
    public static VerifiedClaim verified(String claim, int sourceIndex) {
        return new VerifiedClaim(claim, "VERIFIED", sourceIndex);
    }
}
