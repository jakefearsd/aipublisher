package com.jakefear.aipublisher.document;

import java.util.List;
import java.util.Objects;

/**
 * Output from the Fact Checker Agent containing verification results.
 */
public record FactCheckReport(
        /**
         * The annotated content with fact-check markers (if any).
         */
        String annotatedContent,

        /**
         * Claims that were successfully verified.
         */
        List<VerifiedClaim> verifiedClaims,

        /**
         * Claims that are questionable or unverifiable.
         */
        List<QuestionableClaim> questionableClaims,

        /**
         * Internal consistency issues found in the article.
         */
        List<String> consistencyIssues,

        /**
         * Overall confidence in the article's factual accuracy.
         */
        ConfidenceLevel overallConfidence,

        /**
         * Recommended action based on the fact-check.
         */
        RecommendedAction recommendedAction
) {
    public FactCheckReport {
        Objects.requireNonNull(overallConfidence, "overallConfidence must not be null");
        Objects.requireNonNull(recommendedAction, "recommendedAction must not be null");

        // Ensure immutable collections
        verifiedClaims = verifiedClaims == null ? List.of() : List.copyOf(verifiedClaims);
        questionableClaims = questionableClaims == null ? List.of() : List.copyOf(questionableClaims);
        consistencyIssues = consistencyIssues == null ? List.of() : List.copyOf(consistencyIssues);
    }

    /**
     * Check if the fact-check passed (approved for next phase).
     */
    public boolean isPassed() {
        return recommendedAction == RecommendedAction.APPROVE;
    }

    /**
     * Check if the article needs revision.
     */
    public boolean needsRevision() {
        return recommendedAction == RecommendedAction.REVISE;
    }

    /**
     * Check if the article was rejected.
     */
    public boolean isRejected() {
        return recommendedAction == RecommendedAction.REJECT;
    }

    /**
     * Get the total number of issues found.
     */
    public int getIssueCount() {
        return questionableClaims.size() + consistencyIssues.size();
    }

    /**
     * Check if this confidence level meets the specified minimum.
     */
    public boolean meetsConfidenceThreshold(ConfidenceLevel minimum) {
        return overallConfidence.meetsMinimum(minimum);
    }
}
