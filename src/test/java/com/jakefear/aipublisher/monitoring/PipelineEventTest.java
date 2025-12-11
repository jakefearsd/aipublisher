package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineEvent")
class PipelineEventTest {

    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        document = new PublishingDocument(topicBrief);
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("Creates pipeline started event")
        void createsPipelineStartedEvent() {
            PipelineEvent event = PipelineEvent.pipelineStarted(document);

            assertNotNull(event.id());
            assertEquals(PipelineEvent.EventType.PIPELINE_STARTED, event.type());
            assertEquals("Test Topic", event.topic());
            assertNull(event.previousState());
            assertEquals(DocumentState.CREATED, event.currentState());
            assertTrue(event.message().contains("Pipeline started"));
            assertNotNull(event.timestamp());
            assertSame(document, event.document());
        }

        @Test
        @DisplayName("Creates phase started event")
        void createsPhaseStartedEvent() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.phaseStarted(
                    document, DocumentState.CREATED, DocumentState.RESEARCHING);

            assertEquals(PipelineEvent.EventType.PHASE_STARTED, event.type());
            assertEquals(DocumentState.CREATED, event.previousState());
            assertEquals(DocumentState.RESEARCHING, event.currentState());
            assertTrue(event.message().contains("RESEARCHING"));
        }

        @Test
        @DisplayName("Creates phase completed event")
        void createsPhaseCompletedEvent() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.phaseCompleted(
                    document, DocumentState.RESEARCHING, "Research complete: 5 facts");

            assertEquals(PipelineEvent.EventType.PHASE_COMPLETED, event.type());
            assertEquals(DocumentState.RESEARCHING, event.currentState());
            assertEquals("Research complete: 5 facts", event.message());
        }

        @Test
        @DisplayName("Creates approval requested event")
        void createsApprovalRequestedEvent() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.approvalRequested(
                    document, DocumentState.RESEARCHING);

            assertEquals(PipelineEvent.EventType.APPROVAL_REQUESTED, event.type());
            assertTrue(event.message().contains("RESEARCHING"));
        }

        @Test
        @DisplayName("Creates approval received event - approved")
        void createsApprovalReceivedEventApproved() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.approvalReceived(
                    document, DocumentState.RESEARCHING, true);

            assertEquals(PipelineEvent.EventType.APPROVAL_RECEIVED, event.type());
            assertTrue(event.message().contains("Approved"));
        }

        @Test
        @DisplayName("Creates approval received event - changes requested")
        void createsApprovalReceivedEventChangesRequested() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.approvalReceived(
                    document, DocumentState.RESEARCHING, false);

            assertEquals(PipelineEvent.EventType.APPROVAL_RECEIVED, event.type());
            assertTrue(event.message().contains("Changes requested"));
        }

        @Test
        @DisplayName("Creates revision started event")
        void createsRevisionStartedEvent() {
            PipelineEvent event = PipelineEvent.revisionStarted(document, 2, 3);

            assertEquals(PipelineEvent.EventType.REVISION_STARTED, event.type());
            assertTrue(event.message().contains("2/3"));
        }

        @Test
        @DisplayName("Creates pipeline completed event")
        void createsPipelineCompletedEvent() {
            PipelineEvent event = PipelineEvent.pipelineCompleted(document, 5000);

            assertEquals(PipelineEvent.EventType.PIPELINE_COMPLETED, event.type());
            assertEquals(DocumentState.PUBLISHED, event.currentState());
            assertTrue(event.message().contains("5000 ms"));
        }

        @Test
        @DisplayName("Creates pipeline failed event")
        void createsPipelineFailedEvent() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.pipelineFailed(
                    document, DocumentState.RESEARCHING, "Research failed");

            assertEquals(PipelineEvent.EventType.PIPELINE_FAILED, event.type());
            assertTrue(event.message().contains("Research failed"));
            assertTrue(event.message().contains("RESEARCHING"));
        }

        @Test
        @DisplayName("Creates warning event")
        void createsWarningEvent() {
            document.transitionTo(DocumentState.RESEARCHING);

            PipelineEvent event = PipelineEvent.warning(document, "Low confidence");

            assertEquals(PipelineEvent.EventType.WARNING, event.type());
            assertEquals("Low confidence", event.message());
        }

        @Test
        @DisplayName("Creates info event")
        void createsInfoEvent() {
            PipelineEvent event = PipelineEvent.info(document, "Processing started");

            assertEquals(PipelineEvent.EventType.INFO, event.type());
            assertEquals("Processing started", event.message());
        }
    }

    @Nested
    @DisplayName("EventType enum")
    class EventTypeEnum {

        @Test
        @DisplayName("Has all expected event types")
        void hasAllExpectedEventTypes() {
            PipelineEvent.EventType[] types = PipelineEvent.EventType.values();
            assertEquals(10, types.length);

            assertNotNull(PipelineEvent.EventType.valueOf("PIPELINE_STARTED"));
            assertNotNull(PipelineEvent.EventType.valueOf("PHASE_STARTED"));
            assertNotNull(PipelineEvent.EventType.valueOf("PHASE_COMPLETED"));
            assertNotNull(PipelineEvent.EventType.valueOf("APPROVAL_REQUESTED"));
            assertNotNull(PipelineEvent.EventType.valueOf("APPROVAL_RECEIVED"));
            assertNotNull(PipelineEvent.EventType.valueOf("REVISION_STARTED"));
            assertNotNull(PipelineEvent.EventType.valueOf("PIPELINE_COMPLETED"));
            assertNotNull(PipelineEvent.EventType.valueOf("PIPELINE_FAILED"));
            assertNotNull(PipelineEvent.EventType.valueOf("WARNING"));
            assertNotNull(PipelineEvent.EventType.valueOf("INFO"));
        }
    }
}
