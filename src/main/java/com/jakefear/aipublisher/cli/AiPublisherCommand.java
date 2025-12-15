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
import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.domain.Topic;
import com.jakefear.aipublisher.domain.TopicRelationship;
import com.jakefear.aipublisher.domain.TopicUniverse;
import com.jakefear.aipublisher.domain.TopicUniverseRepository;
import com.jakefear.aipublisher.document.TopicBrief;
import com.jakefear.aipublisher.gap.GapConcept;
import com.jakefear.aipublisher.gap.StubGenerationService;
import com.jakefear.aipublisher.pipeline.PipelineResult;
import com.jakefear.aipublisher.pipeline.PublishingPipeline;
import com.jakefear.aipublisher.util.PageNameUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.function.Supplier;
import java.util.ArrayList;
import java.util.stream.Collectors;

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
                "Universe Mode (generate from saved universe):",
                "  aipublisher --universe investing-basics        # Generate from universe ID",
                "  aipublisher -u investing-basics                # Short form",
                "  aipublisher -u myuniverse --generate-stubs     # Also generate stubs for gaps",
                "",
                "Stub Generation (fill gaps in existing wiki):",
                "  aipublisher --analyze-gaps                     # Report gaps only",
                "  aipublisher --stubs-only                       # Generate stubs for existing content",
                "  aipublisher --stubs-only --context \"Finance\"   # With domain context",
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
    private Supplier<OutputProperties> outputPropertiesSupplier;
    private Supplier<ChatLanguageModel> summaryModelSupplier;
    private Supplier<StubGenerationService> stubGenerationServiceSupplier;

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

    @Option(names = {"--universe", "-u"},
            description = "Generate articles from a saved topic universe (by ID or file path)")
    private String universeId;

    @Option(names = {"--generate-stubs"},
            description = "Generate stub pages for gap concepts after universe generation")
    private boolean generateStubs;

    @Option(names = {"--stubs-only"},
            description = "Only generate stub pages for existing wiki content (skip main article generation)")
    private boolean stubsOnly;

    @Option(names = {"--analyze-gaps"},
            description = "Analyze wiki content for gaps without generating stubs (report only)")
    private boolean analyzeGaps;

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
     * Set the output properties supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setOutputPropertiesProvider(ObjectProvider<OutputProperties> outputPropertiesProvider) {
        this.outputPropertiesSupplier = outputPropertiesProvider::getObject;
    }

    /**
     * Set the summary model supplier (called by Spring via @Autowired).
     * Uses the writer model for generating summaries.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSummaryModelProvider(@Qualifier("writerChatModel") ObjectProvider<ChatLanguageModel> summaryModelProvider) {
        this.summaryModelSupplier = summaryModelProvider::getObject;
    }

    /**
     * Set the stub generation service supplier (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setStubGenerationServiceProvider(ObjectProvider<StubGenerationService> stubGenerationServiceProvider) {
        this.stubGenerationServiceSupplier = stubGenerationServiceProvider::getObject;
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

            // Handle stubs-only mode - generate stubs for existing content
            // Check this BEFORE universe mode since stubs-only is more specific
            if (stubsOnly) {
                return runStubsOnlyMode(in, out);
            }

            // Handle analyze-gaps mode - report only, no generation
            if (analyzeGaps) {
                return runAnalyzeGapsMode(out);
            }

            // Handle universe mode - generate articles from saved universe
            if (universeId != null && !universeId.isBlank()) {
                return runUniverseMode(in, out);
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

    /**
     * Run article generation from a saved topic universe.
     */
    private Integer runUniverseMode(BufferedReader in, PrintWriter out) {
        try {
            TopicUniverseRepository repository = universeRepositorySupplier.get();

            // Load the universe
            var universeOpt = repository.load(universeId);
            if (universeOpt.isEmpty()) {
                // Try loading as a file path
                Path path = Path.of(universeId);
                if (Files.exists(path)) {
                    universeOpt = repository.loadFromPath(path);
                }
            }

            if (universeOpt.isEmpty()) {
                out.println("ERROR: Universe not found: " + universeId);
                out.println();
                out.println("Available universes:");
                var available = repository.listAll();
                if (available.isEmpty()) {
                    out.println("  (none)");
                } else {
                    for (String id : available) {
                        out.println("  - " + id);
                    }
                }
                out.println();
                out.println("Create a new universe with: aipublisher --discover");
                return 1;
            }

            TopicUniverse universe = universeOpt.get();

            out.println();
            out.println("╔═══════════════════════════════════════════════════════════════════╗");
            out.println("║              AI PUBLISHER - UNIVERSE GENERATION MODE              ║");
            out.println("╚═══════════════════════════════════════════════════════════════════╝");
            out.println();
            out.printf("Universe: %s%n", universe.name());
            out.printf("Topics:   %d to generate%n", universe.getAcceptedCount());
            out.println();

            // Get topics in generation order
            var topics = universe.getGenerationOrder();

            out.println("Topics in generation order:");
            for (int i = 0; i < topics.size(); i++) {
                var topic = topics.get(i);
                out.printf("  %2d. %s [%s]%n", i + 1, topic.name(), topic.priority().getDisplayName());
            }
            out.println();

            // Confirm before starting
            out.print("Generate articles for all topics? [Y/n]: ");
            out.flush();
            String response = in.readLine();
            if (response != null && response.toLowerCase().startsWith("n")) {
                out.println("Cancelled.");
                return 0;
            }

            // Configure auto-approval
            approvalServiceSupplier.get().setCallback(createAutoApproveCallback());

            int successCount = 0;
            int failCount = 0;
            List<Topic> successfulTopics = new ArrayList<>();

            for (int i = 0; i < topics.size(); i++) {
                var topic = topics.get(i);
                out.println();
                out.println("─".repeat(67));
                out.printf("Generating %d/%d: %s%n", i + 1, topics.size(), topic.name());
                out.println("─".repeat(67));

                // Build TopicBrief from universe topic
                TopicBrief brief = TopicBrief.builder(topic.name())
                        .targetAudience(audience)
                        .targetWordCount(topic.estimatedWords() > 0 ? topic.estimatedWords() : wordCount)
                        .contentType(topic.contentType())
                        .domainContext(universe.name())
                        .build();

                try {
                    PipelineResult result = pipelineSupplier.get().execute(brief);

                    if (result.success()) {
                        successCount++;
                        successfulTopics.add(topic);
                        out.printf("✓ Success: %s%n", result.outputPath());
                    } else {
                        failCount++;
                        out.printf("✗ Failed: %s%n", result.errorMessage());
                    }
                } catch (Exception e) {
                    failCount++;
                    out.printf("✗ Error: %s%n", e.getMessage());
                    if (verbose) {
                        e.printStackTrace(out);
                    }
                }
            }

            // Generate summary page if any topics succeeded
            if (!successfulTopics.isEmpty()) {
                Path summaryPath = generateSummaryPage(universe, successfulTopics, out);
                if (summaryPath != null) {
                    out.println();
                    out.printf("Summary page: %s%n", summaryPath);
                }
            }

            // Generate stubs for gap concepts if requested
            if (generateStubs && !successfulTopics.isEmpty()) {
                out.println();
                out.println("─".repeat(67));
                out.println("Generating stub pages for gap concepts...");
                out.println("─".repeat(67));

                try {
                    StubGenerationService.StubGenerationResult stubResult =
                            stubGenerationServiceSupplier.get().generateStubs(universe.name(), audience, out);

                    out.println();
                    out.printf("Stubs: %d generated, %d redirects, %d ignored, %d flagged for review%n",
                            stubResult.stubsGenerated(),
                            stubResult.redirectsGenerated(),
                            stubResult.ignored(),
                            stubResult.flaggedForReview());

                    if (stubResult.hasReviewItems()) {
                        out.println();
                        out.println("Topics flagged for full article generation:");
                        for (GapConcept gap : stubResult.reviewNeeded()) {
                            out.printf("  - %s%n", gap.name());
                        }
                    }
                } catch (Exception e) {
                    out.printf("Warning: Stub generation failed: %s%n", e.getMessage());
                    if (verbose) {
                        e.printStackTrace(out);
                    }
                }
            }

            out.println();
            out.println("═".repeat(67));
            out.printf("Generation complete: %d succeeded, %d failed%n", successCount, failCount);
            out.println("═".repeat(67));

            return failCount > 0 ? 1 : 0;

        } catch (Exception e) {
            out.println();
            out.println("ERROR in universe mode: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    public String getUniverseId() {
        return universeId;
    }

    public boolean isGenerateStubs() {
        return generateStubs;
    }

    public boolean isStubsOnly() {
        return stubsOnly;
    }

    public boolean isAnalyzeGaps() {
        return analyzeGaps;
    }

    /**
     * Run stub generation for existing wiki content without main article generation.
     */
    private Integer runStubsOnlyMode(BufferedReader in, PrintWriter out) {
        try {
            out.println();
            out.println("╔═══════════════════════════════════════════════════════════════════╗");
            out.println("║              AI PUBLISHER - STUB GENERATION MODE                  ║");
            out.println("╚═══════════════════════════════════════════════════════════════════╝");
            out.println();

            // Get universe name for context - try universe ID first, then domainContext
            String universeName = null;
            String targetAudience = audience;

            // If universe ID provided, load it to get context
            if (universeId != null && !universeId.isBlank()) {
                TopicUniverseRepository repository = universeRepositorySupplier.get();
                var universeOpt = repository.load(universeId);
                if (universeOpt.isEmpty()) {
                    Path path = Path.of(universeId);
                    if (Files.exists(path)) {
                        universeOpt = repository.loadFromPath(path);
                    }
                }
                if (universeOpt.isPresent()) {
                    TopicUniverse universe = universeOpt.get();
                    universeName = universe.name();
                    // Use universe's audience if available and not overridden
                    if (universe.scope() != null && universe.scope().audienceDescription() != null
                            && !universe.scope().audienceDescription().isBlank()
                            && "general readers".equals(audience)) {
                        targetAudience = universe.scope().audienceDescription();
                    }
                    out.printf("Loaded universe: %s%n", universeId);
                } else {
                    out.printf("Warning: Universe '%s' not found, using as context name%n", universeId);
                    universeName = universeId;
                }
            }

            // Fall back to domainContext or default
            if (universeName == null || universeName.isBlank()) {
                universeName = domainContext != null && !domainContext.isBlank()
                        ? domainContext
                        : "Wiki Collection";
            }

            out.printf("Domain context: %s%n", universeName);
            out.printf("Target audience: %s%n", targetAudience);
            out.println();

            // Confirm before starting
            out.print("Generate stub pages for gap concepts? [Y/n]: ");
            out.flush();
            String response = in.readLine();
            if (response != null && response.toLowerCase().startsWith("n")) {
                out.println("Cancelled.");
                return 0;
            }

            StubGenerationService.StubGenerationResult result =
                    stubGenerationServiceSupplier.get().generateStubs(universeName, targetAudience, out);

            out.println();
            out.println("═".repeat(67));
            out.println("Stub generation complete!");
            out.println();
            out.printf("  Gaps detected:    %d%n", result.gapsDetected());
            out.printf("  Stubs generated:  %d%n", result.stubsGenerated());
            out.printf("  Redirects:        %d%n", result.redirectsGenerated());
            out.printf("  Ignored:          %d%n", result.ignored());
            out.printf("  Flagged review:   %d%n", result.flaggedForReview());
            if (result.skipped() > 0) {
                out.printf("  Skipped (exist):  %d%n", result.skipped());
            }
            if (result.failed() > 0) {
                out.printf("  Failed:           %d%n", result.failed());
            }

            if (result.hasReviewItems()) {
                out.println();
                out.println("Topics that need full articles (add to universe):");
                for (GapConcept gap : result.reviewNeeded()) {
                    out.printf("  - %s (referenced by: %s)%n",
                            gap.name(),
                            String.join(", ", gap.referencedBy()));
                }
            }

            out.println("═".repeat(67));

            return result.failed() > 0 ? 1 : 0;

        } catch (Exception e) {
            out.println();
            out.println("ERROR in stub generation mode: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    /**
     * Analyze wiki content for gaps without generating anything.
     */
    private Integer runAnalyzeGapsMode(PrintWriter out) {
        try {
            out.println();
            out.println("╔═══════════════════════════════════════════════════════════════════╗");
            out.println("║              AI PUBLISHER - GAP ANALYSIS MODE                     ║");
            out.println("╚═══════════════════════════════════════════════════════════════════╝");
            out.println();

            // Get universe name for context - try universe ID first, then domainContext
            String universeName = null;

            if (universeId != null && !universeId.isBlank()) {
                TopicUniverseRepository repository = universeRepositorySupplier.get();
                var universeOpt = repository.load(universeId);
                if (universeOpt.isEmpty()) {
                    Path path = Path.of(universeId);
                    if (Files.exists(path)) {
                        universeOpt = repository.loadFromPath(path);
                    }
                }
                if (universeOpt.isPresent()) {
                    universeName = universeOpt.get().name();
                    out.printf("Loaded universe: %s%n", universeId);
                }
            }

            if (universeName == null || universeName.isBlank()) {
                universeName = domainContext != null && !domainContext.isBlank()
                        ? domainContext
                        : "Wiki Collection";
            }

            java.util.List<GapConcept> gaps =
                    stubGenerationServiceSupplier.get().analyzeGaps(universeName, out);

            out.println();
            out.println("═".repeat(67));

            if (gaps.isEmpty()) {
                out.println("No gaps detected - all internal links resolve to existing pages.");
            } else {
                out.printf("Total gaps: %d%n", gaps.size());
                out.println();
                out.println("To generate stubs for these gaps, use:");
                out.println("  aipublisher --stubs-only");
                if (domainContext != null && !domainContext.isBlank()) {
                    out.println("  aipublisher --stubs-only --context \"" + domainContext + "\"");
                }
            }

            out.println("═".repeat(67));

            return 0;

        } catch (Exception e) {
            out.println();
            out.println("ERROR in gap analysis mode: " + e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return 1;
        }
    }

    /**
     * Generate a summary page for the universe with links to all generated articles.
     * Uses the LLM to create a meaningful summary based on universe metadata.
     *
     * @param universe The topic universe
     * @param successfulTopics List of topics that were successfully generated
     * @param out PrintWriter for output messages
     * @return Path to the generated summary file, or null if generation failed
     */
    private Path generateSummaryPage(TopicUniverse universe, List<Topic> successfulTopics, PrintWriter out) {
        try {
            OutputProperties outputProps = outputPropertiesSupplier.get();
            Path outputDir = outputProps.getDirectoryPath();
            String extension = outputProps.getFileExtension();

            // Create the summary filename: UniverseNameSummary.txt
            String universeCamelCase = PageNameUtils.toCamelCaseOrDefault(universe.name(), "Universe");
            String summaryFilename = universeCamelCase + "Summary" + extension;
            Path summaryPath = outputDir.resolve(summaryFilename);

            out.println("Generating summary page...");

            // Build the prompt for the LLM
            String prompt = buildSummaryPrompt(universe, successfulTopics);

            // Generate summary using LLM
            String generatedSummary;
            try {
                ChatLanguageModel model = summaryModelSupplier.get();
                generatedSummary = model.generate(prompt);
            } catch (Exception e) {
                out.printf("Warning: LLM summary generation failed, using fallback: %s%n", e.getMessage());
                generatedSummary = buildFallbackSummary(universe, successfulTopics);
            }

            // Build final content with generated summary and article links
            StringBuilder content = new StringBuilder();

            // Title
            content.append("!!! ").append(universe.name()).append("\n\n");

            // LLM-generated summary
            content.append(generatedSummary.trim()).append("\n\n");

            // Links section
            content.append("!! Articles\n\n");

            // Sort topics by name for consistent ordering
            List<Topic> sortedTopics = new ArrayList<>(successfulTopics);
            sortedTopics.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));

            for (Topic topic : sortedTopics) {
                String pageName = PageNameUtils.toCamelCaseOrDefault(topic.name(), "UnnamedPage");
                content.append("* [").append(topic.name()).append("|").append(pageName).append("]");
                // Add brief description if available
                if (topic.description() != null && !topic.description().isBlank()) {
                    // Truncate long descriptions
                    String desc = topic.description();
                    if (desc.length() > 80) {
                        desc = desc.substring(0, 77) + "...";
                    }
                    content.append(" - ").append(desc);
                }
                content.append("\n");
            }

            content.append("\n----\n");
            content.append("Generated by AI Publisher\n");

            // Write the file
            Files.createDirectories(outputDir);
            Files.writeString(summaryPath, content.toString());

            return summaryPath;

        } catch (Exception e) {
            out.printf("Warning: Failed to generate summary page: %s%n", e.getMessage());
            if (verbose) {
                e.printStackTrace(out);
            }
            return null;
        }
    }

    /**
     * Build the prompt for generating the summary using the LLM.
     */
    private String buildSummaryPrompt(TopicUniverse universe, List<Topic> successfulTopics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Write a brief, informative summary paragraph (3-5 sentences) for a wiki collection.\n\n");

        prompt.append("COLLECTION: ").append(universe.name()).append("\n");
        if (universe.description() != null && !universe.description().isBlank()) {
            prompt.append("DESCRIPTION: ").append(universe.description()).append("\n");
        }
        prompt.append("TARGET AUDIENCE: ").append(audience).append("\n\n");

        prompt.append("TOPICS COVERED:\n");
        for (Topic topic : successfulTopics) {
            prompt.append("- ").append(topic.name());
            if (topic.description() != null && !topic.description().isBlank()) {
                prompt.append(": ").append(topic.description());
            }
            if (topic.contentType() != null) {
                prompt.append(" [").append(topic.contentType().getDisplayName()).append("]");
            }
            prompt.append("\n");
        }

        // Include key relationships to help the model understand structure
        List<TopicRelationship> activeRelationships = universe.relationships().stream()
                .filter(TopicRelationship::isActive)
                .limit(10) // Limit to avoid prompt bloat
                .collect(Collectors.toList());

        if (!activeRelationships.isEmpty()) {
            prompt.append("\nKEY RELATIONSHIPS:\n");
            for (TopicRelationship rel : activeRelationships) {
                universe.getTopicById(rel.sourceTopicId()).ifPresent(source ->
                        universe.getTopicById(rel.targetTopicId()).ifPresent(target ->
                                prompt.append("- ").append(source.name())
                                        .append(" ").append(rel.type().getDisplayName().toLowerCase())
                                        .append(" ").append(target.name()).append("\n")));
            }
        }

        prompt.append("\nWrite ONLY the summary paragraph. Do not include a title, bullet points, or article links.\n");
        prompt.append("The summary should help readers understand what this collection covers and how to use it.\n");
        prompt.append("Keep it concise and informative - ideal for embedding in an existing wiki.\n");

        return prompt.toString();
    }

    /**
     * Build a fallback summary if LLM generation fails.
     */
    private String buildFallbackSummary(TopicUniverse universe, List<Topic> successfulTopics) {
        StringBuilder summary = new StringBuilder();
        summary.append("This collection covers ").append(universe.name().toLowerCase());
        summary.append(", providing ").append(successfulTopics.size()).append(" articles for ").append(audience);
        summary.append(". Topics range from foundational concepts to practical applications.");
        if (universe.description() != null && !universe.description().isBlank()) {
            summary.append(" ").append(universe.description());
        }
        return summary.toString();
    }
}
