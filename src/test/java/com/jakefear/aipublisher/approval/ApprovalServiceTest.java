package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApprovalService")
class ApprovalServiceTest {

    @Mock
    private ApprovalCallback approvalCallback;

    private PipelineProperties pipelineProperties;
    private ApprovalService approvalService;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties();
        approvalService = new ApprovalService(pipelineProperties, approvalCallback);
    }

    @Nested
    @DisplayName("isApprovalRequired")
    class IsApprovalRequired {

        @Test
        @DisplayName("Returns false for RESEARCHING when afterResearch is false")
        void returnsFalseForResearchingWhenNotRequired() {
            pipelineProperties.getApproval().setAfterResearch(false);
            assertFalse(approvalService.isApprovalRequired(DocumentState.RESEARCHING));
        }

        @Test
        @DisplayName("Returns true for RESEARCHING when afterResearch is true")
        void returnsTrueForResearchingWhenRequired() {
            pipelineProperties.getApproval().setAfterResearch(true);
            assertTrue(approvalService.isApprovalRequired(DocumentState.RESEARCHING));
        }

        @Test
        @DisplayName("Returns false for DRAFTING when afterDraft is false")
        void returnsFalseForDraftingWhenNotRequired() {
            pipelineProperties.getApproval().setAfterDraft(false);
            assertFalse(approvalService.isApprovalRequired(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("Returns true for DRAFTING when afterDraft is true")
        void returnsTrueForDraftingWhenRequired() {
            pipelineProperties.getApproval().setAfterDraft(true);
            assertTrue(approvalService.isApprovalRequired(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("Returns false for FACT_CHECKING when afterFactcheck is false")
        void returnsFalseForFactCheckingWhenNotRequired() {
            pipelineProperties.getApproval().setAfterFactcheck(false);
            assertFalse(approvalService.isApprovalRequired(DocumentState.FACT_CHECKING));
        }

        @Test
        @DisplayName("Returns true for FACT_CHECKING when afterFactcheck is true")
        void returnsTrueForFactCheckingWhenRequired() {
            pipelineProperties.getApproval().setAfterFactcheck(true);
            assertTrue(approvalService.isApprovalRequired(DocumentState.FACT_CHECKING));
        }

        @Test
        @DisplayName("Returns true for EDITING when beforePublish is true (default)")
        void returnsTrueForEditingWhenRequired() {
            // beforePublish defaults to true
            assertTrue(approvalService.isApprovalRequired(DocumentState.EDITING));
        }

        @Test
        @DisplayName("Returns false for EDITING when beforePublish is false")
        void returnsFalseForEditingWhenNotRequired() {
            pipelineProperties.getApproval().setBeforePublish(false);
            assertFalse(approvalService.isApprovalRequired(DocumentState.EDITING));
        }

        @Test
        @DisplayName("Returns false for CREATED state")
        void returnsFalseForCreatedState() {
            assertFalse(approvalService.isApprovalRequired(DocumentState.CREATED));
        }

        @Test
        @DisplayName("Returns false for PUBLISHED state")
        void returnsFalseForPublishedState() {
            assertFalse(approvalService.isApprovalRequired(DocumentState.PUBLISHED));
        }
    }

    @Nested
    @DisplayName("requestApproval")
    class RequestApproval {

        @Test
        @DisplayName("Skips callback when approval not required")
        void skipsCallbackWhenNotRequired() {
            // Arrange
            pipelineProperties.getApproval().setAfterResearch(false);
            PublishingDocument document = createDocument(DocumentState.RESEARCHING);

            // Act
            ApprovalDecision decision = approvalService.requestApproval(document);

            // Assert
            assertTrue(decision.isApproved());
            assertEquals("not-required", decision.approver());
            verifyNoInteractions(approvalCallback);
        }

        @Test
        @DisplayName("Calls callback when approval is required")
        void callsCallbackWhenRequired() {
            // Arrange
            pipelineProperties.getApproval().setBeforePublish(true);
            PublishingDocument document = createDocument(DocumentState.EDITING);

            when(approvalCallback.requestApproval(any()))
                    .thenReturn(ApprovalDecision.approve(UUID.randomUUID(), "test-approver"));

            // Act
            ApprovalDecision decision = approvalService.requestApproval(document);

            // Assert
            assertTrue(decision.isApproved());
            assertEquals("test-approver", decision.approver());
            verify(approvalCallback).requestApproval(any());
        }
    }

    @Nested
    @DisplayName("checkAndApprove")
    class CheckAndApprove {

        @Test
        @DisplayName("Returns true when approval not required")
        void returnsTrueWhenNotRequired() {
            // Arrange
            pipelineProperties.getApproval().setAfterResearch(false);
            PublishingDocument document = createDocument(DocumentState.RESEARCHING);

            // Act
            boolean result = approvalService.checkAndApprove(document);

            // Assert
            assertTrue(result);
            verifyNoInteractions(approvalCallback);
        }

        @Test
        @DisplayName("Returns true when approved")
        void returnsTrueWhenApproved() {
            // Arrange
            pipelineProperties.getApproval().setBeforePublish(true);
            PublishingDocument document = createDocument(DocumentState.EDITING);

            when(approvalCallback.requestApproval(any()))
                    .thenReturn(ApprovalDecision.approve(UUID.randomUUID(), "test-approver"));

            // Act
            boolean result = approvalService.checkAndApprove(document);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("Returns false when changes requested")
        void returnsFalseWhenChangesRequested() {
            // Arrange
            pipelineProperties.getApproval().setBeforePublish(true);
            PublishingDocument document = createDocument(DocumentState.EDITING);

            when(approvalCallback.requestApproval(any()))
                    .thenReturn(ApprovalDecision.requestChanges(
                            UUID.randomUUID(), "test-approver", "Needs more detail"));

            // Act
            boolean result = approvalService.checkAndApprove(document);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("Throws exception when rejected")
        void throwsExceptionWhenRejected() {
            // Arrange
            pipelineProperties.getApproval().setBeforePublish(true);
            PublishingDocument document = createDocument(DocumentState.EDITING);

            when(approvalCallback.requestApproval(any()))
                    .thenReturn(ApprovalDecision.reject(
                            UUID.randomUUID(), "test-approver", "Not acceptable"));

            // Act & Assert
            ApprovalService.ApprovalRejectedException ex = assertThrows(
                    ApprovalService.ApprovalRejectedException.class,
                    () -> approvalService.checkAndApprove(document)
            );

            assertEquals(DocumentState.EDITING, ex.getState());
            assertTrue(ex.getMessage().contains("test-approver"));
            assertTrue(ex.getMessage().contains("Not acceptable"));
        }
    }

    private PublishingDocument createDocument(DocumentState state) {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);

        // Follow proper state transitions
        if (state == DocumentState.CREATED) {
            return doc;
        }
        doc.transitionTo(DocumentState.RESEARCHING);
        if (state == DocumentState.RESEARCHING) {
            return doc;
        }
        doc.transitionTo(DocumentState.DRAFTING);
        if (state == DocumentState.DRAFTING) {
            return doc;
        }
        doc.transitionTo(DocumentState.FACT_CHECKING);
        if (state == DocumentState.FACT_CHECKING) {
            return doc;
        }
        doc.transitionTo(DocumentState.EDITING);
        if (state == DocumentState.EDITING) {
            return doc;
        }
        doc.transitionTo(DocumentState.PUBLISHED);
        return doc;
    }
}
