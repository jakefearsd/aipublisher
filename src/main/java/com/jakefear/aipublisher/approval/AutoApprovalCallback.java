package com.jakefear.aipublisher.approval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Auto-approval callback that automatically approves all requests.
 *
 * Used when human approval is disabled or for testing/automation.
 */
@Component
@ConditionalOnProperty(name = "pipeline.approval.auto-approve", havingValue = "true", matchIfMissing = true)
public class AutoApprovalCallback implements ApprovalCallback {

    private static final Logger log = LoggerFactory.getLogger(AutoApprovalCallback.class);

    @Override
    public ApprovalDecision requestApproval(ApprovalRequest request) {
        log.info("Auto-approving: {}", request.summary());
        return ApprovalDecision.approve(request.id(), "auto-approve");
    }
}
