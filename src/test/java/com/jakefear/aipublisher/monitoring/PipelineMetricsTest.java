package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.agent.AgentRole;
import com.jakefear.aipublisher.document.DocumentState;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineMetrics")
class PipelineMetricsTest {

    private PipelineMetrics metrics;

    @BeforeEach
    void setUp() {
        metrics = new PipelineMetrics();
    }

    @Nested
    @DisplayName("Pipeline statistics")
    class PipelineStatistics {

        @Test
        @DisplayName("Tracks pipelines started")
        void tracksPipelinesStarted() {
            assertEquals(0, metrics.getTotalPipelinesStarted());

            metrics.recordPipelineStarted();
            metrics.recordPipelineStarted();

            assertEquals(2, metrics.getTotalPipelinesStarted());
        }

        @Test
        @DisplayName("Tracks pipelines completed")
        void tracksPipelinesCompleted() {
            assertEquals(0, metrics.getTotalPipelinesCompleted());

            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(10));

            assertEquals(2, metrics.getTotalPipelinesCompleted());
        }

        @Test
        @DisplayName("Tracks pipelines failed")
        void tracksPipelinesFailed() {
            assertEquals(0, metrics.getTotalPipelinesFailed());

            metrics.recordPipelineFailed(DocumentState.RESEARCHING);
            metrics.recordPipelineFailed(DocumentState.DRAFTING);

            assertEquals(2, metrics.getTotalPipelinesFailed());
        }

        @Test
        @DisplayName("Tracks failures by state")
        void tracksFailuresByState() {
            metrics.recordPipelineFailed(DocumentState.RESEARCHING);
            metrics.recordPipelineFailed(DocumentState.RESEARCHING);
            metrics.recordPipelineFailed(DocumentState.DRAFTING);

            Map<DocumentState, Integer> failures = metrics.getFailuresByState();
            assertEquals(2, failures.get(DocumentState.RESEARCHING));
            assertEquals(1, failures.get(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("Tracks revision cycles")
        void tracksRevisionCycles() {
            assertEquals(0, metrics.getTotalRevisionCycles());

            metrics.recordRevisionCycle();
            metrics.recordRevisionCycle();
            metrics.recordRevisionCycle();

            assertEquals(3, metrics.getTotalRevisionCycles());
        }
    }

    @Nested
    @DisplayName("Approval statistics")
    class ApprovalStatistics {

        @Test
        @DisplayName("Tracks approvals requested")
        void tracksApprovalsRequested() {
            assertEquals(0, metrics.getTotalApprovalsRequested());

            metrics.recordApprovalRequested();
            metrics.recordApprovalRequested();

            assertEquals(2, metrics.getTotalApprovalsRequested());
        }

        @Test
        @DisplayName("Tracks approvals granted")
        void tracksApprovalsGranted() {
            assertEquals(0, metrics.getTotalApprovalsGranted());

            metrics.recordApprovalGranted();

            assertEquals(1, metrics.getTotalApprovalsGranted());
        }

        @Test
        @DisplayName("Tracks approvals rejected")
        void tracksApprovalsRejected() {
            assertEquals(0, metrics.getTotalApprovalsRejected());

            metrics.recordApprovalRejected();
            metrics.recordApprovalRejected();

            assertEquals(2, metrics.getTotalApprovalsRejected());
        }
    }

    @Nested
    @DisplayName("Processing time statistics")
    class ProcessingTimeStatistics {

        @Test
        @DisplayName("Tracks total processing time")
        void tracksTotalProcessingTime() {
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(10));

            assertEquals(15000, metrics.getTotalProcessingTimeMs());
        }

        @Test
        @DisplayName("Tracks min processing time")
        void tracksMinProcessingTime() {
            metrics.recordPipelineCompleted(Duration.ofSeconds(10));
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(8));

            assertEquals(5000, metrics.getMinProcessingTimeMs());
        }

        @Test
        @DisplayName("Tracks max processing time")
        void tracksMaxProcessingTime() {
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(10));
            metrics.recordPipelineCompleted(Duration.ofSeconds(7));

            assertEquals(10000, metrics.getMaxProcessingTimeMs());
        }

        @Test
        @DisplayName("Calculates average processing time")
        void calculatesAverageProcessingTime() {
            metrics.recordPipelineCompleted(Duration.ofSeconds(4));
            metrics.recordPipelineCompleted(Duration.ofSeconds(6));

            assertEquals(5000.0, metrics.getAverageProcessingTimeMs());
        }

        @Test
        @DisplayName("Returns zero for min when no completions")
        void returnsZeroForMinWhenNoCompletions() {
            assertEquals(0, metrics.getMinProcessingTimeMs());
        }

