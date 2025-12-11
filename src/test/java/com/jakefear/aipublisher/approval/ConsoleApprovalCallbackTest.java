package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.document.*;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsoleApprovalCallback")
class ConsoleApprovalCallbackTest {

    @Nested
    @DisplayName("Approve input")
    class ApproveInput {

        @Test
        @DisplayName("Accepts 'A' for approve")
        void acceptsAForApprove() {
            ConsoleApprovalCallback callback = createCallback("A\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
            assertEquals("console-user", decision.approver());
        }

        @Test
        @DisplayName("Accepts 'APPROVE' for approve")
        void acceptsApproveForApprove() {
            ConsoleApprovalCallback callback = createCallback("APPROVE\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }

        @Test
        @DisplayName("Is case insensitive")
        void isCaseInsensitive() {
            ConsoleApprovalCallback callback = createCallback("a\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }
    }

    @Nested
    @DisplayName("Reject input")
    class RejectInput {

        @Test
        @DisplayName("Accepts 'R' for reject with reason")
        void acceptsRForReject() {
            ConsoleApprovalCallback callback = createCallback("R\nNot good enough\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isRejected());
            assertEquals("Not good enough", decision.feedback());
            assertEquals("console-user", decision.approver());
        }

        @Test
        @DisplayName("Accepts 'REJECT' for reject")
        void acceptsRejectForReject() {
            ConsoleApprovalCallback callback = createCallback("REJECT\nBad content\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isRejected());
            assertEquals("Bad content", decision.feedback());
        }
    }

    @Nested
    @DisplayName("Request changes input")
    class RequestChangesInput {

        @Test
        @DisplayName("Accepts 'C' for changes with feedback")
        void acceptsCForChanges() {
            ConsoleApprovalCallback callback = createCallback("C\nAdd more detail\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.changesRequested());
            assertEquals("Add more detail", decision.feedback());
            assertEquals("console-user", decision.approver());
        }

        @Test
        @DisplayName("Accepts 'CHANGES' for changes")
        void acceptsChangesForChanges() {
            ConsoleApprovalCallback callback = createCallback("CHANGES\nNeeds work\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.changesRequested());
            assertEquals("Needs work", decision.feedback());
        }
    }

    @Nested
    @DisplayName("Invalid input handling")
    class InvalidInputHandling {

        @Test
        @DisplayName("Reprompts on invalid input then accepts valid")
        void repromptsOnInvalidInput() {
            ConsoleApprovalCallback callback = createCallback("X\nINVALID\nA\n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }

        @Test
        @DisplayName("Handles whitespace in input")
        void handlesWhitespaceInInput() {
            ConsoleApprovalCallback callback = createCallback("  A  \n");
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }
    }

    @Nested
    @DisplayName("Timeout handling")
    class TimeoutHandling {

        @Test
        @DisplayName("Throws timeout exception when no input within timeout")
        void throwsTimeoutExceptionWhenNoInput() {
            // Create a reader that will block
            BufferedReader blockingReader = new BufferedReader(new StringReader("")) {
                @Override
                public String readLine() {
                    try {
                        Thread.sleep(10000); // Sleep longer than timeout
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }
            };

            ConsoleApprovalCallback callback = new ConsoleApprovalCallback(
                    Duration.ofMillis(100), blockingReader);
            ApprovalRequest request = createRequest(DocumentState.RESEARCHING);

            assertThrows(ApprovalCallback.ApprovalTimeoutException.class,
                    () -> callback.requestApproval(request));
        }
    }

    @Nested
    @DisplayName("Display information")
    class DisplayInformation {

        @Test
        @DisplayName("Handles request with research brief")
        void handlesRequestWithResearchBrief() {
            ConsoleApprovalCallback callback = createCallback("A\n");
            ApprovalRequest request = createRequestWithResearch();

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }

        @Test
        @DisplayName("Handles request with draft")
        void handlesRequestWithDraft() {
            ConsoleApprovalCallback callback = createCallback("A\n");
            ApprovalRequest request = createRequestWithDraft();

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }

        @Test
        @DisplayName("Handles request with fact check report")
        void handlesRequestWithFactCheckReport() {
            ConsoleApprovalCallback callback = createCallback("A\n");
            ApprovalRequest request = createRequestWithFactCheck();

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }

        @Test
        @DisplayName("Handles request with final article")
        void handlesRequestWithFinalArticle() {
            ConsoleApprovalCallback callback = createCallback("A\n");
            ApprovalRequest request = createRequestWithFinalArticle();

            ApprovalDecision decision = callback.requestApproval(request);

            assertTrue(decision.isApproved());
        }
    }

    private ConsoleApprovalCallback createCallback(String input) {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        return new ConsoleApprovalCallback(Duration.ofMinutes(1), reader);
    }

    private ApprovalRequest createRequest(DocumentState state) {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        if (state != DocumentState.CREATED) {
            doc.transitionTo(state);
        }
        return ApprovalRequest.create(doc, state);
    }

    private ApprovalRequest createRequestWithResearch() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        doc.setResearchBrief(new ResearchBrief(
                List.of(KeyFact.unsourced("Fact 1"), KeyFact.unsourced("Fact 2")),
                List.of(),
                List.of("Intro", "Body"),
                List.of(),
                Map.of(),
                List.of()
        ));
        return ApprovalRequest.create(doc, DocumentState.RESEARCHING);
    }

    private ApprovalRequest createRequestWithDraft() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        doc.setResearchBrief(new ResearchBrief(
                List.of(KeyFact.unsourced("Fact 1")),
                List.of(),
                List.of("Intro"),
                List.of(),
                Map.of(),
                List.of()
        ));
        doc.transitionTo(DocumentState.DRAFTING);
        doc.setDraft(new ArticleDraft(
                "## Test Article\n\nThis is test content that is long enough to show truncation in the preview. " +
                        "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                "Test summary",
                List.of("category1"),
                List.of(),
                Map.of()
        ));
        return ApprovalRequest.create(doc, DocumentState.DRAFTING);
    }

    private ApprovalRequest createRequestWithFactCheck() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        doc.setResearchBrief(new ResearchBrief(
                List.of(KeyFact.unsourced("Fact 1")),
                List.of(),
                List.of("Intro"),
                List.of(),
                Map.of(),
                List.of()
        ));
        doc.transitionTo(DocumentState.DRAFTING);
        doc.setDraft(new ArticleDraft(
                "## Test Article\n\nContent.",
                "Test summary",
                List.of(),
                List.of(),
                Map.of()
        ));
        doc.transitionTo(DocumentState.FACT_CHECKING);
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().markdownContent(),
                List.of(VerifiedClaim.verified("Claim 1", 0)),
                List.of(QuestionableClaim.withoutSuggestion("Claim 2", "Needs verification")),
                List.of(),
                ConfidenceLevel.MEDIUM,
                RecommendedAction.REVISE
        ));
        return ApprovalRequest.create(doc, DocumentState.FACT_CHECKING);
    }

    private ApprovalRequest createRequestWithFinalArticle() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        doc.setResearchBrief(new ResearchBrief(
                List.of(KeyFact.unsourced("Fact 1")),
                List.of(),
                List.of("Intro"),
                List.of(),
                Map.of(),
                List.of()
        ));
        doc.transitionTo(DocumentState.DRAFTING);
        doc.setDraft(new ArticleDraft(
                "## Test Article\n\nContent.",
                "Test summary",
                List.of(),
                List.of(),
                Map.of()
        ));
        doc.transitionTo(DocumentState.FACT_CHECKING);
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().markdownContent(),
                List.of(VerifiedClaim.verified("Claim 1", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        ));
        doc.transitionTo(DocumentState.EDITING);
        doc.setFinalArticle(new FinalArticle(
                "## Final Article\n\nPolished content.",
                DocumentMetadata.create("Test Topic", "Test summary"),
                "Edited for style",
                0.85,
                List.of()
        ));
        return ApprovalRequest.create(doc, DocumentState.EDITING);
    }
}
