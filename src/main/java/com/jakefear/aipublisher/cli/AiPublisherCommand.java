package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.approval.ApprovalCallback;
import com.jakefear.aipublisher.approval.ApprovalDecision;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.content.ContentTypeSelector;
import com.jakefear.aipublisher.discovery.CostProfile;
import com.jakefear.aipublisher.discovery.GapAnalyzer;
import com.jakefear.aipublisher.discovery.RelationshipSuggester;
import com.jakefear.aipublisher.discovery.TopicExpander;
import com.jakefear.aipublisher.domain.TopicUniverse;
import com.jakefear.aipublisher.domain.TopicUniverseRepository;
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
 * - Content type selection
 * - Auto-approve mode for scripting
 */
@Component
@Command(
        name = "aipublisher",
        mixinStandardHelpOptions = true,
        version = "AI Publisher 0.2.0",
        description = "Generate well-researched, fact-checked articles using AI agents.",
        footer = {
                "",
                "Examples:",
                "  aipublisher                                    # Interactive mode",
                "  aipublisher -t \"Apache Kafka\"                  # Simple topic",
                "  aipublisher -t \"How to use Docker\" --type tutorial",
                "  aipublisher -t \"Kafka vs RabbitMQ\" --type comparison",
                "  aipublisher -t \"Docker\" -a \"DevOps engineers\" -w 1000 --auto-approve",
                "",
                "Content Types:",
                "  concept         Explains what something is",
                "  tutorial        Step-by-step guide",
                "  reference       Quick lookup information",
                "  guide           Decision support and best practices",
                "  comparison      Analyzes alternatives",
                "  troubleshooting Problem diagnosis and solutions",
                "  overview        High-level introduction",
                "",
                "Discovery Mode:",
                "  aipublisher --discover                         # Interactive domain discovery",
                "  aipublisher --discover -c minimal              # Quick prototype mode",
                "  aipublisher --discover --cost-profile balanced # Standard coverage",
                "",
                "Cost Profiles (for --discover):",
                "  MINIMAL       Quick prototype, 2-4 topics, ~$0.50-2",
                "  BALANCED      Good coverage, 9-31 topics, ~$2-5 (default)",
                "  COMPREHENSIVE Full coverage, 25-150 topics, ~$5-15",
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
    private Supplier<ContentTypeSelector> contentTypeSelectorSupplier;
    private Supplier<TopicExpander> topicExpanderSupplier;
    private Supplier<RelationshipSuggester> relationshipSuggesterSupplier;
    private Supplier<GapAnalyzer> gapAnalyzerSupplier;
    private Supplier<TopicUniverseRepository> universeRepositorySupplier;

    @Option(names = {"-t", "--topic"},
            description = "Topic to write about (launches interactive mode if not specified)")
    private String topic;

    @Option(names = {"--type"},
            description = "Content type: concept, tutorial, reference, guide, comparison, troubleshooting, overview")
    private String contentTypeStr;

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

    @Option(names = {"--context"},
            description = "Domain context (e.g., 'e-commerce', 'microservices')")
    private String domainContext;

    @Option(names = {"--goal"},
            description = "Specific goal or outcome for tutorials and guides")
    private String specificGoal;

    @Option(names = {"--auto-approve"},
            description = "Skip all approval prompts (for scripting)")
    private boolean autoApprove;

    @Option(names = {"-i", "--interactive"},
            description = "Force interactive mode even with topic specified")
    private boolean forceInteractive;

    @Option(names = {"--discover"},
            description = "Launch interactive domain discovery session")
    private boolean discoverMode;

    @Option(names = {"--cost-profile", "-c"},
            description = "Cost profile for discovery: MINIMAL (quick prototype), BALANCED (most projects), COMPREHENSIVE (enterprise docs)")
    private String costProfileStr;

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
     * Set the content type selector supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setContentTypeSelectorProvider(ObjectProvider<ContentTypeSelector> contentTypeSelectorProvider) {
        this.contentTypeSelectorSupplier = contentTypeSelectorProvider::getObject;
    }

    /**
     * Set the topic expander supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setTopicExpanderProvider(ObjectProvider<TopicExpander> topicExpanderProvider) {
        this.topicExpanderSupplier = topicExpanderProvider::getObject;
    }

    /**
     * Set the relationship suggester supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setRelationshipSuggesterProvider(ObjectProvider<RelationshipSuggester> relationshipSuggesterProvider) {
        this.relationshipSuggesterSupplier = relationshipSuggesterProvider::getObject;
    }

    /**
     * Set the gap analyzer supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setGapAnalyzerProvider(ObjectProvider<GapAnalyzer> gapAnalyzerProvider) {
        this.gapAnalyzerSupplier = gapAnalyzerProvider::getObject;
    }

    /**
     * Set the universe repository supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setUniverseRepositoryProvider(ObjectProvider<TopicUniverseRepository> universeRepositoryProvider) {
        this.universeRepositorySupplier = universeRepositoryProvider::getObject;
    }

    /**
     * Constructor for testing - uses direct instances.
     */
    public AiPublisherCommand(PublishingPipeline pipeline, ApprovalService approvalService, ContentTypeSelector contentTypeSelector) {
        this.pipelineSupplier = () -> pipeline;
        this.approvalServiceSupplier = () -> approvalService;
        this.contentTypeSelectorSupplier = () -> contentTypeSelector;
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

            // Handle discovery mode
            if (discoverMode) {
                return runDiscoveryMode(in, out);
            }

            TopicBrief topicBrief;

            // Determine if we should use interactive mode
            boolean useInteractive = forceInteractive || (topic == null || topic.isBlank());

            if (useInteractive) {
                // Run full interactive session
                InteractiveSession session = new InteractiveSession(in, out, contentTypeSelectorSupplier.get());
                if (!session.run()) {
                    return 0; // User cancelled
                }

                // Build TopicBrief from session
                topicBrief = TopicBrief.builder(session.getTopic())
                        .targetAudience(session.getAudience())
                        .targetWordCount(session.getWordCount())
                        .contentType(session.getContentType())
                        .requiredSections(session.getRequiredSections())
                        .relatedPages(session.getRelatedPages())
                        .domainContext(session.getDomainContext())
                        .specificGoal(session.getSpecificGoal())
                        .build();

                // Set auto-approve for post-interactive execution
                autoApprove = true;

            } else {
                // Parse content type if specified
                ContentType contentType = null;
                if (contentTypeStr != null && !contentTypeStr.isBlank()) {
                    contentType = ContentType.fromString(contentTypeStr);
                    if (contentType == null) {
                        out.println("WARNING: Unrecognized content type '" + contentTypeStr + "'. Will auto-detect.");
                    }
                }

                // Auto-detect content type if not specified
                if (contentType == null) {
                    contentType = contentTypeSelectorSupplier.get().detectFromTopic(topic);
                    if (verbose) {
                        out.println("Auto-detected content type: " + contentType.getDisplayName());
                    }
                }

                // Build TopicBrief from CLI options
                topicBrief = TopicBrief.builder(topic)
                        .targetAudience(audience)
                        .targetWordCount(wordCount)
                        .contentType(contentType)
                        .requiredSections(requiredSections)
                        .relatedPages(relatedPages)
                        .domainContext(domainContext)
                        .specificGoal(specificGoal)
                        .build();
            }

            // Configure auto-approval if requested
            if (autoApprove) {
                approvalServiceSupplier.get().setCallback(createAutoApproveCallback());
            }

            // Display configuration
            if (!quiet) {
                printConfiguration(out, topicBrief);
            }

            // Execute pipeline
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

    private void printConfiguration(PrintWriter out, TopicBrief brief) {
        out.println();
        out.println("╔═══════════════════════════════════════════════════════════╗");
        out.println("║                      AI PUBLISHER                         ║");
        out.println("║     Generate well-researched articles with AI agents      ║");
        out.println("╚═══════════════════════════════════════════════════════════╝");
        out.println();
        out.println("Topic:        " + brief.topic());
        out.println("Content Type: " + (brief.contentType() != null ? brief.contentType().getDisplayName() : "Auto"));
        out.println("Audience:     " + brief.targetAudience());
        out.println("Target words: " + brief.targetWordCount());

        if (brief.domainContext() != null && !brief.domainContext().isEmpty()) {
            out.println("Context:      " + brief.domainContext());
        }
        if (brief.specificGoal() != null && !brief.specificGoal().isEmpty()) {
            out.println("Goal:         " + brief.specificGoal());
        }
        if (!brief.relatedPages().isEmpty()) {
            out.println("Related:      " + String.join(", ", brief.relatedPages()));
        }
        out.println();
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

    public String getContentTypeStr() {
        return contentTypeStr;
    }

    public boolean isForceInteractive() {
        return forceInteractive;
    }

    public boolean isDiscoverMode() {
        return discoverMode;
    }

    public String getCostProfileStr() {
        return costProfileStr;
    }

    /**
     * Run the interactive domain discovery session.
     */
    private Integer runDiscoveryMode(BufferedReader in, PrintWriter out) {
        try {
            // Parse cost profile from CLI if provided
            CostProfile costProfile = null;
            if (costProfileStr != null && !costProfileStr.isBlank()) {
                costProfile = CostProfile.fromName(costProfileStr);
                if (costProfile == null) {
                    out.println("WARNING: Unrecognized cost profile '" + costProfileStr + "'. Will prompt for selection.");
                }
            }

            DiscoveryInteractiveSession discoverySession = new DiscoveryInteractiveSession(
                    in,
                    out,
                    topicExpanderSupplier.get(),
                    relationshipSuggesterSupplier.get(),
                    gapAnalyzerSupplier.get(),
                    costProfile
            );

            TopicUniverse universe = discoverySession.run();

            if (universe == null) {
                // User cancelled
                return 0;
            }

            // Save the universe
            TopicUniverseRepository repository = universeRepositorySupplier.get();
            Path savedPath = repository.save(universe);

            out.println();
            out.println("═".repeat(67));
            out.println("Topic universe saved!");
            out.println();
            out.printf("  ID:       %s%n", universe.id());
            out.printf("  Name:     %s%n", universe.name());
            out.printf("  Topics:   %d accepted%n", universe.getAcceptedCount());
            out.printf("  Location: %s%n", savedPath);
            out.println();
            out.println("To generate articles from this universe, use:");
            out.printf("  aipublisher --universe %s%n", universe.id());
            out.println("═".repeat(67));

            return 0;

        } catch (Exception e) {
            out.println();
            out.println("ERROR in discovery mode: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }
}