        @Test
        @DisplayName("Returns zero for average when no completions")
        void returnsZeroForAverageWhenNoCompletions() {
            assertEquals(0.0, metrics.getAverageProcessingTimeMs());
        }
    }

    @Nested
    @DisplayName("Success rate")
    class SuccessRate {

        @Test
        @DisplayName("Calculates success rate correctly")
        void calculatesSuccessRateCorrectly() {
            metrics.recordPipelineStarted();
            metrics.recordPipelineStarted();
            metrics.recordPipelineStarted();
            metrics.recordPipelineStarted();

            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineFailed(DocumentState.RESEARCHING);

            assertEquals(0.75, metrics.getSuccessRate());
        }

        @Test
        @DisplayName("Returns zero when no pipelines started")
        void returnsZeroWhenNoPipelinesStarted() {
            assertEquals(0.0, metrics.getSuccessRate());
        }
    }

    @Nested
    @DisplayName("Agent metrics")
    class AgentMetrics {

        @Test
        @DisplayName("Tracks agent processing time")
        void tracksAgentProcessingTime() {
            metrics.recordAgentProcessing(AgentRole.RESEARCHER, Duration.ofSeconds(3));
            metrics.recordAgentProcessing(AgentRole.RESEARCHER, Duration.ofSeconds(2));

            assertEquals(5000, metrics.getAgentTotalProcessingTimeMs(AgentRole.RESEARCHER));
        }

        @Test
        @DisplayName("Tracks agent invocation count")
        void tracksAgentInvocationCount() {
            metrics.recordAgentProcessing(AgentRole.WRITER, Duration.ofSeconds(1));
            metrics.recordAgentProcessing(AgentRole.WRITER, Duration.ofSeconds(2));
            metrics.recordAgentProcessing(AgentRole.WRITER, Duration.ofSeconds(3));

            assertEquals(3, metrics.getAgentInvocationCount(AgentRole.WRITER));
        }

        @Test
        @DisplayName("Calculates agent average processing time")
        void calculatesAgentAverageProcessingTime() {
            metrics.recordAgentProcessing(AgentRole.FACT_CHECKER, Duration.ofSeconds(4));
            metrics.recordAgentProcessing(AgentRole.FACT_CHECKER, Duration.ofSeconds(6));

            assertEquals(5000.0, metrics.getAgentAverageProcessingTimeMs(AgentRole.FACT_CHECKER));
        }

        @Test
        @DisplayName("Returns zero for unknown agent")
        void returnsZeroForUnknownAgent() {
            assertEquals(0, metrics.getAgentTotalProcessingTimeMs(AgentRole.EDITOR));
            assertEquals(0, metrics.getAgentInvocationCount(AgentRole.EDITOR));
            assertEquals(0.0, metrics.getAgentAverageProcessingTimeMs(AgentRole.EDITOR));
        }
    }

    @Nested
    @DisplayName("Report generation")
    class ReportGeneration {

        @Test
        @DisplayName("Generates non-empty report")
        void generatesNonEmptyReport() {
            metrics.recordPipelineStarted();
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));

            String report = metrics.generateReport();

            assertNotNull(report);
            assertTrue(report.contains("Pipeline Statistics"));
            assertTrue(report.contains("Processing Time"));
            assertTrue(report.contains("Approval Statistics"));
        }

        @Test
        @DisplayName("Report includes failure details when failures exist")
        void reportIncludesFailureDetails() {
            metrics.recordPipelineFailed(DocumentState.RESEARCHING);

            String report = metrics.generateReport();

            assertTrue(report.contains("Failures by State"));
            assertTrue(report.contains("RESEARCHING"));
        }
    }

    @Nested
    @DisplayName("Reset")
    class Reset {

        @Test
        @DisplayName("Resets all metrics")
        void resetsAllMetrics() {
            // Record some metrics
            metrics.recordPipelineStarted();
            metrics.recordPipelineCompleted(Duration.ofSeconds(5));
            metrics.recordPipelineFailed(DocumentState.RESEARCHING);
            metrics.recordApprovalRequested();
            metrics.recordAgentProcessing(AgentRole.WRITER, Duration.ofSeconds(3));

            // Reset
            metrics.reset();

            // Verify all reset
            assertEquals(0, metrics.getTotalPipelinesStarted());
            assertEquals(0, metrics.getTotalPipelinesCompleted());
            assertEquals(0, metrics.getTotalPipelinesFailed());
            assertEquals(0, metrics.getTotalProcessingTimeMs());
            assertEquals(0, metrics.getTotalApprovalsRequested());
            assertTrue(metrics.getFailuresByState().isEmpty());
            assertEquals(0, metrics.getAgentInvocationCount(AgentRole.WRITER));
        }
    }

    @Nested
    @DisplayName("Uptime")
    class Uptime {

        @Test
        @DisplayName("Returns positive uptime")
        void returnsPositiveUptime() {
            Duration uptime = metrics.getUptime();
            assertTrue(uptime.toMillis() >= 0);
        }
    }
}
