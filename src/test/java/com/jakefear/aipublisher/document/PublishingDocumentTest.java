package com.jakefear.aipublisher.document;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PublishingDocument")
class PublishingDocumentTest {

    private TopicBrief topicBrief;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        topicBrief = TopicBrief.simple("Apache Kafka", "developers", 1500);
        document = new PublishingDocument(topicBrief);
    }

    @Nested
    @DisplayName("Creation")
    class Creation {

        @Test
        @DisplayName("Creates document with generated ID")
        void createsWithGeneratedId() {
            assertNotNull(document.getId());
        }

        @Test
        @DisplayName("Creates document in CREATED state")
        void createsInCreatedState() {
            assertEquals(DocumentState.CREATED, document.getState());
        }

        @Test
        @DisplayName("Generates CamelCase page name from topic")
        void generatesCamelCasePageName() {
            assertEquals("ApacheKafka", document.getPageName());
        }

        @Test
        @DisplayName("Sets title from topic")
        void setsTitleFromTopic() {
            assertEquals("Apache Kafka", document.getTitle());
        }

        @Test
        @DisplayName("Stores topic brief")
        void storesTopicBrief() {
            assertEquals(topicBrief, document.getTopicBrief());
        }

        @Test
        @DisplayName("Sets creation timestamp")
        void setsCreationTimestamp() {
            assertNotNull(document.getCreatedAt());
        }

        @Test
        @DisplayName("Initializes with empty contributions")
        void initializesWithEmptyContributions() {
            assertTrue(document.getContributions().isEmpty());
        }

        @Test
        @DisplayName("Initializes with zero revision cycles")
        void initializesWithZeroRevisions() {
            assertEquals(0, document.getRevisionCycleCount());
        }

        @Test
        @DisplayName("Throws on null topic brief")
        void throwsOnNullTopicBrief() {
            assertThrows(NullPointerException.class, () -> new PublishingDocument(null));
        }
    }

    @Nested
    @DisplayName("Page Name Generation")
    class PageNameGeneration {

        @Test
        @DisplayName("Converts spaces to CamelCase")
        void convertsSpacesToCamelCase() {
            var brief = TopicBrief.simple("event driven architecture", "developers", 1000);
            var doc = new PublishingDocument(brief);
            assertEquals("EventDrivenArchitecture", doc.getPageName());
        }

        @Test
        @DisplayName("Handles hyphens")
        void handlesHyphens() {
            var brief = TopicBrief.simple("real-time streaming", "developers", 1000);
            var doc = new PublishingDocument(brief);
            assertEquals("RealTimeStreaming", doc.getPageName());
        }

        @Test
        @DisplayName("Handles underscores")
        void handlesUnderscores() {
            var brief = TopicBrief.simple("message_queue_basics", "developers", 1000);
            var doc = new PublishingDocument(brief);
            assertEquals("MessageQueueBasics", doc.getPageName());
        }

        @Test
        @DisplayName("Removes special characters")
        void removesSpecialCharacters() {
            var brief = TopicBrief.simple("What is Kafka?", "developers", 1000);
            var doc = new PublishingDocument(brief);
            assertEquals("WhatIsKafka", doc.getPageName());
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("Transitions from CREATED to RESEARCHING")
        void transitionsToResearching() {
            document.transitionTo(DocumentState.RESEARCHING);
            assertEquals(DocumentState.RESEARCHING, document.getState());
        }

        @Test
        @DisplayName("Advances through happy path")
        void advancesThroughHappyPath() {
            document.advanceToNextState(); // CREATED -> RESEARCHING
            assertEquals(DocumentState.RESEARCHING, document.getState());

            document.advanceToNextState(); // RESEARCHING -> DRAFTING
            assertEquals(DocumentState.DRAFTING, document.getState());

            document.advanceToNextState(); // DRAFTING -> FACT_CHECKING
            assertEquals(DocumentState.FACT_CHECKING, document.getState());

            document.advanceToNextState(); // FACT_CHECKING -> EDITING
            assertEquals(DocumentState.EDITING, document.getState());

            document.advanceToNextState(); // EDITING -> CRITIQUING
            assertEquals(DocumentState.CRITIQUING, document.getState());

            document.advanceToNextState(); // CRITIQUING -> PUBLISHED
            assertEquals(DocumentState.PUBLISHED, document.getState());
        }

        @Test
        @DisplayName("Throws on invalid transition")
        void throwsOnInvalidTransition() {
            assertThrows(IllegalStateException.class,
                    () -> document.transitionTo(DocumentState.EDITING));
        }

        @Test
        @DisplayName("Throws when advancing from terminal state")
        void throwsWhenAdvancingFromTerminal() {
            document.transitionTo(DocumentState.REJECTED);
            assertThrows(IllegalStateException.class, () -> document.advanceToNextState());
        }

        @Test
        @DisplayName("Updates timestamp on transition")
        void updatesTimestampOnTransition() throws InterruptedException {
            var before = document.getUpdatedAt();
            Thread.sleep(10);
            document.transitionTo(DocumentState.RESEARCHING);
            assertTrue(document.getUpdatedAt().isAfter(before));
        }
    }

    @Nested
    @DisplayName("Revision Handling")
    class RevisionHandling {

        @BeforeEach
        void advanceToFactChecking() {
            document.transitionTo(DocumentState.RESEARCHING);
            document.transitionTo(DocumentState.DRAFTING);
            document.transitionTo(DocumentState.FACT_CHECKING);
        }

        @Test
        @DisplayName("Reverts to previous state")
        void revertsToPreviousState() {
            document.revertForRevision();
            assertEquals(DocumentState.DRAFTING, document.getState());
        }

        @Test
        @DisplayName("Increments revision count on revert")
        void incrementsRevisionCount() {
            assertEquals(0, document.getRevisionCycleCount());
            document.revertForRevision();
            assertEquals(1, document.getRevisionCycleCount());
        }

        @Test
        @DisplayName("Tracks multiple revision cycles")
        void tracksMultipleRevisions() {
            document.revertForRevision(); // Back to DRAFTING
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.revertForRevision(); // Back to DRAFTING again

            assertEquals(2, document.getRevisionCycleCount());
        }

        @Test
        @DisplayName("canRevise returns true when under limit")
        void canReviseWhenUnderLimit() {
            assertTrue(document.canRevise(3));
        }

        @Test
        @DisplayName("canRevise returns false when at limit")
        void cannotReviseAtLimit() {
            document.revertForRevision();
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.revertForRevision();
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.revertForRevision();

            assertFalse(document.canRevise(3));
        }

        @Test
        @DisplayName("canRevise returns false when terminal")
        void cannotReviseWhenTerminal() {
            document.transitionTo(DocumentState.REJECTED);
            assertFalse(document.canRevise(10));
        }
    }

    @Nested
    @DisplayName("Content Setting")
    class ContentSetting {

        @Test
        @DisplayName("Sets research brief in RESEARCHING state")
        void setsResearchBriefInResearchingState() {
            document.transitionTo(DocumentState.RESEARCHING);

            var researchBrief = new ResearchBrief(
                    List.of(KeyFact.unsourced("Kafka is a streaming platform")),
                    List.of(),
                    List.of("Introduction", "Core Concepts"),
                    List.of(),
                    Map.of(),
                    List.of()
            );

            document.setResearchBrief(researchBrief);
            assertEquals(researchBrief, document.getResearchBrief());
        }

        @Test
        @DisplayName("Throws when setting research brief in wrong state")
        void throwsWhenSettingResearchBriefInWrongState() {
            var researchBrief = new ResearchBrief(
                    List.of(KeyFact.unsourced("fact")),
                    List.of(),
                    List.of("Outline"),
                    List.of(),
                    Map.of(),
                    List.of()
            );

            assertThrows(IllegalStateException.class,
                    () -> document.setResearchBrief(researchBrief));
        }

        @Test
        @DisplayName("Sets draft in DRAFTING state")
        void setsDraftInDraftingState() {
            document.transitionTo(DocumentState.RESEARCHING);
            document.transitionTo(DocumentState.DRAFTING);

            var draft = new ArticleDraft(
                    "## Apache Kafka\n\nContent here...",
                    "Introduction to Apache Kafka",
                    List.of(),
                    List.of(),
                    Map.of()
            );

            document.setDraft(draft);
            assertEquals(draft, document.getDraft());
        }

        @Test
        @DisplayName("Throws when setting draft in wrong state")
        void throwsWhenSettingDraftInWrongState() {
            var draft = new ArticleDraft("content", "summary", List.of(), List.of(), Map.of());
            assertThrows(IllegalStateException.class, () -> document.setDraft(draft));
        }

        @Test
        @DisplayName("Sets fact check report in FACT_CHECKING state")
        void setsFactCheckReportInFactCheckingState() {
            document.transitionTo(DocumentState.RESEARCHING);
            document.transitionTo(DocumentState.DRAFTING);
            document.transitionTo(DocumentState.FACT_CHECKING);

            var report = new FactCheckReport(
                    "annotated content",
                    List.of(),
                    List.of(),
                    List.of(),
                    ConfidenceLevel.HIGH,
                    RecommendedAction.APPROVE
            );

            document.setFactCheckReport(report);
            assertEquals(report, document.getFactCheckReport());
        }

        @Test
        @DisplayName("Sets final article in EDITING state")
        void setsFinalArticleInEditingState() {
            document.transitionTo(DocumentState.RESEARCHING);
            document.transitionTo(DocumentState.DRAFTING);
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.transitionTo(DocumentState.EDITING);

            var metadata = DocumentMetadata.create("Apache Kafka", "Introduction to Kafka");
            var finalArticle = new FinalArticle(
                    "## Apache Kafka\n\nFinal content...",
                    metadata,
                    "Polished for publication",
                    0.95,
                    List.of()
            );

            document.setFinalArticle(finalArticle);
            assertEquals(finalArticle, document.getFinalArticle());
        }
    }

    @Nested
    @DisplayName("Contributions")
    class Contributions {

        @Test
        @DisplayName("Adds contribution to history")
        void addsContribution() {
            var contribution = AgentContribution.create("RESEARCHER", Duration.ofSeconds(5));
            document.addContribution(contribution);

            assertEquals(1, document.getContributions().size());
            assertEquals(contribution, document.getContributions().get(0));
        }

        @Test
        @DisplayName("Returns unmodifiable contributions list")
        void returnsUnmodifiableContributions() {
            assertThrows(UnsupportedOperationException.class,
                    () -> document.getContributions().add(
                            AgentContribution.create("TEST", Duration.ZERO)));
        }
    }

    @Nested
    @DisplayName("Status Helpers")
    class StatusHelpers {

        @Test
        @DisplayName("isComplete returns false for non-terminal states")
        void isCompleteReturnsFalseForNonTerminal() {
            assertFalse(document.isComplete());
        }

        @Test
        @DisplayName("isComplete returns true for PUBLISHED")
        void isCompleteReturnsTrueForPublished() {
            document.transitionTo(DocumentState.RESEARCHING);
            document.transitionTo(DocumentState.DRAFTING);
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.transitionTo(DocumentState.EDITING);
            document.transitionTo(DocumentState.CRITIQUING);
            document.transitionTo(DocumentState.PUBLISHED);

            assertTrue(document.isComplete());
            assertTrue(document.isPublished());
        }

        @Test
        @DisplayName("isComplete returns true for REJECTED")
        void isCompleteReturnsTrueForRejected() {
            document.reject();

            assertTrue(document.isComplete());
            assertTrue(document.isRejected());
        }
    }
}
