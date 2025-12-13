package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.approval.ApprovalCallback;
import com.jakefear.aipublisher.approval.ApprovalDecision;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.document.TopicBrief;
import com.jakefear.aipublisher.pipeline.PipelineResult;
import com.jakefear.aipublisher.pipeline.PublishingPipeline;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.function.Supplier;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Main CLI command for AI Publisher.
 *
 * Provides a professional command-line interface using Picocli with support for:
 * - Named and short options
 * - Auto-generated help
 * - Interactive mode when topic not specified
 * - Auto-approve mode for scripting
 */
@Component
@Command(
        name = "aipublisher",
        mixinStandardHelpOptions = true,
        version = "AI Publisher 0.1.0",
        description = "Generate well-researched, fact-checked articles using AI agents.",
        footer = {
                "",
                "Examples:",
                "  aipublisher -t \"Apache Kafka\"",
                "  aipublisher --topic \"Machine Learning\" --audience \"beginners\" --words 1500",
                "  aipublisher -t \"Docker\" -a \"DevOps engineers\" -w 1000 --auto-approve",
                "  aipublisher -t \"Kubernetes\" -k sk-ant-api03-xxxxx",
                "  aipublisher -t \"Kubernetes\" --key-file ~/.anthropic-key",
                "",
                "API Key (one of these is required):",
                "  -k, --key         Pass API key directly on command line",
                "  --key-file        Read API key from a file",
                "  ANTHROPIC_API_KEY Environment variable"
        }
)
public class AiPublisherCommand implements Callable<Integer> {

    private Supplier<PublishingPipeline> pipelineSupplier;
    private Supplier<ApprovalService> approvalServiceSupplier;

    @Option(names = {"-t", "--topic"},
            description = "Topic to write about (prompts interactively if not specified)")
    private String topic;

    @Option(names = {"-a", "--audience"},
            description = "Target audience for the article",
            defaultValue = "general readers")
    private String audience;

    @Option(names = {"-w", "--words"},
            description = "Target word count",
            defaultValue = "800")
    private int wordCount;

    @Option(names = {"-o", "--output"},
            description = "Output directory for generated articles",
            defaultValue = "./output")
    private String outputDirectory;

    @Option(names = {"--sections"},
            description = "Required sections (comma-separated)",
            split = ",")
    private List<String> requiredSections = List.of();

    @Option(names = {"--related"},
            description = "Related pages for internal linking (comma-separated)",
            split = ",")
    private List<String> relatedPages = List.of();

    @Option(names = {"--auto-approve"},
            description = "Skip all approval prompts (for scripting)")
    private boolean autoApprove;

    @Option(names = {"-v", "--verbose"},
            description = "Enable verbose output")
    private boolean verbose;

    @Option(names = {"-q", "--quiet"},
            description = "Suppress non-essential output")
    private boolean quiet;

    @Option(names = {"-k", "--key"},
            description = "Anthropic API key (overrides environment variable)")
    private String apiKey;

    @Option(names = {"--key-file"},
            description = "Path to file containing Anthropic API key")
    private Path keyFile;

    // For testing - allows injecting a custom reader
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Default constructor for Picocli/Spring integration.
     */
    public AiPublisherCommand() {
        // Dependencies will be injected via setters
    }

    /**
     * Set the pipeline supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setPipelineProvider(ObjectProvider<PublishingPipeline> pipelineProvider) {
        this.pipelineSupplier = pipelineProvider::getObject;
    }

    /**
     * Set the approval service supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setApprovalServiceProvider(ObjectProvider<ApprovalService> approvalServiceProvider) {
        this.approvalServiceSupplier = approvalServiceProvider::getObject;
    }

    /**
     * Constructor for testing - uses direct instances.
     */
    public AiPublisherCommand(PublishingPipeline pipeline, ApprovalService approvalService) {
        this.pipelineSupplier = () -> pipeline;
        this.approvalServiceSupplier = () -> approvalService;
    }

    /**
     * Set custom input/output streams for testing.
     */
    public void setStreams(BufferedReader reader, PrintWriter writer) {
        this.inputReader = reader;
        this.outputWriter = writer;
    }

