package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.approval.ApprovalCallback;
import com.jakefear.aipublisher.approval.ApprovalDecision;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.pipeline.PipelineResult;
import com.jakefear.aipublisher.pipeline.PublishingPipeline;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import picocli.CommandLine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiPublisherCommand")
class AiPublisherCommandTest {

    @Mock
    private PublishingPipeline pipeline;

    @Mock
    private ApprovalService approvalService;

    private AiPublisherCommand command;
    private StringWriter outputWriter;
    private PrintWriter out;

    @BeforeEach
    void setUp() {
        command = new AiPublisherCommand(pipeline, approvalService);
        outputWriter = new StringWriter();
        out = new PrintWriter(outputWriter, true);
    }

    @Nested
    @DisplayName("Command Line Parsing")
    class CommandLineParsing {

        @Test
        @DisplayName("Parses short topic option")
        void parsesShortTopicOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Apache Kafka");

            assertEquals("Apache Kafka", command.getTopic());
        }

        @Test
        @DisplayName("Parses long topic option")
        void parsesLongTopicOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("--topic", "Machine Learning");

            assertEquals("Machine Learning", command.getTopic());
        }

        @Test
        @DisplayName("Parses audience option")
        void parsesAudienceOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "-a", "developers");

            assertEquals("developers", command.getAudience());
        }

        @Test
        @DisplayName("Parses word count option")
        void parsesWordCountOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "-w", "1500");

            assertEquals(1500, command.getWordCount());
        }

        @Test
        @DisplayName("Parses auto-approve flag")
        void parsesAutoApproveFlag() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "--auto-approve");

            assertTrue(command.isAutoApprove());
        }

        @Test
        @DisplayName("Uses default values when not specified")
        void usesDefaultValues() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic");

            assertEquals("general readers", command.getAudience());
            assertEquals(800, command.getWordCount());
            assertFalse(command.isAutoApprove());
        }

        @Test
        @DisplayName("Parses short API key option")
        void parsesShortApiKeyOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "-k", "sk-ant-test-key");

            assertEquals("sk-ant-test-key", command.getApiKey());
        }

        @Test
        @DisplayName("Parses long API key option")
        void parsesLongApiKeyOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "--key", "sk-ant-test-key-long");

            assertEquals("sk-ant-test-key-long", command.getApiKey());
        }

        @Test
        @DisplayName("Parses key file option")
        void parsesKeyFileOption() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs("-t", "Topic", "--key-file", "/path/to/keyfile");

            assertEquals(Path.of("/path/to/keyfile"), command.getKeyFile());
        }

        @Test
        @DisplayName("Parses all options together")
        void parsesAllOptionsTogether() {
            CommandLine cmd = new CommandLine(command);
            cmd.parseArgs(
                    "-t", "Docker Containers",
                    "-a", "DevOps engineers",
                    "-w", "1200",
                    "--auto-approve"
            );

            assertEquals("Docker Containers", command.getTopic());
            assertEquals("DevOps engineers", command.getAudience());
            assertEquals(1200, command.getWordCount());
            assertTrue(command.isAutoApprove());
        }
    }

    @Nested
    @DisplayName("Help and Version")
    class HelpAndVersion {

        @Test
        @DisplayName("Shows help with -h")
        void showsHelpWithShortOption() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(command);
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute("-h");

            assertEquals(0, exitCode);
            String output = sw.toString();
            assertTrue(output.contains("aipublisher"));
            assertTrue(output.contains("--topic"));
            assertTrue(output.contains("--audience"));
        }

        @Test
        @DisplayName("Shows version with -V")
        void showsVersionWithShortOption() {
            StringWriter sw = new StringWriter();
            CommandLine cmd = new CommandLine(command);
            cmd.setOut(new PrintWriter(sw));

            int exitCode = cmd.execute("-V");

            assertEquals(0, exitCode);
            assertTrue(sw.toString().contains("AI Publisher"));
        }
    }

    @Nested
    @DisplayName("Pipeline Execution")
    class PipelineExecution {

        @Test
        @DisplayName("Executes pipeline successfully with topic")
        void executesPipelineSuccessfully() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/TestTopic.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute("-t", "Test Topic", "--auto-approve");

            // Assert
            assertEquals(0, exitCode);
            verify(pipeline).execute(any(TopicBrief.class));

            String output = outputWriter.toString();
            assertTrue(output.contains("SUCCESS"));
            assertTrue(output.contains("output/TestTopic.md"));
        }

        @Test
        @DisplayName("Returns exit code 1 on pipeline failure")
        void returnsFailureExitCode() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 800);
            PublishingDocument document = new PublishingDocument(topicBrief);
            PipelineResult failureResult = PipelineResult.failure(
                    document,
                    "Research failed",
                    DocumentState.RESEARCHING,
                    Duration.ofSeconds(5)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(failureResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute("-t", "Test Topic", "--auto-approve");

            // Assert
            assertEquals(1, exitCode);

            String output = outputWriter.toString();
            assertTrue(output.contains("FAILED"));
            assertTrue(output.contains("Research failed"));
        }

        @Test
        @DisplayName("Sets auto-approve callback when flag is set")
        void setsAutoApproveCallback() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test Topic", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/TestTopic.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test Topic", "--auto-approve");

            // Assert
            verify(approvalService).setCallback(any(ApprovalCallback.class));
        }

        @Test
        @DisplayName("Uses custom audience and word count")
        void usesCustomAudienceAndWordCount() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Docker", "DevOps", 1500);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Docker.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(argThat(brief ->
                    brief.topic().equals("Docker") &&
                    brief.targetAudience().equals("DevOps") &&
                    brief.targetWordCount() == 1500
            ))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute(
                    "-t", "Docker",
                    "-a", "DevOps",
                    "-w", "1500",
                    "--auto-approve"
            );

            // Assert
            assertEquals(0, exitCode);
            verify(pipeline).execute(argThat(brief ->
                    brief.topic().equals("Docker") &&
                    brief.targetAudience().equals("DevOps") &&
                    brief.targetWordCount() == 1500
            ));
        }
    }

    @Nested
    @DisplayName("Interactive Mode")
    class InteractiveMode {

        @Test
        @DisplayName("Prompts for topic when not specified")
        void promptsForTopicWhenNotSpecified() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Interactive Topic", "general readers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/InteractiveTopic.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            // Simulate user input
            String userInput = "Interactive Topic\n";
            command.setStreams(
                    new BufferedReader(new StringReader(userInput)),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute("--auto-approve");

            // Assert
            assertEquals(0, exitCode);

            String output = outputWriter.toString();
            assertTrue(output.contains("Enter topic"));
        }

        @Test
        @DisplayName("Exits gracefully on quit command")
        void exitsGracefullyOnQuit() {
            // Simulate user typing 'quit'
            String userInput = "quit\n";
            command.setStreams(
                    new BufferedReader(new StringReader(userInput)),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute();

            // Assert
            assertEquals(0, exitCode);
            verify(pipeline, never()).execute(any());

            String output = outputWriter.toString();
            assertTrue(output.contains("Goodbye"));
        }
    }

    @Nested
    @DisplayName("Output Formatting")
    class OutputFormatting {

        @Test
        @DisplayName("Displays banner in normal mode")
        void displaysBannerInNormalMode() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "--auto-approve");

            // Assert
            String output = outputWriter.toString();
            assertTrue(output.contains("AI PUBLISHER"));
        }

        @Test
        @DisplayName("Suppresses banner in quiet mode")
        void suppressesBannerInQuietMode() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "--auto-approve", "-q");

            // Assert
            String output = outputWriter.toString();
            assertFalse(output.contains("AI PUBLISHER"));
            // Results should still be shown
            assertTrue(output.contains("SUCCESS"));
        }

        @Test
        @DisplayName("Shows quality score and word count on success")
        void showsQualityMetricsOnSuccess() {
            // Arrange
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "--auto-approve");

            // Assert
            String output = outputWriter.toString();
            assertTrue(output.contains("Quality score"));
            assertTrue(output.contains("Word count"));
            assertTrue(output.contains("Total time"));
        }
    }

    @Nested
    @DisplayName("API Key Configuration")
    class ApiKeyConfiguration {

        @Test
        @DisplayName("Sets system property when API key provided via --key")
        void setsSystemPropertyWhenKeyProvided() throws Exception {
            // Arrange
            String testKey = "sk-ant-test-key-" + System.currentTimeMillis();
            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "-k", testKey, "--auto-approve");

            // Assert
            assertEquals(testKey, System.getProperty("ANTHROPIC_API_KEY"));
        }

        @Test
        @DisplayName("Reads API key from key file")
        void readsApiKeyFromFile(@TempDir Path tempDir) throws Exception {
            // Arrange
            String testKey = "sk-ant-file-key-" + System.currentTimeMillis();
            Path keyFile = tempDir.resolve("api-key.txt");
            Files.writeString(keyFile, testKey);

            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "--key-file", keyFile.toString(), "--auto-approve");

            // Assert
            assertEquals(testKey, System.getProperty("ANTHROPIC_API_KEY"));
        }

        @Test
        @DisplayName("Trims whitespace from key file content")
        void trimsWhitespaceFromKeyFile(@TempDir Path tempDir) throws Exception {
            // Arrange
            String testKey = "sk-ant-trimmed-key-" + System.currentTimeMillis();
            Path keyFile = tempDir.resolve("api-key.txt");
            Files.writeString(keyFile, "  " + testKey + "\n\n");

            TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 800);
            PublishingDocument document = createSuccessfulDocument(topicBrief);
            PipelineResult successResult = PipelineResult.success(
                    document,
                    Path.of("output/Test.md"),
                    Duration.ofSeconds(10)
            );

            when(pipeline.execute(any(TopicBrief.class))).thenReturn(successResult);

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            cmd.execute("-t", "Test", "--key-file", keyFile.toString(), "--auto-approve");

            // Assert
            assertEquals(testKey, System.getProperty("ANTHROPIC_API_KEY"));
        }

        @Test
        @DisplayName("Returns error when key file not found")
        void returnsErrorWhenKeyFileNotFound() {
            // Arrange
            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute("-t", "Test", "--key-file", "/nonexistent/path/key.txt", "--auto-approve");

            // Assert
            assertEquals(1, exitCode);
            String output = outputWriter.toString();
            assertTrue(output.contains("ERROR"));
            assertTrue(output.contains("not found"));
        }

        @Test
        @DisplayName("Returns error when key file is empty")
        void returnsErrorWhenKeyFileEmpty(@TempDir Path tempDir) throws Exception {
            // Arrange
            Path keyFile = tempDir.resolve("empty-key.txt");
            Files.writeString(keyFile, "   \n");

            command.setStreams(
                    new BufferedReader(new StringReader("")),
                    out
            );

            // Act
            CommandLine cmd = new CommandLine(command);
            int exitCode = cmd.execute("-t", "Test", "--key-file", keyFile.toString(), "--auto-approve");

            // Assert
            assertEquals(1, exitCode);
            String output = outputWriter.toString();
            assertTrue(output.contains("ERROR"));
            assertTrue(output.contains("empty"));
        }
    }

    /**
     * Create a document that has completed the pipeline successfully.
     */
    private PublishingDocument createSuccessfulDocument(TopicBrief topicBrief) {
        PublishingDocument document = new PublishingDocument(topicBrief);

        // Transition to RESEARCHING and set research brief
        document.transitionTo(DocumentState.RESEARCHING);
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(KeyFact.unsourced("Fact 1"), KeyFact.unsourced("Fact 2"), KeyFact.unsourced("Fact 3")),
                List.of(),
                List.of("Section 1", "Section 2"),
                List.of(),
                java.util.Map.of(),
                List.of()
        );
        document.setResearchBrief(researchBrief);

        // Transition to DRAFTING and set draft
        document.transitionTo(DocumentState.DRAFTING);
        ArticleDraft draft = new ArticleDraft(
                "# Test Article\n\nThis is test content.",
                "Test summary",
                List.of(),
                List.of(),
                java.util.Map.of()
        );
        document.setDraft(draft);

        // Transition to FACT_CHECKING and set fact check report
        document.transitionTo(DocumentState.FACT_CHECKING);
        FactCheckReport factCheckReport = new FactCheckReport(
                draft.markdownContent(),
                List.of(new VerifiedClaim("Claim 1", "VERIFIED", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        );
        document.setFactCheckReport(factCheckReport);

        // Transition to EDITING and set final article
        document.transitionTo(DocumentState.EDITING);
        FinalArticle finalArticle = new FinalArticle(
                "# Test Article\n\nThis is the final polished content with approximately 500 words of content.",
                DocumentMetadata.create("Test Article", "Test summary"),
                "Article edited for publication",
                0.85,
                List.of()
        );
        document.setFinalArticle(finalArticle);

        // Transition to PUBLISHED
        document.transitionTo(DocumentState.PUBLISHED);

        return document;
    }
}
