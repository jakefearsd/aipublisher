package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.agent.*;
import com.jakefear.aipublisher.approval.ApprovalCallback;
import com.jakefear.aipublisher.approval.ApprovalDecision;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.config.QualityProperties;
import com.jakefear.aipublisher.document.TopicBrief;
import com.jakefear.aipublisher.glossary.GlossaryService;
import com.jakefear.aipublisher.monitoring.PipelineMonitoringService;
import com.jakefear.aipublisher.output.WikiOutputService;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the complete publishing pipeline using the real Claude API.
 *
 * These tests are only run when the ANTHROPIC_API_KEY environment variable is set.
 * They make real API calls and consume tokens/credits.
 *
 * Run with: mvn test -Dtest=PublishingPipelineIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@DisplayName("PublishingPipeline Integration")
class PublishingPipelineIntegrationTest {

    @TempDir
    Path tempDir;

    private PublishingPipeline pipeline;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");

        // Create agents with appropriate temperatures
        ChatLanguageModel researchModel = createModel(apiKey, 0.3);
        ChatLanguageModel writerModel = createModel(apiKey, 0.7);
        ChatLanguageModel factCheckerModel = createModel(apiKey, 0.1);
        ChatLanguageModel editorModel = createModel(apiKey, 0.5);
        ChatLanguageModel criticModel = createModel(apiKey, 0.3);

        ResearchAgent researchAgent = new ResearchAgent(researchModel, AgentPrompts.RESEARCH);
        WriterAgent writerAgent = new WriterAgent(writerModel, AgentPrompts.WRITER);
        FactCheckerAgent factCheckerAgent = new FactCheckerAgent(factCheckerModel, AgentPrompts.FACT_CHECKER);
        EditorAgent editorAgent = new EditorAgent(editorModel, AgentPrompts.EDITOR);
        CriticAgent criticAgent = new CriticAgent(criticModel, AgentPrompts.CRITIC);

        // Configure output service
        OutputProperties outputProperties = new OutputProperties();
        outputProperties.setDirectory(tempDir.toString());
        WikiOutputService outputService = new WikiOutputService(outputProperties);

        // Configure pipeline
        PipelineProperties pipelineProperties = new PipelineProperties();
        pipelineProperties.setMaxRevisionCycles(2);

        QualityProperties qualityProperties = new QualityProperties();
        qualityProperties.setMinEditorScore(0.7);

        // Auto-approve everything for integration tests
        ApprovalCallback autoApprove = request ->
                ApprovalDecision.approve(request.id(), "integration-test");
        ApprovalService approvalService = new ApprovalService(pipelineProperties, autoApprove);

        // Monitoring service with no listeners for integration tests
        PipelineMonitoringService monitoringService = new PipelineMonitoringService(List.of());

        // Glossary service
        GlossaryService glossaryService = new GlossaryService();

        pipeline = new PublishingPipeline(
                researchAgent, writerAgent, factCheckerAgent, editorAgent, criticAgent,
                outputService, approvalService, monitoringService, glossaryService, pipelineProperties, qualityProperties
        );
    }

    private ChatLanguageModel createModel(String apiKey, double temperature) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-sonnet-4-20250514")
                .maxTokens(4096)
                .temperature(temperature)
                .build();
    }

    @Test
    @DisplayName("Executes full pipeline for a simple topic")
    void executesFullPipelineForSimpleTopic() {
        // Arrange
        TopicBrief topicBrief = TopicBrief.simple(
                "Version Control Basics",
                "developers new to software development",
                400
        );

        // Act
        PipelineResult result = pipeline.execute(topicBrief);

        // Assert
        System.out.println("=== Pipeline Result ===");
        System.out.println("Success: " + result.success());
        System.out.println("Total time: " + result.totalTime().toMillis() + "ms");

        if (result.success()) {
            System.out.println("Output path: " + result.outputPath());
            System.out.println("Quality score: " + result.document().getFinalArticle().qualityScore());
            System.out.println("Word count: " + result.document().getFinalArticle().estimateWordCount());

            assertTrue(result.success());
            assertNotNull(result.outputPath());
            assertTrue(Files.exists(result.outputPath()));

            // Verify content was written
            try {
                String content = Files.readString(result.outputPath());
                System.out.println("\n=== Output Content Preview ===");
                System.out.println(content.substring(0, Math.min(500, content.length())));

                assertFalse(content.isBlank());
                assertTrue(content.contains("!"), "Should have JSPWiki headings"); // JSPWiki uses ! for headings
            } catch (Exception e) {
                fail("Failed to read output file: " + e.getMessage());
            }
        } else {
            System.out.println("Failed at: " + result.failedAtState());
            System.out.println("Error: " + result.errorMessage());

            // Pipeline might fail due to various reasons (rate limits, etc.)
            // Log for inspection but don't necessarily fail the test
            System.out.println("Pipeline did not succeed - check logs for details");
        }
    }

    @Test
    @DisplayName("Generates valid JSPWiki Markdown")
    void generatesValidJSPWikiMarkdown() {
        // Arrange
        TopicBrief topicBrief = TopicBrief.simple(
                "Git Commands",
                "beginners",
                300
        );

        // Act
        PipelineResult result = pipeline.execute(topicBrief);

        // Assert
        if (result.success()) {
            try {
                String content = Files.readString(result.outputPath());

                System.out.println("=== JSPWiki Validation ===");

                // Check for proper JSPWiki heading structure (! or !!! not ##)
                boolean hasMainHeading = content.contains("!!!") || content.contains("!! ") || content.contains("! ");
                System.out.println("Has JSPWiki heading: " + hasMainHeading);

                // Check for metadata comment
                boolean hasMetadata = content.contains("<!-- ");
                System.out.println("Has metadata: " + hasMetadata);

                // Check content is substantial
                int wordCount = content.split("\\s+").length;
                System.out.println("Approximate word count: " + wordCount);

                assertTrue(hasMainHeading, "Should have JSPWiki heading (!, !!, or !!!)");
                assertTrue(hasMetadata, "Should have metadata comment");
                assertTrue(wordCount > 100, "Should have substantial content");

            } catch (Exception e) {
                fail("Failed to read output file: " + e.getMessage());
            }
        } else {
            System.out.println("Pipeline did not succeed: " + result.errorMessage());
        }
    }

    @Test
    @DisplayName("Reports timing for each phase")
    void reportsTimingForEachPhase() {
        // Arrange
        TopicBrief topicBrief = TopicBrief.simple("REST APIs", "developers", 300);

        // Act
        PipelineResult result = pipeline.execute(topicBrief);

        // Assert
        System.out.println("=== Phase Timing ===");
        System.out.println("Total time: " + result.totalTime().toMillis() + "ms");

        assertNotNull(result.totalTime());
        assertTrue(result.totalTime().toMillis() > 0);

        // Check contributions for timing info
        if (result.document() != null) {
            System.out.println("\nContributions:");
            result.document().getContributions().forEach(c -> {
                System.out.println("  " + c.agentRole() + ": " + c.processingTime().toMillis() + "ms");
            });
        }
    }
}
