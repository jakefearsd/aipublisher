package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AutoApprovalCallback")
class AutoApprovalCallbackTest {

    private AutoApprovalCallback callback;

    @BeforeEach
    void setUp() {
        callback = new AutoApprovalCallback();
    }

    @Test
    @DisplayName("Always approves requests")
    void alwaysApprovesRequests() {
        // Arrange
        ApprovalRequest request = createRequest();

        // Act
        ApprovalDecision decision = callback.requestApproval(request);

        // Assert
        assertTrue(decision.isApproved());
        assertFalse(decision.isRejected());
        assertFalse(decision.changesRequested());
    }

    @Test
    @DisplayName("Returns auto-approve as approver")
    void returnsAutoApproveAsApprover() {
        // Arrange
        ApprovalRequest request = createRequest();

        // Act
        ApprovalDecision decision = callback.requestApproval(request);

        // Assert
        assertEquals("auto-approve", decision.approver());
    }

    @Test
    @DisplayName("Returns request ID in decision")
    void returnsRequestIdInDecision() {
        // Arrange
        ApprovalRequest request = createRequest();

        // Act
        ApprovalDecision decision = callback.requestApproval(request);

        // Assert
        assertEquals(request.id(), decision.requestId());
    }

    @Test
    @DisplayName("Sets decision timestamp")
    void setsDecisionTimestamp() {
        // Arrange
        ApprovalRequest request = createRequest();

        // Act
        ApprovalDecision decision = callback.requestApproval(request);

        // Assert
        assertNotNull(decision.decidedAt());
    }

    private ApprovalRequest createRequest() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        return ApprovalRequest.create(doc, DocumentState.RESEARCHING);
    }
}
