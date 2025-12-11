package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.agent.AgentRole;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineMonitoringService")
class PipelineMonitoringServiceTest {

    private PipelineMonitoringService service;
    private PublishingDocument document;
    private List<PipelineEvent> capturedEvents;

    @BeforeEach
    void setUp() {
        service = new PipelineMonitoringService(List.of());
        capturedEvents = new ArrayList<>();
        service.addListener(capturedEvents::add);

        TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
        document = new PublishingDocument(topicBrief);
    }

    @Nested
    @DisplayName("Listener management")
    class ListenerManagement {

        @Test
        @DisplayName("Adds listeners")
        void addsListeners() {
            assertEquals(1, service.getListenerCount());

            service.addListener(event -> {});

            assertEquals(2, service.getListenerCount());
        }

        @Test
        @DisplayName("Removes listeners")
        void removesListeners() {
            PipelineEventListener listener = event -> {};
            service.addListener(listener);

            assertEquals(2, service.getListenerCount());

            service.removeListener(listener);

            assertEquals(1, service.getListenerCount());
        }

        @Test
        @DisplayName("Auto-registers Spring listeners")
        void autoRegistersSpringListeners() {
            PipelineEventListener listener1 = event -> {};
            PipelineEventListener listener2 = event -> {};

            PipelineMonitoringService serviceWithListeners =
                    new PipelineMonitoringService(List.of(listener1, listener2));

            assertEquals(2, serviceWithListeners.getListenerCount());
        }
    }

    @Nested
    @DisplayName("Event emission")
    class EventEmission {

        @Test
        @DisplayName("Emits pipeline started event")
        void emitsPipelineStartedEvent() {
            service.pipelineStarted(document);

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.PIPELINE_STARTED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits phase started event")
        void emitsPhaseStartedEvent() {
            service.phaseStarted(document, DocumentState.CREATED, DocumentState.RESEARCHING);

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.PHASE_STARTED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits phase completed event")
        void emitsPhaseCompletedEvent() {
            service.phaseCompleted(document, DocumentState.RESEARCHING, "Complete");

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.PHASE_COMPLETED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits approval requested event")
        void emitsApprovalRequestedEvent() {
            service.approvalRequested(document, DocumentState.RESEARCHING);

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.APPROVAL_REQUESTED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits approval received event")
        void emitsApprovalReceivedEvent() {
            service.approvalReceived(document, DocumentState.RESEARCHING, true);

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.APPROVAL_RECEIVED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits revision started event")
        void emitsRevisionStartedEvent() {
            service.revisionStarted(document, 1, 3);

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.REVISION_STARTED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits pipeline completed event")
        void emitsPipelineCompletedEvent() {
            service.pipelineCompleted(document, Duration.ofSeconds(5));

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.PIPELINE_COMPLETED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits pipeline failed event")
        void emitsPipelineFailedEvent() {
            service.pipelineFailed(document, DocumentState.RESEARCHING, "Error");

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.PIPELINE_FAILED, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits warning event")
        void emitsWarningEvent() {
            service.warn(document, "Warning message");

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.WARNING, capturedEvents.get(0).type());
        }

        @Test
        @DisplayName("Emits info event")
        void emitsInfoEvent() {
            service.info(document, "Info message");

            assertEquals(1, capturedEvents.size());
            assertEquals(PipelineEvent.EventType.INFO, capturedEvents.get(0).type());
        }
    }

    @Nested
    @DisplayName("Metrics recording")
    class MetricsRecording {

        @Test
        @DisplayName("Records pipeline started in metrics")
        void recordsPipelineStartedInMetrics() {
            service.pipelineStarted(document);

            assertEquals(1, service.getMetrics().getTotalPipelinesStarted());
        }

        @Test
        @DisplayName("Records pipeline completed in metrics")
        void recordsPipelineCompletedInMetrics() {
            service.pipelineCompleted(document, Duration.ofSeconds(5));

            assertEquals(1, service.getMetrics().getTotalPipelinesCompleted());
            assertEquals(5000, service.getMetrics().getTotalProcessingTimeMs());
        }

        @Test
        @DisplayName("Records pipeline failed in metrics")
        void recordsPipelineFailedInMetrics() {
            service.pipelineFailed(document, DocumentState.RESEARCHING, "Error");

            assertEquals(1, service.getMetrics().getTotalPipelinesFailed());
        }

        @Test
        @DisplayName("Records approval requested in metrics")
        void recordsApprovalRequestedInMetrics() {
            service.approvalRequested(document, DocumentState.RESEARCHING);

            assertEquals(1, service.getMetrics().getTotalApprovalsRequested());
        }

        @Test
        @DisplayName("Records approval granted in metrics")
        void recordsApprovalGrantedInMetrics() {
            service.approvalReceived(document, DocumentState.RESEARCHING, true);

            assertEquals(1, service.getMetrics().getTotalApprovalsGranted());
        }

        @Test
        @DisplayName("Records approval rejected in metrics")
        void recordsApprovalRejectedInMetrics() {
            service.approvalReceived(document, DocumentState.RESEARCHING, false);

            assertEquals(1, service.getMetrics().getTotalApprovalsRejected());
        }

        @Test
        @DisplayName("Records revision cycle in metrics")
        void recordsRevisionCycleInMetrics() {
            service.revisionStarted(document, 1, 3);

            assertEquals(1, service.getMetrics().getTotalRevisionCycles());
        }

        @Test
        @DisplayName("Records agent processing in metrics")
        void recordsAgentProcessingInMetrics() {
            service.recordAgentProcessing(AgentRole.WRITER, Duration.ofSeconds(3));

            assertEquals(3000, service.getMetrics().getAgentTotalProcessingTimeMs(AgentRole.WRITER));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("Listener errors do not break event emission")
        void listenerErrorsDoNotBreakEventEmission() {
            AtomicInteger callCount = new AtomicInteger(0);

            // Add a listener that throws
            service.addListener(event -> {
                throw new RuntimeException("Listener error");
            });

            // Add another listener after the throwing one
            service.addListener(event -> callCount.incrementAndGet());

            // Emit an event
            service.pipelineStarted(document);

            // Both original listener and the counting one should have received the event
            // (even though one threw an exception)
            assertEquals(1, capturedEvents.size());
            assertEquals(1, callCount.get());
        }
    }

    @Nested
    @DisplayName("Report generation")
    class ReportGeneration {

        @Test
        @DisplayName("Generates metrics report")
        void generatesMetricsReport() {
            service.pipelineStarted(document);
            service.pipelineCompleted(document, Duration.ofSeconds(5));

            String report = service.generateMetricsReport();

            assertNotNull(report);
            assertTrue(report.contains("Pipeline Statistics"));
        }
    }
}
