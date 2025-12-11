package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.document.*;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApprovalRequest")
class ApprovalRequestTest {

    @Test
    @DisplayName("Creates request with unique ID")
    void createsRequestWithUniqueId() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);

        ApprovalRequest request1 = ApprovalRequest.create(doc, DocumentState.RESEARCHING);
        ApprovalRequest request2 = ApprovalRequest.create(doc, DocumentState.RESEARCHING);

        assertNotEquals(request1.id(), request2.id());
    }

    @Test
    @DisplayName("Captures document and state")
    void capturesDocumentAndState() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);

        ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.RESEARCHING);

        assertSame(doc, request.document());
        assertEquals(DocumentState.RESEARCHING, request.atState());
        assertNotNull(request.requestedAt());
    }

    @Nested
    @DisplayName("Summary generation")
    class SummaryGeneration {

        @Test
        @DisplayName("Generates summary for RESEARCHING state")
        void generatesSummaryForResearching() {
            PublishingDocument doc = createDocumentWithResearch();

            ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.RESEARCHING);

            assertTrue(request.summary().contains("Research complete"));
            assertTrue(request.summary().contains("Test Topic"));
            assertTrue(request.summary().contains("3 key facts"));
        }

        @Test
        @DisplayName("Generates summary for DRAFTING state")
        void generatesSummaryForDrafting() {
            PublishingDocument doc = createDocumentWithDraft();

            ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.DRAFTING);

            assertTrue(request.summary().contains("Draft ready"));
            assertTrue(request.summary().contains("Test Topic"));
            assertTrue(request.summary().contains("words"));
        }

        @Test
        @DisplayName("Generates summary for FACT_CHECKING state")
        void generatesSummaryForFactChecking() {
            PublishingDocument doc = createDocumentWithFactCheck();

            ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.FACT_CHECKING);

            assertTrue(request.summary().contains("Fact check complete"));
            assertTrue(request.summary().contains("Test Topic"));
        }

        @Test
        @DisplayName("Generates summary for EDITING state")
        void generatesSummaryForEditing() {
            PublishingDocument doc = createDocumentWithFinalArticle();

            ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.EDITING);

            assertTrue(request.summary().contains("Ready to publish"));
            assertTrue(request.summary().contains("Test Topic"));
            assertTrue(request.summary().contains("quality score"));
        }

        @Test
        @DisplayName("Handles missing data gracefully")
        void handlesMissingDataGracefully() {
            TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
            PublishingDocument doc = new PublishingDocument(topicBrief);
            doc.transitionTo(DocumentState.RESEARCHING);

            ApprovalRequest request = ApprovalRequest.create(doc, DocumentState.RESEARCHING);

            assertNotNull(request.summary());
            assertTrue(request.summary().contains("0 key facts"));
        }
    }

    private PublishingDocument createDocumentWithResearch() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        PublishingDocument doc = new PublishingDocument(topicBrief);
        doc.transitionTo(DocumentState.RESEARCHING);
        doc.setResearchBrief(new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Fact 1"),
                        KeyFact.unsourced("Fact 2"),
                        KeyFact.unsourced("Fact 3")
                ),
                List.of(),
                List.of("Intro", "Body"),
                List.of(),
                Map.of(),
                List.of()
        ));
        return doc;
    }

    private PublishingDocument createDocumentWithDraft() {
        PublishingDocument doc = createDocumentWithResearch();
        doc.transitionTo(DocumentState.DRAFTING);
        doc.setDraft(new ArticleDraft(
                "## Test Article\n\nThis is some test content for the article.",
                "Test summary",
                List.of(),
                List.of(),
                Map.of()
        ));
        return doc;
    }

    private PublishingDocument createDocumentWithFactCheck() {
        PublishingDocument doc = createDocumentWithDraft();
        doc.transitionTo(DocumentState.FACT_CHECKING);
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().markdownContent(),
                List.of(VerifiedClaim.verified("Test claim", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        ));
        return doc;
    }

    private PublishingDocument createDocumentWithFinalArticle() {
        PublishingDocument doc = createDocumentWithFactCheck();
        doc.transitionTo(DocumentState.EDITING);
        doc.setFinalArticle(new FinalArticle(
                "## Final Article\n\nPolished content.",
                DocumentMetadata.create("Test Topic", "Test summary"),
                "Edit summary",
                0.85,
                List.of()
        ));
        return doc;
    }
}
