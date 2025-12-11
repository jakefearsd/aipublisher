package com.jakefear.aipublisher.document;

import java.util.Objects;

/**
 * A claim that the fact checker found questionable or unverifiable.
 */
public record QuestionableClaim(
        /**
         * The claim that is questionable.
         */
        String claim,

        /**
         * Description of the issue with this claim.
         */
        String issue,

        /**
         * Suggested correction or clarification.
         */
        String suggestion
) {
    public QuestionableClaim {
        Objects.requireNonNull(claim, "claim must not be null");
        Objects.requireNonNull(issue, "issue must not be null");
        if (suggestion == null) {
            suggestion = "";
        }
    }

    /**
     * Create a questionable claim with a suggestion for fix.
     */
    public static QuestionableClaim withSuggestion(String claim, String issue, String suggestion) {
        return new QuestionableClaim(claim, issue, suggestion);
    }

    /**
     * Create a questionable claim without a suggestion.
     */
    public static QuestionableClaim withoutSuggestion(String claim, String issue) {
        return new QuestionableClaim(claim, issue, "");
    }
}
