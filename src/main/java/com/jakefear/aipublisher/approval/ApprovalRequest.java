package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a request for human approval at a pipeline checkpoint.
 */
public record ApprovalRequest(
        /**
         * Unique identifier for this approval request.
         */
        UUID id,

        /**
         * The document awaiting approval.
         */
        PublishingDocument document,

        /**
         * The state/phase where approval is required.
         */
        DocumentState atState,

        /**
         * When the approval was requested.
         */
        Instant requestedAt,

        /**
         * Summary of what needs approval.
         */
        String summary
) {
    /**
     * Create a new approval request for a document at a specific state.
     */
    public static ApprovalRequest create(PublishingDocument document, DocumentState atState) {
        return new ApprovalRequest(
                UUID.randomUUID(),
                document,
                atState,
                Instant.now(),
                generateSummary(document, atState)
        );
    }

    private static String generateSummary(PublishingDocument document, DocumentState state) {
        return switch (state) {
            case RESEARCHING -> String.format(
                    "Research complete for '%s': %d key facts gathered",
                    document.getTopicBrief().topic(),
                    document.getResearchBrief() != null ? document.getResearchBrief().keyFacts().size() : 0
            );
            case DRAFTING -> String.format(
                    "Draft ready for '%s': ~%d words",
                    document.getTopicBrief().topic(),
                    document.getDraft() != null ? document.getDraft().estimateWordCount() : 0
            );
            case FACT_CHECKING -> String.format(
                    "Fact check complete for '%s': %s confidence, %s",
                    document.getTopicBrief().topic(),
                    document.getFactCheckReport() != null ? document.getFactCheckReport().overallConfidence() : "N/A",
                    document.getFactCheckReport() != null ? document.getFactCheckReport().recommendedAction() : "N/A"
            );
            case EDITING -> String.format(
                    "Ready to publish '%s': quality score %.2f",
                    document.getTopicBrief().topic(),
                    document.getFinalArticle() != null ? document.getFinalArticle().qualityScore() : 0.0
            );
            default -> "Approval required for " + document.getTopicBrief().topic();
        };
    }
}
