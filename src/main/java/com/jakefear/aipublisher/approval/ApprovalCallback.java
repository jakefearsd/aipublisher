package com.jakefear.aipublisher.approval;

/**
 * Callback interface for handling approval requests.
 *
 * Implementations can provide different approval mechanisms:
 * - Console-based (interactive CLI)
 * - File-based (write request, wait for response file)
 * - Web-based (REST API endpoint)
 * - Auto-approve (for testing/automation)
 */
@FunctionalInterface
public interface ApprovalCallback {

    /**
     * Request approval from a human.
     *
     * This method should block until a decision is made or timeout occurs.
     *
     * @param request The approval request details
     * @return The human's decision
     * @throws ApprovalTimeoutException if approval times out
     */
    ApprovalDecision requestApproval(ApprovalRequest request) throws ApprovalTimeoutException;

    /**
     * Exception thrown when approval request times out.
     */
    class ApprovalTimeoutException extends RuntimeException {
        public ApprovalTimeoutException(String message) {
            super(message);
        }
    }
}
