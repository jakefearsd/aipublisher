package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.agent.*;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.config.QualityProperties;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.glossary.GlossaryService;
import com.jakefear.aipublisher.monitoring.PipelineMonitoringService;
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
    private CriticAgent criticAgent;
    @Mock
    private WikiOutputService outputService;
    @Mock
    private ApprovalService approvalService;
    @Mock
    private PipelineMonitoringService monitoringService;

    private GlossaryService glossaryService;
    private PipelineProperties pipelineProperties;
    private QualityProperties qualityProperties;
    private PublishingPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipelineProperties = new PipelineProperties();
        pipelineProperties.setMaxRevisionCycles(3);

        qualityProperties = new QualityProperties();
        qualityProperties.setMinEditorScore(0.7);

        glossaryService = new GlossaryService();

        // By default, auto-approve everything (lenient because not all tests use approval)
        lenient().when(approvalService.checkAndApprove(any())).thenReturn(true);

        pipeline = new PublishingPipeline(
                researchAgent, writerAgent, factCheckerAgent, editorAgent, criticAgent,
                outputService, approvalService, monitoringService, glossaryService, pipelineProperties, qualityProperties
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
            verify(criticAgent).process(any());
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
            setupCriticSuccess();
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
        @DisplayName("Continues after max revision cycles without embedding markers")
        void continuesAfterMaxRevisionCyclesWithoutEmbeddingMarkers() throws IOException {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(2);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            // Always returns REVISE
            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReportWithQuestionableClaims(doc);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert - pipeline continues instead of failing
            assertTrue(result.success(), "Pipeline should succeed after max revisions: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            // Draft should NOT contain failure markers (they are logged instead)
            String draftContent = result.document().getDraft().wikiContent();
            assertFalse(draftContent.contains("FACT CHECK FAIL BEGIN"),
                    "Content should not contain embedded failure markers");
        }

        @Test
        @DisplayName("Content remains clean after max revision cycles")
        void contentRemainsCleanAfterMaxRevisionCycles() throws IOException {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(1);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReportWithQuestionableClaims(doc);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert - content should be clean without embedded markers
            assertTrue(result.success(), "Pipeline should succeed: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            String draftContent = result.document().getDraft().wikiContent();
            assertFalse(draftContent.contains("Questionable Claim"),
                    "Content should not contain embedded failure details");
        }

        @Test
        @DisplayName("Logs issues to session log after max revision cycles")
        void logsIssuesToSessionLogAfterMaxRevisionCycles() throws IOException {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(1);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();

            when(factCheckerAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setFactCheckReportWithConsistencyIssues(doc);
                return doc;
            });
            when(factCheckerAgent.validate(any())).thenReturn(true);
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert - pipeline succeeds, issues are logged (not embedded)
            assertTrue(result.success(), "Pipeline should succeed: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            String draftContent = result.document().getDraft().wikiContent();
            assertFalse(draftContent.contains("Consistency Issues"),
                    "Content should not contain embedded consistency issues");
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
            // No critic setup needed - pipeline fails in editing phase before critic

            when(editorAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                doc.setFinalArticle(new FinalArticle(
                        "!!! Content",
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
        setupCriticSuccess();
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
                    "!!! Test Article\n\nContent here.",
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
                    "!!! Final Article\n\nPolished content.",
                    DocumentMetadata.create("Final Title", "Final summary"),
                    "Edited for publication",
                    0.85,
                    List.of()
            ));
            return doc;
        });
        when(editorAgent.validate(any())).thenReturn(true);
    }

    private void setupCriticSuccess() {
        when(criticAgent.process(any())).thenAnswer(invocation -> {
            PublishingDocument doc = invocation.getArgument(0);
            doc.setCriticReport(new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            ));
            return doc;
        });
        when(criticAgent.validate(any())).thenReturn(true);
    }

    private void setFactCheckReport(PublishingDocument doc, RecommendedAction action) {
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().wikiContent(),
                List.of(VerifiedClaim.verified("Test claim", 0)),
                action == RecommendedAction.APPROVE ? List.of() :
                        List.of(QuestionableClaim.withoutSuggestion("Issue claim", "Some issue")),
                List.of(),
                action == RecommendedAction.REJECT ? ConfidenceLevel.LOW : ConfidenceLevel.MEDIUM,
                action
        ));
    }

    private void setFactCheckReportWithQuestionableClaims(PublishingDocument doc) {
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().wikiContent(),
                List.of(VerifiedClaim.verified("Investing builds wealth", 0)),
                List.of(
                        QuestionableClaim.withSuggestion(
                                "The stock market always goes up",
                                "Historical data shows periods of decline",
                                "Consider rephrasing to 'historically trends upward over long periods'"
                        ),
                        QuestionableClaim.withoutSuggestion(
                                "Returns are guaranteed",
                                "No investment returns are guaranteed"
                        )
                ),
                List.of(),
                ConfidenceLevel.MEDIUM,
                RecommendedAction.REVISE
        ));
    }

    private void setFactCheckReportWithConsistencyIssues(PublishingDocument doc) {
        doc.setFactCheckReport(new FactCheckReport(
                doc.getDraft().wikiContent(),
                List.of(VerifiedClaim.verified("Test claim", 0)),
                List.of(QuestionableClaim.withoutSuggestion("Some claim", "Issue with claim")),
                List.of(
                        "Date conflict between sections: 2020 vs 2021",
                        "Terminology inconsistency: 'compound interest' vs 'compounding'"
                ),
                ConfidenceLevel.MEDIUM,
                RecommendedAction.REVISE
        ));
    }

    @Nested
    @DisplayName("Fact Check Failure Logging")
    class FactCheckFailureLogging {

        @Test
        @DisplayName("Does not throw when report is null")
        void doesNotThrowWhenReportIsNull() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logFactCheckFailures("TestPage", null));
        }

        @Test
        @DisplayName("Does not throw when no questionable claims")
        void doesNotThrowWhenNoQuestionableClaims() {
            // Arrange
            FactCheckReport report = new FactCheckReport(
                    "content",
                    List.of(VerifiedClaim.verified("All good", 0)),
                    List.of(), // No questionable claims
                    List.of(),
                    ConfidenceLevel.HIGH,
                    RecommendedAction.APPROVE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logFactCheckFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs questionable claims without throwing")
        void logsQuestionableClaimsWithoutThrowing() {
            // Arrange
            FactCheckReport report = new FactCheckReport(
                    "content",
                    List.of(),
                    List.of(QuestionableClaim.withSuggestion(
                            "Bad claim",
                            "This is wrong",
                            "Try this instead"
                    )),
                    List.of(),
                    ConfidenceLevel.LOW,
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logFactCheckFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs consistency issues without throwing")
        void logsConsistencyIssuesWithoutThrowing() {
            // Arrange
            FactCheckReport report = new FactCheckReport(
                    "content",
                    List.of(),
                    List.of(QuestionableClaim.withoutSuggestion("Claim", "Issue")),
                    List.of("Inconsistent dates", "Conflicting terminology"),
                    ConfidenceLevel.LOW,
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logFactCheckFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs multiple questionable claims without throwing")
        void logsMultipleQuestionableClaimsWithoutThrowing() {
            // Arrange
            FactCheckReport report = new FactCheckReport(
                    "content",
                    List.of(),
                    List.of(
                            QuestionableClaim.withoutSuggestion("First claim", "Issue 1"),
                            QuestionableClaim.withoutSuggestion("Second claim", "Issue 2"),
                            QuestionableClaim.withoutSuggestion("Third claim", "Issue 3")
                    ),
                    List.of(),
                    ConfidenceLevel.LOW,
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logFactCheckFailures("TestPage", report));
        }
    }

    @Nested
    @DisplayName("Critique Phase")
    class CritiquePhase {

        @Test
        @DisplayName("Proceeds when critic approves")
        void proceedsWhenCriticApproves() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupSuccessfulMocks();
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            verify(criticAgent, times(1)).process(any());
        }

        @Test
        @DisplayName("Triggers revision when critic recommends revise")
        void triggersRevisionWhenCriticRecommendsRevise() throws IOException {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();

            // First critique: REVISE, second: APPROVE
            when(criticAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setCriticReport(doc, RecommendedAction.REVISE);
                return doc;
            }).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setCriticReport(doc, RecommendedAction.APPROVE);
                return doc;
            });
            when(criticAgent.validate(any())).thenReturn(true);
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            verify(criticAgent, times(2)).process(any());
            verify(editorAgent, times(2)).process(any()); // Original + revision
        }

        @Test
        @DisplayName("Fails when critic rejects")
        void failsWhenCriticRejects() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();

            when(criticAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setCriticReport(doc, RecommendedAction.REJECT);
                return doc;
            });
            when(criticAgent.validate(any())).thenReturn(true);
            when(outputService.getExistingPagesList()).thenReturn(List.of());

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertFalse(result.success());
            assertEquals(DocumentState.CRITIQUING, result.failedAtState());
            assertTrue(result.errorMessage().contains("rejected"));
        }

        @Test
        @DisplayName("Continues after max revision cycles without embedding markers")
        void continuesAfterMaxRevisionCyclesWithoutEmbeddingMarkers() throws IOException {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(2);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();

            // Always returns REVISE with issues
            when(criticAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setCriticReportWithIssues(doc);
                return doc;
            });
            when(criticAgent.validate(any())).thenReturn(true);
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert - pipeline continues instead of failing
            assertTrue(result.success(), "Pipeline should succeed after max revisions: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            // Final article should NOT contain critique markers (they are logged instead)
            String finalContent = result.document().getFinalArticle().wikiContent();
            assertFalse(finalContent.contains("CRITIQUE REVIEW NOTES BEGIN"),
                    "Content should not contain embedded failure markers");
        }

        @Test
        @DisplayName("Content remains clean after max revision cycles")
        void contentRemainsCleanAfterMaxRevisionCycles() throws IOException {
            // Arrange
            pipelineProperties.setMaxRevisionCycles(1);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();

            when(criticAgent.process(any())).thenAnswer(invocation -> {
                PublishingDocument doc = invocation.getArgument(0);
                setCriticReportWithIssues(doc);
                return doc;
            });
            when(criticAgent.validate(any())).thenReturn(true);
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert - content should be clean without embedded markers
            assertTrue(result.success(), "Pipeline should succeed: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            String finalContent = result.document().getFinalArticle().wikiContent();
            assertFalse(finalContent.contains("Syntax Issues"),
                    "Content should not contain embedded syntax issues");
        }

        private void setCriticReport(PublishingDocument doc, RecommendedAction action) {
            doc.setCriticReport(new CriticReport(
                    action == RecommendedAction.APPROVE ? 0.9 : 0.7,
                    0.85, 0.85, 0.85,
                    List.of(), List.of(), List.of(), List.of(),
                    action
            ));
        }

        private void setCriticReportWithIssues(PublishingDocument doc) {
            doc.setCriticReport(new CriticReport(
                    0.65, 0.7, 0.6, 0.7,
                    List.of("Missing introduction section"),
                    List.of("Markdown heading found: # should be !!!"),
                    List.of("Paragraph too long"),
                    List.of("Add more examples"),
                    RecommendedAction.REVISE
            ));
        }
    }

    @Nested
    @DisplayName("Critique Failure Logging")
    class CritiqueFailureLogging {

        @Test
        @DisplayName("Does not throw when report is null")
        void doesNotThrowWhenReportIsNull() {
            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", null));
        }

        @Test
        @DisplayName("Does not throw when no issues")
        void doesNotThrowWhenNoIssues() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), // No structure issues
                    List.of(), // No syntax issues
                    List.of(), // No style issues
                    List.of(), // No suggestions
                    RecommendedAction.APPROVE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs syntax issues without throwing")
        void logsSyntaxIssuesWithoutThrowing() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.7, 0.8, 0.6, 0.8,
                    List.of(),
                    List.of("Heading issue", "Code formatting issue"),
                    List.of(),
                    List.of(),
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs structure issues without throwing")
        void logsStructureIssuesWithoutThrowing() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.7, 0.6, 0.8, 0.8,
                    List.of("Missing introduction", "No conclusion section"),
                    List.of(),
                    List.of(),
                    List.of(),
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs style issues without throwing")
        void logsStyleIssuesWithoutThrowing() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.7, 0.8, 0.8, 0.6,
                    List.of(),
                    List.of(),
                    List.of("Passive voice overused", "Paragraphs too long"),
                    List.of(),
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs suggestions without throwing")
        void logsSuggestionsWithoutThrowing() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.7, 0.8, 0.8, 0.8,
                    List.of("Minor structure issue"),
                    List.of(),
                    List.of(),
                    List.of("Add more examples", "Consider adding a table of contents"),
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }

        @Test
        @DisplayName("Logs all issue types without throwing")
        void logsAllIssueTypesWithoutThrowing() {
            // Arrange
            CriticReport report = new CriticReport(
                    0.6, 0.6, 0.5, 0.6,
                    List.of("Structure problem"),
                    List.of("Syntax problem"),
                    List.of("Style problem"),
                    List.of("Suggestion"),
                    RecommendedAction.REVISE
            );

            // Act & Assert - should not throw
            assertDoesNotThrow(() -> pipeline.logCritiqueFailures("TestPage", report));
        }
    }

    @Nested
    @DisplayName("Skip Phase Flags")
    class SkipPhaseFlags {

        @Test
        @DisplayName("Skips fact-check when skip-fact-check=true")
        void skipsFactCheckWhenFlagIsTrue() throws IOException {
            // Arrange
            pipelineProperties.setSkipFactCheck(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            // Note: NOT setting up fact check - it should be skipped
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success(), "Pipeline should succeed with skip-fact-check: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            verify(factCheckerAgent, never()).process(any());
            verify(editorAgent).process(any()); // Editor should still be called
        }

        @Test
        @DisplayName("Skips critique when skip-critique=true")
        void skipsCritiqueWhenFlagIsTrue() throws IOException {
            // Arrange
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();
            // Note: NOT setting up critic - it should be skipped
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success(), "Pipeline should succeed with skip-critique: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            verify(criticAgent, never()).process(any());
            verify(editorAgent).process(any()); // Editor should still be called
        }

        @Test
        @DisplayName("Skips both phases when both flags are true")
        void skipsBothPhasesWhenBothFlagsAreTrue() throws IOException {
            // Arrange - combined skip scenario
            pipelineProperties.setSkipFactCheck(true);
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupEditorSuccess();
            // Note: NOT setting up fact check or critic - both should be skipped
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success(), "Pipeline should succeed with both phases skipped: " +
                    (result.errorMessage() != null ? result.errorMessage() : "no error"));
            verify(factCheckerAgent, never()).process(any());
            verify(criticAgent, never()).process(any());
            verify(researchAgent).process(any()); // Research should still be called
            verify(writerAgent).process(any()); // Writer should still be called
            verify(editorAgent).process(any()); // Editor should still be called
        }

        @Test
        @DisplayName("Document has null FactCheckReport when fact-check is skipped")
        void documentHasNullFactCheckReportWhenSkipped() throws IOException {
            // Arrange
            pipelineProperties.setSkipFactCheck(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            assertNull(result.document().getFactCheckReport(),
                    "FactCheckReport should be null when fact-check is skipped");
        }

        @Test
        @DisplayName("Document has null CriticReport when critique is skipped")
        void documentHasNullCriticReportWhenSkipped() throws IOException {
            // Arrange
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            assertNull(result.document().getCriticReport(),
                    "CriticReport should be null when critique is skipped");
        }

        @Test
        @DisplayName("Document has both null reports when both phases skipped")
        void documentHasBothNullReportsWhenBothSkipped() throws IOException {
            // Arrange
            pipelineProperties.setSkipFactCheck(true);
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupEditorSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            assertNull(result.document().getFactCheckReport(),
                    "FactCheckReport should be null when fact-check is skipped");
            assertNull(result.document().getCriticReport(),
                    "CriticReport should be null when critique is skipped");
        }

        @Test
        @DisplayName("Approval service called correct number of times with skip-fact-check")
        void approvalServiceCalledCorrectlyWithSkipFactCheck() throws IOException {
            // Arrange
            pipelineProperties.setSkipFactCheck(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupEditorSuccess();
            setupCriticSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            // Should be called 4 times: after research, draft, editing, and critique
            // (NOT after fact-check since it's skipped)
            verify(approvalService, times(4)).checkAndApprove(any());
        }

        @Test
        @DisplayName("Approval service called correct number of times with skip-critique")
        void approvalServiceCalledCorrectlyWithSkipCritique() throws IOException {
            // Arrange
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupFactCheckSuccess();
            setupEditorSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            // Should be called 4 times: after research, draft, fact-check, and editing
            // (NOT after critique since it's skipped)
            verify(approvalService, times(4)).checkAndApprove(any());
        }

        @Test
        @DisplayName("Approval service called correct number of times with both phases skipped")
        void approvalServiceCalledCorrectlyWithBothSkipped() throws IOException {
            // Arrange
            pipelineProperties.setSkipFactCheck(true);
            pipelineProperties.setSkipCritique(true);
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
            setupResearchSuccess();
            setupWriterSuccess();
            setupEditorSuccess();
            when(outputService.getExistingPagesList()).thenReturn(List.of());
            when(outputService.writeDocument(any())).thenReturn(tempDir.resolve("Test.md"));

            // Act
            PipelineResult result = pipeline.execute(topicBrief);

            // Assert
            assertTrue(result.success());
            // Should be called 3 times: after research, draft, and editing
            // (NOT after fact-check or critique since both are skipped)
            verify(approvalService, times(3)).checkAndApprove(any());
        }
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
            // Should be called 5 times: after research, draft, fact-check, editing, and critique
            verify(approvalService, times(5)).checkAndApprove(any());
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
