package com.jakefear.aipublisher.approval;

import org.junit.jupiter.api.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApprovalDecision")
class ApprovalDecisionTest {

    private final UUID requestId = UUID.randomUUID();

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("Creates approve decision")
        void createsApproveDecision() {
            ApprovalDecision decision = ApprovalDecision.approve(requestId, "approver");

            assertEquals(requestId, decision.requestId());
            assertEquals(ApprovalDecision.Decision.APPROVE, decision.decision());
            assertEquals("approver", decision.approver());
            assertNull(decision.feedback());
            assertNotNull(decision.decidedAt());
        }

        @Test
        @DisplayName("Creates reject decision with reason")
        void createsRejectDecision() {
            ApprovalDecision decision = ApprovalDecision.reject(requestId, "approver", "Not good enough");

            assertEquals(requestId, decision.requestId());
            assertEquals(ApprovalDecision.Decision.REJECT, decision.decision());
            assertEquals("approver", decision.approver());
            assertEquals("Not good enough", decision.feedback());
            assertNotNull(decision.decidedAt());
        }

        @Test
        @DisplayName("Creates request changes decision with feedback")
        void createsRequestChangesDecision() {
            ApprovalDecision decision = ApprovalDecision.requestChanges(requestId, "approver", "Add more detail");

            assertEquals(requestId, decision.requestId());
            assertEquals(ApprovalDecision.Decision.REQUEST_CHANGES, decision.decision());
            assertEquals("approver", decision.approver());
            assertEquals("Add more detail", decision.feedback());
            assertNotNull(decision.decidedAt());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("isApproved returns true for APPROVE")
        void isApprovedReturnsTrue() {
            ApprovalDecision decision = ApprovalDecision.approve(requestId, "approver");
            assertTrue(decision.isApproved());
            assertFalse(decision.isRejected());
            assertFalse(decision.changesRequested());
        }

        @Test
        @DisplayName("isRejected returns true for REJECT")
        void isRejectedReturnsTrue() {
            ApprovalDecision decision = ApprovalDecision.reject(requestId, "approver", "reason");
            assertFalse(decision.isApproved());
            assertTrue(decision.isRejected());
            assertFalse(decision.changesRequested());
        }

        @Test
        @DisplayName("changesRequested returns true for REQUEST_CHANGES")
        void changesRequestedReturnsTrue() {
            ApprovalDecision decision = ApprovalDecision.requestChanges(requestId, "approver", "feedback");
            assertFalse(decision.isApproved());
            assertFalse(decision.isRejected());
            assertTrue(decision.changesRequested());
        }
    }

    @Nested
    @DisplayName("Decision enum")
    class DecisionEnum {

        @Test
        @DisplayName("Has all expected values")
        void hasAllExpectedValues() {
            ApprovalDecision.Decision[] values = ApprovalDecision.Decision.values();
            assertEquals(3, values.length);
            assertNotNull(ApprovalDecision.Decision.valueOf("APPROVE"));
            assertNotNull(ApprovalDecision.Decision.valueOf("REJECT"));
            assertNotNull(ApprovalDecision.Decision.valueOf("REQUEST_CHANGES"));
        }
    }
}
