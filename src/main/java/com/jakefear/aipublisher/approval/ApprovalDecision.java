package com.jakefear.aipublisher.approval;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a human's decision on an approval request.
 */
public record ApprovalDecision(
        /**
         * ID of the approval request this decision is for.
         */
        UUID requestId,

        /**
         * The decision made.
         */
        Decision decision,

        /**
         * Optional feedback or reason for the decision.
         */
        String feedback,

        /**
         * Who made the decision (could be username, email, etc.).
         */
        String approver,

        /**
         * When the decision was made.
         */
        Instant decidedAt
) {
    /**
     * The possible decisions a human can make.
     */
    public enum Decision {
        /**
         * Approve and continue to next phase.
         */
        APPROVE,

        /**
         * Reject and stop the pipeline.
         */
        REJECT,

        /**
         * Request changes and retry the current phase.
         */
        REQUEST_CHANGES
    }

    /**
     * Create an approval decision.
     */
    public static ApprovalDecision approve(UUID requestId, String approver) {
        return new ApprovalDecision(requestId, Decision.APPROVE, null, approver, Instant.now());
    }

    /**
     * Create a rejection decision.
     */
    public static ApprovalDecision reject(UUID requestId, String approver, String reason) {
        return new ApprovalDecision(requestId, Decision.REJECT, reason, approver, Instant.now());
    }

    /**
     * Create a request for changes.
     */
    public static ApprovalDecision requestChanges(UUID requestId, String approver, String feedback) {
        return new ApprovalDecision(requestId, Decision.REQUEST_CHANGES, feedback, approver, Instant.now());
    }

    /**
     * Check if this is an approval.
     */
    public boolean isApproved() {
        return decision == Decision.APPROVE;
    }

    /**
     * Check if this is a rejection.
     */
    public boolean isRejected() {
        return decision == Decision.REJECT;
    }

    /**
     * Check if changes were requested.
     */
    public boolean changesRequested() {
        return decision == Decision.REQUEST_CHANGES;
    }
}