    @Override
    public Integer call() {
        PrintWriter out = outputWriter != null ? outputWriter : new PrintWriter(System.out, true);
        BufferedReader in = inputReader != null ? inputReader : new BufferedReader(new InputStreamReader(System.in));

        try {
            // Configure API key if provided via CLI
            if (!configureApiKey(out)) {
                return 1;
            }

            // Interactive mode if topic not specified
            if (topic == null || topic.isBlank()) {
                topic = promptForTopic(in, out);
                if (topic == null) {
                    return 0; // User cancelled
                }
                // Also prompt for audience and word count in interactive mode
                promptForInteractiveOptions(in, out);
            }

            // Configure auto-approval if requested
            if (autoApprove) {
                approvalServiceSupplier.get().setCallback(createAutoApproveCallback());
            }

            // Display banner
            if (!quiet) {
                printBanner(out);
                out.println();
                out.println("Topic: " + topic);
                out.println("Audience: " + audience);
                out.println("Target words: " + wordCount);
                out.println();
            }

            // Create topic brief and execute pipeline
            TopicBrief topicBrief = new TopicBrief(
                    topic,
                    audience,
                    wordCount,
                    requiredSections,
                    relatedPages,
                    List.of()
            );

            PipelineResult result = pipelineSupplier.get().execute(topicBrief);

            // Display results
            printResults(out, result);

            return result.success() ? 0 : 1;

        } catch (Exception e) {
            out.println();
            out.println("ERROR: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    private String promptForTopic(BufferedReader in, PrintWriter out) {
        try {
            printBanner(out);
            out.println();
            out.print("Enter topic (or 'quit' to exit): ");
            out.flush();

            String input = in.readLine();
            if (input == null || input.equalsIgnoreCase("quit") || input.equalsIgnoreCase("q")) {
                out.println("Goodbye!");
                return null;
            }

            return input.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private void promptForInteractiveOptions(BufferedReader in, PrintWriter out) {
        try {
            // Prompt for audience
            out.println();
            out.print("Target audience [" + audience + "]: ");
            out.flush();
            String audienceInput = in.readLine();
            if (audienceInput != null && !audienceInput.trim().isEmpty()) {
                audience = audienceInput.trim();
            }

            // Prompt for word count
            out.print("Target word count [" + wordCount + "]: ");
            out.flush();
            String wordCountInput = in.readLine();
            if (wordCountInput != null && !wordCountInput.trim().isEmpty()) {
                try {
                    int parsed = Integer.parseInt(wordCountInput.trim());
                    if (parsed > 0) {
                        wordCount = parsed;
                    }
                } catch (NumberFormatException e) {
                    out.println("Invalid number, using default: " + wordCount);
                }
            }
        } catch (Exception e) {
            // Keep defaults on error
        }
    }

    private void printBanner(PrintWriter out) {
        out.println("╔═══════════════════════════════════════════════════════════╗");
        out.println("║                      AI PUBLISHER                         ║");
        out.println("║     Generate well-researched articles with AI agents      ║");
        out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    private void printResults(PrintWriter out, PipelineResult result) {
        out.println();
        out.println("═".repeat(60));

        if (result.success()) {
            out.println("SUCCESS! Article published.");
            out.println();
            out.println("Output: " + result.outputPath());

            if (result.document().getFinalArticle() != null) {
                out.println("Quality score: " + String.format("%.2f", result.document().getFinalArticle().qualityScore()));
                out.println("Word count: " + result.document().getFinalArticle().estimateWordCount());
            }
        } else {
            out.println("FAILED at phase: " + result.failedAtState());
            out.println("Error: " + result.errorMessage());

            // Show path to saved failed document for debugging
            result.getFailedDocumentPath().ifPresent(path -> {
                out.println();
                out.println("Debug document saved: " + path);
                out.println("(Contains draft content and fact-check issues for review)");
            });
        }

        out.println();
        out.println("Total time: " + result.totalTime().toMillis() + " ms");
        out.println("═".repeat(60));
    }

    private ApprovalCallback createAutoApproveCallback() {
        return request -> ApprovalDecision.approve(request.id(), "auto-approved");
    }

    /**
     * Configure the API key from CLI options.
     * Priority: --key > --key-file > ANTHROPIC_API_KEY env var
     *
     * @return true if API key is configured, false on error
     */
    private boolean configureApiKey(PrintWriter out) {
        String key = null;

        // Priority 1: Direct key from command line
        if (apiKey != null && !apiKey.isBlank()) {
            key = apiKey.trim();
            if (verbose) {
                out.println("Using API key from --key option");
            }
        }
        // Priority 2: Key file
        else if (keyFile != null) {
            try {
                if (!Files.exists(keyFile)) {
                    out.println("ERROR: Key file not found: " + keyFile);
                    return false;
                }
                key = Files.readString(keyFile).trim();
                if (key.isBlank()) {
                    out.println("ERROR: Key file is empty: " + keyFile);
                    return false;
                }
                if (verbose) {
                    out.println("Using API key from file: " + keyFile);
                }
            } catch (Exception e) {
                out.println("ERROR: Failed to read key file: " + e.getMessage());
                return false;
            }
        }

        // Set the API key as system property if provided via CLI
        if (key != null) {
            System.setProperty("ANTHROPIC_API_KEY", key);
        }

        return true;
    }

    // Getters for testing
    public String getTopic() {
        return topic;
    }

    public String getAudience() {
        return audience;
    }

    public int getWordCount() {
        return wordCount;
    }

    public boolean isAutoApprove() {
        return autoApprove;
    }

    public String getApiKey() {
        return apiKey;
    }

    public Path getKeyFile() {
        return keyFile;
    }
}
