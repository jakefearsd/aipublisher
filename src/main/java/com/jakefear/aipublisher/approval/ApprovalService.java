package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for managing human approval checkpoints in the pipeline.
 */
@Service
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final PipelineProperties pipelineProperties;
    private ApprovalCallback approvalCallback;

    public ApprovalService(PipelineProperties pipelineProperties, ApprovalCallback approvalCallback) {
        this.pipelineProperties = pipelineProperties;
        this.approvalCallback = approvalCallback;
    }

    /**
     * Set a custom approval callback (useful for auto-approve mode).
     */
    public void setCallback(ApprovalCallback callback) {
        this.approvalCallback = callback;
    }

    /**
     * Check if approval is required at the given state.
     *
     * @param state The current document state
     * @return true if human approval is required
     */
    public boolean isApprovalRequired(DocumentState state) {
        var approval = pipelineProperties.getApproval();

        return switch (state) {
            case RESEARCHING -> approval.isAfterResearch();
            case DRAFTING -> approval.isAfterDraft();
            case FACT_CHECKING -> approval.isAfterFactcheck();
            case EDITING -> approval.isBeforePublish();
            default -> false;
        };
    }

    /**
     * Request approval for a document at its current state.
     *
     * @param document The document to approve
     * @return The approval decision
     * @throws ApprovalCallback.ApprovalTimeoutException if approval times out
     */
    public ApprovalDecision requestApproval(PublishingDocument document) {
        DocumentState state = document.getState();

        if (!isApprovalRequired(state)) {
            log.debug("No approval required at state {}", state);
            return ApprovalDecision.approve(null, "not-required");
        }

        log.info("Requesting approval for document '{}' at state {}",
                document.getTopicBrief().topic(), state);

        ApprovalRequest request = ApprovalRequest.create(document, state);
        ApprovalDecision decision = approvalCallback.requestApproval(request);

        log.info("Approval decision for '{}': {} by {}",
                document.getTopicBrief().topic(),
                decision.decision(),
                decision.approver());

        return decision;
    }

    /**
     * Check approval and return whether the pipeline should continue.
     *
     * @param document The document to check
     * @return true if approved to continue, false otherwise
     * @throws ApprovalRejectedException if explicitly rejected
     */
    public boolean checkAndApprove(PublishingDocument document) throws ApprovalRejectedException {
        if (!isApprovalRequired(document.getState())) {
            return true;
        }

        ApprovalDecision decision = requestApproval(document);

        if (decision.isRejected()) {
            throw new ApprovalRejectedException(
                    "Document rejected by " + decision.approver() + ": " + decision.feedback(),
                    document.getState()
            );
        }

        if (decision.changesRequested()) {
            log.info("Changes requested: {}", decision.feedback());
            return false; // Signal to retry the current phase
        }

        return decision.isApproved();
    }

    /**
     * Exception thrown when a document is explicitly rejected.
     */
    public static class ApprovalRejectedException extends RuntimeException {
        private final DocumentState state;

        public ApprovalRejectedException(String message, DocumentState state) {
            super(message);
            this.state = state;
        }

        public DocumentState getState() {
            return state;
        }
    }
}
