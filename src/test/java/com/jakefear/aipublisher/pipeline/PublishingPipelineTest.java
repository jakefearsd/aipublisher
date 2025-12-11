package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.agent.*;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.config.QualityProperties;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.output.WikiOutputService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublishingPipeline")
class PublishingPipelineTest {

    @TempDir
    Path tempDir;

    @Mock
    private ResearchAgent researchAgent;
    @Mock
    private WriterAgent writerAgent;
    @Mock
    private FactCheckerAgent factCheckerAgent;
    @Mock
    private EditorAgent editorAgent;
    @Mock
    private WikiOutputService outputService;
    @Mock
    private ApprovalService approvalService;

    private PipelineProperties pipelineProperties;
    private QualityProperties qualityProperties;
    private PublishingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties();
        pipelineProperties.setMaxRevisionCycles(3);

        qualityProperties = new QualityProperties();
        qualityProperties.setMinEditorScore(0.7);

        // By default, auto-approve everything (lenient because not all tests use approval)
        lenient().when(approvalService.checkAndApprove(any())).thenReturn(true);

        pipeline = new PublishingPipeline(
                researchAgent, writerAgent, factCheckerAgent, editorAgent,
                outputService, approvalService, pipelineProperties, qualityProperties
        );
    }

    @Nested
    @DisplayName("Successful Pipeline")
    class SuccessfulPipeline {

        @Test
        @DisplayName("Executes all phases successfully")
        void executesAllPhasesSuccessfully() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 500);
            setupSuccessfulMocks();

            Path outputPath = tempDir.resolve("TestTopic.md");
            when(outputService.writeDocument(any())).thenReturn(outputPath);

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            assertNotNull(result.document());
            assertEquals(outputPath, result.outputPath());
            assertNull(result.errorMessage());
            assertNull(result.failedAtState());

            // Verify all agents were called
            verify(researchAgent).process(any());
            verify(writerAgent).process(any());
            verify(factCheckerAgent).process(any());
            verify(editorAgent).process(any());
            verify(outputService).writeDocument(any());
        }

        @Test
        @DisplayName("Reports total processing time")
        void reportsTotalProcessingTime() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertNotNull(result.totalTime());
            assertTrue(result.totalTime().toMillis() >= 0);
        }

        @Test
        @DisplayName("Provides existing pages to editor")
        void providesExistingPagesToEditor() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));
            when(outputService.getExistingPagesList()).thenReturn(List.of("Page1", "Page2"));

            // Act
            pipeline.execute(topicBrief);

            // Assert
            verify(editorAgent).setExistingPages(List.of("Page1", "Page2"));
        }
    }

    @Nested
    @DisplayName("Research Phase")
    class ResearchPhase {

        @Test
        @DisplayName("Fails when research agent fails")
        void failsWhenResearchAgentFails() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            when(researchAgent.process(any())).thenThrow(
                    new AgentException(AgentRole.RESEARCHER, "Research failed"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertEquals(DocumentState.RESEARCHING, result.failedAtState());
            assertTrue(result.errorMessage().contains("Research"));
        }

        @Test
        @DisplayName("Fails when research validation fails")
        void failsWhenResearchValidationFails() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            when(researchAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                // Don't set research brief, validation will fail
                return doc;
            });
            when(researchAgent.validate(any())).thenReturn(false);

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertEquals(DocumentState.RESEARCHING, result.failedAtState());
        }
    }

    @Nested
    @DisplayName("Drafting Phase")
    class DraftingPhase {

        @Test
        @DisplayName("Fails when writer agent fails")
        void failsWhenWriterAgentFails() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            when(writerAgent.process(any())).thenThrow(
                    new AgentException(AgentRole.WRITER, "Writing failed"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertEquals(DocumentState.DRAFTING, result.failedAtState());
        }
    }

    @Nested
    @DisplayName("Fact Check Phase")
    class FactCheckPhase {

        @Test
        @DisplayName("Proceeds when fact check approves")
        void proceedsWhenFactCheckApproves() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            verify(factCheckerAgent, times(1)).process(any());
        }

        @Test
        @DisplayName("Triggers revision when fact check recommends revise")
        void triggersRevisionWhenFactCheckRecommendsRevise() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            // First fact check: REVISE, second: APPROVE
            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReport(doc, RecommendedAction.REVISE);
                return doc;
            }).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReport(doc, RecommendedAction.APPROVE);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);
            setupEditorSuccess();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            verify(factCheckerAgent, times(2)).process(any());
            verify(writerAgent, times(2)).process(any()); // Original + revision
        }

        @Test
        @DisplayName("Fails when fact check rejects")
        void failsWhenFactCheckRejects() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReport(doc, RecommendedAction.REJECT);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertEquals(DocumentState.FACT_CHECKING, result.failedAtState());
            assertTrue(result.errorMessage().contains("rejected"));
        }

        @Test
        @DisplayName("Fails after max revision cycles")
        void failsAfterMaxRevisionCycles() {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(2);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            // Always returns REVISE
            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReport(doc, RecommendedAction.REVISE);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertTrue(result.errorMessage().contains("Maximum revision cycles"));
        }
    }

    @Nested
    @DisplayName("Editing Phase")
    class EditingPhase {

        @Test
        @DisplayName("Fails when quality score below threshold")
        void failsWhenQualityScoreBelowThreshold() {
            // Arrange
            qualityProperties.setMinEditorScore(0.9);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();

            when(editorAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                doc.setFinalArticle(new FinalArticle(
                        "## Content",
                        DocumentMetadata.create("Title", "Summary"),
                        "Edit summary",
                        0.75, // Below 0.9 threshold
                        List.of()
                ));
                return doc;
            });
            when(editorAgent.validate(any())).thenReturn(true);

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertTrue(result.errorMessage().contains("Quality score"));
        }
    }

    @Nested
    @DisplayName("Publishing Phase")
    class PublishingPhase {

        @Test
        @DisplayName("Fails when output write fails")
        void failsWhenOutputWriteFails() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenThrow(new IOException("Disk full"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertTrue(result.errorMessage().contains("Disk full"));
        }
    }

    // Helper methods for setting up mocks

    private void setupSuccessfulMocks() {
        setupResearchSuccess();
        setupWriterSuccess();
        setupFactCheckSuccess();
        setupEditorSuccess();
        when(outputService.getExistingPagesList()).thenReturn(List.of());
    }

    private void setupResearchSuccess() {
        when(researchAgent.process(any())).thenAnswer(invocation -> {
            PublishingDocument doc = invocation.getArgument(0);
            doc.setResearchBrief(new ResearchBrief(
                    List.of(KeyFact.unsourced("Fact 1"), KeyFact.unsourced("Fact 2"), KeyFact.unsourced("Fact 3")),
                    List.of(),
                    List.of("Intro", "Main"),
                    List.of(),
                    Map.of(),
                    List.of()
            ));
            return doc;
        });
        when(researchAgent.validate(any())).thenReturn(true);
    }

    private void setupWriterSuccess() {
        when(writerAgent.process(any())).thenAnswer(invocation -> {
            PublishingDocument doc = invocation.getArgument(0);
            doc.setDraft(new ArticleDraft(
                    "## Test Article\n\nContent here.",
                    "Test summary",
                    List.of(),
                    List.of(),
                    Map.of()
            ));
            return doc;
        });
        when(writerAgent.validate(any())).thenReturn(true);
    }

    private void setupFactCheckSuccess() {
        when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
            PublishingDocument doc = invocation.getArgument(0);
            setFactCheckReport(doc, RecommendedAction.APPROVE);
            return doc;
        });
        when(factCheckerAgent.validate(any())).thenReturn(true);
    }

    private void setupEditorSuccess() {
        when(editorAgent.process(any())).thenAnswer(invocation -> {
            PublishingDocument doc = invocation.getArgument(0);
            doc.setFinalArticle(new FinalArticle(
                    "## Final Article\n\nPolished content.",
                    DocumentMetadata.create("Final Title", "Final summary"),
                    "Edited for publication",
                    0.85,
                    List.of()
            ));
            return doc;
        });
        when(editorAgent.validate(any())).thenReturn(true);
    }

    private void setFactCheckReport(PublishingDocument doc, RecommendedAction action) {
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().markdownContent(),
                List.of(VerifiedClaim.verified("Test claim", 0)),
                action == RecommendedAction.APPROVE ? List.of() :
                        List.of(QuestionableClaim.withoutSuggestion("Issue claim", "Some issue")),
                List.of(),
                action == RecommendedAction.REJECT ? ConfidenceLevel.LOW : ConfidenceLevel.MEDIUM,
                action
        ));
    }

    @Nested
    @DisplayName("Approval Workflow")
    class ApprovalWorkflow {

        @Test
        @DisplayName("Calls approval service after each phase")
        void callsApprovalServiceAfterEachPhase() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            // Should be called 4 times: after research, draft, fact-check, and editing
            verify(approvalService, times(4)).checkAndApprove(any());
        }

        @Test
        @DisplayName("Fails when approval is rejected")
        void failsWhenApprovalIsRejected() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();

            // Reject after research phase
            when(approvalService.checkAndApprove(any()))
                    .thenThrow(new ApprovalService.ApprovalRejectedException(
                            "Research needs more work",
                            DocumentState.RESEARCHING
                    ));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertTrue(result.errorMessage().contains("rejected"));
        }

        @Test
        @DisplayName("Fails when changes are requested")
        void failsWhenChangesAreRequested() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            // Auto-approve research, then request changes after draft
            when(approvalService.checkAndApprove(any()))
                    .thenReturn(true)  // After research
                    .thenReturn(false); // After draft - changes requested

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertTrue(result.errorMessage().contains("Changes requested"));
        }
    }
}
