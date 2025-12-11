package com.jakefear.aipublisher.document;

import java.util.Objects;

/**
 * Quality assessment scores and metrics for a document.
 */
public record QualityAssessment(
        /**
         * Confidence level from fact-checking.
         */
        ConfidenceLevel factCheckConfidence,

        /**
         * Quality score from the editor (0.0 to 1.0).
         */
        double editorScore,

        /**
         * Number of revision cycles the document went through.
         */
        int revisionCycles,

        /**
         * Whether the document passed all quality gates.
         */
        boolean passedAllGates
) {
    public QualityAssessment {
        Objects.requireNonNull(factCheckConfidence, "factCheckConfidence must not be null");
        if (editorScore < 0.0 || editorScore > 1.0) {
            throw new IllegalArgumentException("editorScore must be between 0.0 and 1.0");
        }
        if (revisionCycles < 0) {
            throw new IllegalArgumentException("revisionCycles must be non-negative");
        }
    }

    /**
     * Create an initial assessment (before any processing).
     */
    public static QualityAssessment initial() {
        return new QualityAssessment(ConfidenceLevel.LOW, 0.0, 0, false);
    }

    /**
     * Create an assessment after successful completion.
     */
    public static QualityAssessment passed(ConfidenceLevel confidence, double score, int cycles) {
        return new QualityAssessment(confidence, score, cycles, true);
    }

    /**
     * Create an assessment for a failed document.
     */
    public static QualityAssessment failed(ConfidenceLevel confidence, double score, int cycles) {
        return new QualityAssessment(confidence, score, cycles, false);
    }

    /**
     * Check if quality meets the specified thresholds.
     */
    public boolean meetsThresholds(ConfidenceLevel minConfidence, double minScore) {
        return factCheckConfidence.meetsMinimum(minConfidence) && editorScore >= minScore;
    }
}
