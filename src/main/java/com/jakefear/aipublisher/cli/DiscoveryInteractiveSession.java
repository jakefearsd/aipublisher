package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.curation.relationship.RelationshipCurationCommandFactory;
import com.jakefear.aipublisher.cli.curation.topic.TopicCurationCommandFactory;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.*;
import com.jakefear.aipublisher.domain.*;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Interactive CLI session for domain discovery.
 * Guides users through building a comprehensive topic universe with AI assistance.
 */
public class DiscoveryInteractiveSession {

    private final BufferedReader in;
    private final PrintWriter out;
    private final ConsoleInputHelper input;
    private final TopicExpander topicExpander;
    private final RelationshipSuggester relationshipSuggester;
    private final GapAnalyzer gapAnalyzer;
    private final TopicCurationCommandFactory topicCurationFactory;
    private final RelationshipCurationCommandFactory relationshipCurationFactory;

    private DiscoverySession session;

    public DiscoveryInteractiveSession(
            BufferedReader in,
            PrintWriter out,
            TopicExpander topicExpander,
            RelationshipSuggester relationshipSuggester,
            GapAnalyzer gapAnalyzer) {
        this.in = in;
        this.out = out;
        this.input = new ConsoleInputHelper(in, out);
        this.topicExpander = topicExpander;
        this.relationshipSuggester = relationshipSuggester;
        this.gapAnalyzer = gapAnalyzer;
        this.topicCurationFactory = new TopicCurationCommandFactory();
        this.relationshipCurationFactory = new RelationshipCurationCommandFactory();
    }

    /**
     * Run the complete discovery session.
     *
     * @return The completed topic universe, or null if cancelled
     */
    public TopicUniverse run() {
        try {
            printWelcome();

            // Phase 1: Domain name and seed topics
            if (!runSeedInputPhase()) {
                return null;
            }

            // Phase 2: Scope configuration (optional)
            if (!runScopeSetupPhase()) {
                return null;
            }

            // Phase 3: Topic expansion
            if (!runTopicExpansionPhase()) {
                return null;
            }

            // Phase 4: Relationship mapping
            if (!runRelationshipMappingPhase()) {
                return null;
            }

            // Phase 5: Gap analysis
            if (!runGapAnalysisPhase()) {
                return null;
            }

            // Phase 6: Depth calibration (optional)
            if (!runDepthCalibrationPhase()) {
                return null;
            }

            // Phase 7: Prioritization
            if (!runPrioritizationPhase()) {
                return null;
            }

            // Phase 8: Review and confirm
            if (!runReviewPhase()) {
                return null;
            }

            session.goToPhase(DiscoveryPhase.COMPLETE);
            return session.buildUniverse();

        } catch (Exception e) {
            out.println("\n⚠ Session interrupted: " + e.getMessage());
            return null;
        }
    }

    private void printWelcome() {
        out.println();
        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║              AI PUBLISHER - DOMAIN DISCOVERY MODE                 ║");
        out.println("║                                                                   ║");
        out.println("║   Build a comprehensive topic universe for your wiki with AI.    ║");
        out.println("║   You'll curate AI suggestions to shape your content strategy.   ║");
        out.println("║                                                                   ║");
        out.println("║   Commands: 'quit' to exit, 'skip' to skip optional phases,      ║");
        out.println("║             'back' to return to previous phase                   ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
    }

    // ==================== Phase 1: Seed Input ====================

    private boolean runSeedInputPhase() throws Exception {
        printPhaseHeader("SEED INPUT", 1, 8, "Provide initial topics to explore");

        // Get domain name
        out.println("What domain or subject area is this wiki about?");
        out.println();
        out.println("Examples:");
        out.println("  • Apache Kafka");
        out.println("  • Cloud Native Development");
        out.println("  • Machine Learning Operations");
        out.println();

        InputResponse domainResponse = input.promptRequired("Domain name");
        if (domainResponse.isQuit()) {
            return cancelSession();
        }
        String domainName = domainResponse.value();
        if (domainName == null || domainName.isBlank()) {
            out.println("Domain name is required.");
            return runSeedInputPhase();
        }

        session = new DiscoverySession(domainName);
        out.println();

        // Get domain description
        InputResponse descResponse = input.promptOptional(
                "Brief description of what this wiki will cover", null);
        if (descResponse.isQuit()) {
            return cancelSession();
        }
        if (descResponse.hasValue() && !descResponse.value().isBlank()) {
            session.setDomainDescription(descResponse.value());
        }
        out.println();

        // Get seed topics
        out.println("Enter your initial seed topics (one per line, empty line to finish):");
        out.println("These are the core topics you definitely want to cover.");
        out.println();

        List<String[]> seeds = new ArrayList<>();
        int seedNum = 1;
        while (true) {
            InputResponse topicResponse = input.promptOptional(
                    "Seed topic " + seedNum + " (or press Enter to finish)", null);
            if (topicResponse.isQuit()) {
                return cancelSession();
            }
            String topicName = topicResponse.value();
            if (topicName == null || topicName.isBlank()) {
                break;
            }

            InputResponse descriptionResponse = input.promptOptional("  Brief description", "");
            String topicDesc = descriptionResponse.getValueOrDefault("");

            seeds.add(new String[]{topicName, topicDesc});
            seedNum++;
        }

        if (seeds.isEmpty()) {
            out.println("\nAt least one seed topic is required.");
            return runSeedInputPhase();
        }

        // Mark one as landing page
        out.println();
        out.println("Which topic should be the main landing page?");
        List<String> topicNames = seeds.stream().map(s -> s[0]).toList();
        InputResponse landingResponse = input.promptSelection("Selection", topicNames, 0);
        if (landingResponse.isQuit()) {
            return cancelSession();
        }
        int landingIndex = Integer.parseInt(landingResponse.value());

        // Add topics to session
        for (int i = 0; i < seeds.size(); i++) {
            String[] seed = seeds.get(i);
            if (i == landingIndex) {
                session.addLandingPage(seed[0], seed[1]);
            } else {
                session.addSeedTopic(seed[0], seed[1]);
            }
        }

        out.println();
        out.printf("✓ Created domain '%s' with %d seed topics%n", domainName, seeds.size());
        session.advancePhase();
        return true;
    }

    // ==================== Phase 2: Scope Setup ====================

    private boolean runScopeSetupPhase() throws Exception {
        printPhaseHeader("SCOPE SETUP", 2, 8, "Define boundaries and assumptions (optional)");

        out.println("Would you like to configure scope boundaries?");
        out.println("This helps the AI generate more relevant suggestions.");
        out.println();

        InputResponse configureResponse = input.promptWithNavigationAndDefault(
                "Configure scope? [Y/n/skip]", "y");
        if (configureResponse.isQuit()) {
            return cancelSession();
        }
        if (configureResponse.isSkip() || "n".equalsIgnoreCase(configureResponse.value())) {
            out.println("\n→ Skipping scope configuration.");
            session.advancePhase();
            return true;
        }
        if (configureResponse.isBack()) {
            session.goToPhase(DiscoveryPhase.SEED_INPUT);
            return runSeedInputPhase();
        }

        ScopeConfiguration.Builder scopeBuilder = ScopeConfiguration.builder();

        // Assumed knowledge
        out.println();
        out.println("What knowledge should readers already have? (comma-separated)");
        out.println("These topics won't be covered in detail.");
        out.println();
        out.println("Examples: Java programming, basic SQL, command line familiarity");

        InputResponse assumedResponse = input.promptOptional("Assumed knowledge", null);
        if (assumedResponse.hasValue()) {
            Arrays.stream(assumedResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addAssumedKnowledge);
        }

        // Out of scope
        out.println();
        out.println("What topics should be explicitly excluded? (comma-separated)");

        InputResponse excludeResponse = input.promptOptional("Out of scope", null);
        if (excludeResponse.hasValue()) {
            Arrays.stream(excludeResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addOutOfScope);
        }

        // Focus areas
        out.println();
        out.println("Any specific areas to prioritize? (comma-separated)");

        InputResponse focusResponse = input.promptOptional("Focus areas", null);
        if (focusResponse.hasValue()) {
            Arrays.stream(focusResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addFocusArea);
        }

        // Target audience
        out.println();
        InputResponse audienceResponse = input.promptOptional("Target audience description", null);
        if (audienceResponse.hasValue()) {
            scopeBuilder.audienceDescription(audienceResponse.value());
        }

        session.configureScope(scopeBuilder.build());
        out.println("\n✓ Scope configured.");
        session.advancePhase();
        return true;
    }

    // ==================== Phase 3: Topic Expansion ====================

    private boolean runTopicExpansionPhase() throws Exception {
        printPhaseHeader("TOPIC EXPANSION", 3, 8, "Discover and curate related topics");

        TopicUniverse current = session.buildUniverse();
        Set<String> existingNames = current.topics().stream()
                .map(Topic::name)
                .collect(Collectors.toSet());

        out.println("Generating topic suggestions based on your seed topics...");
        out.println();

        // Generate suggestions for each accepted topic
        int expansionRounds = 0;
        int maxRounds = 3;

        while (expansionRounds < maxRounds) {
            List<Topic> toExpand = session.buildUniverse().getAcceptedTopics().stream()
                    .limit(3) // Expand up to 3 topics per round
                    .toList();

            if (toExpand.isEmpty()) break;

            for (Topic topic : toExpand) {
                session.setExpansionSource(topic.name());

                out.println("━".repeat(60));
                out.printf("Expanding from: %s%n", topic.name());
                out.println("━".repeat(60));

                List<TopicSuggestion> suggestions = topicExpander.expandTopic(
                        topic.name(),
                        session.getDomainName(),
                        existingNames,
                        session.getScope()
                );

                if (suggestions.isEmpty()) {
                    out.println("No new suggestions generated.");
                    continue;
                }

                session.addTopicSuggestions(suggestions);

                // Present suggestions for curation
                if (!curateTopicSuggestions()) {
                    if (readLine() != null && readLine().equalsIgnoreCase("quit")) {
                        return cancelSession();
                    }
                }

                // Update existing names
                existingNames = session.buildUniverse().topics().stream()
                        .map(Topic::name)
                        .collect(Collectors.toSet());
            }

            expansionRounds++;

            // Ask if user wants to continue expanding
            out.println();
            out.printf("Completed expansion round %d. Current topics: %d%n",
                    expansionRounds, session.getAcceptedTopicCount());

            InputResponse continueResponse = input.promptYesNo("Continue expanding?", true);
            if (continueResponse.isQuit() || "no".equals(continueResponse.value())) {
                break;
            }
        }

        out.println();
        out.printf("✓ Topic expansion complete. Total topics: %d%n", session.getAcceptedTopicCount());
        session.advancePhase();
        return true;
    }

    private boolean curateTopicSuggestions() throws Exception {
        List<TopicSuggestion> suggestions = session.getPendingTopicSuggestions();

        if (suggestions.isEmpty()) {
            return true;
        }

        out.println();
        out.printf("Found %d topic suggestions. Review each one:%n", suggestions.size());
        out.println();

        for (int i = 0; i < suggestions.size(); i++) {
            TopicSuggestion suggestion = suggestions.get(i);

            displayTopicSuggestion(suggestion, i + 1, suggestions.size());

            out.println();
            out.println("  " + topicCurationFactory.getMenuPrompt());
            out.print("  Decision: ");
            out.flush();

            String decision = readLine();
            CurationAction action = CurationAction.parse(decision);

            if (action == CurationAction.QUIT) {
                return false;
            }

            // Get remaining suggestions for skip rest command
            List<TopicSuggestion> remaining = suggestions.subList(i + 1, suggestions.size());
            CurationCommand<TopicSuggestion> command = topicCurationFactory.getCommand(action, remaining);

            if (command != null) {
                CurationResult result = command.execute(suggestion, session, input);
                if (result.shouldQuit()) {
                    return false;
                }
                if (result.message() != null) {
                    out.println("  " + getResultIcon(result) + " " + result.message());
                }
                if (result.shouldStop()) {
                    return true;
                }
            }
            out.println();
        }

        session.clearPendingTopicSuggestions();
        return true;
    }

    private void displayTopicSuggestion(TopicSuggestion suggestion, int current, int total) {
        out.println("┌" + "─".repeat(58) + "┐");
        out.printf("│ %d/%d: %s%n", current, total, suggestion.name());
        out.println("├" + "─".repeat(58) + "┤");
        out.printf("│ %s%n", suggestion.description());
        out.printf("│ Category: %-15s  Relevance: %s%n",
                suggestion.category(),
                suggestion.getRelevanceIndicator());
        out.printf("│ Type: %-18s  Complexity: %s%n",
                suggestion.suggestedContentType().getDisplayName(),
                suggestion.suggestedComplexity().getDisplayName());
        if (!suggestion.rationale().isBlank()) {
            out.printf("│ Why: %s%n", truncate(suggestion.rationale(), 50));
        }
        out.println("└" + "─".repeat(58) + "┘");
    }

    private String getResultIcon(CurationResult result) {
        return switch (result.outcome()) {
            case SUCCESS -> "✓";
            case SKIPPED -> "→";
            case MODIFIED -> "✓";
            case ERROR -> "✗";
        };
    }

    // ==================== Phase 4: Relationship Mapping ====================

    private boolean runRelationshipMappingPhase() throws Exception {
        printPhaseHeader("RELATIONSHIP MAPPING", 4, 8, "Define how topics relate to each other");

        List<Topic> topics = session.buildUniverse().getAcceptedTopics();
        if (topics.size() < 2) {
            out.println("Need at least 2 topics for relationship mapping.");
            session.advancePhase();
            return true;
        }

        out.println("Analyzing relationships between your " + topics.size() + " topics...");
        out.println();

        List<RelationshipSuggestion> suggestions = relationshipSuggester.analyzeAllRelationships(topics);

        if (suggestions.isEmpty()) {
            out.println("No relationship suggestions generated.");
            session.advancePhase();
            return true;
        }

        session.addRelationshipSuggestions(suggestions);
        out.printf("Found %d potential relationships. Review the important ones:%n", suggestions.size());
        out.println();

        // Show and curate relationship suggestions
        List<RelationshipSuggestion> pending = session.getPendingRelationshipSuggestions();
        int shown = 0;
        int maxToShow = 15;

        for (RelationshipSuggestion rel : pending) {
            if (shown >= maxToShow) {
                out.println();
                out.printf("Showing %d of %d relationships.%n", shown, pending.size());
                InputResponse moreResponse = input.promptYesNo("Show more?", false);
                if (moreResponse.isQuit() || "no".equals(moreResponse.value())) {
                    // Auto-confirm remaining high-confidence
                    autoProcessRemainingRelationships(pending);
                    break;
                }
                maxToShow += 10;
            }

            displayRelationshipSuggestion(rel);
            out.print("  " + relationshipCurationFactory.getMenuPrompt() + ": ");
            out.flush();

            String decision = readLine();
            CurationAction action = CurationAction.parse(decision);

            if (action == CurationAction.QUIT) {
                return cancelSession();
            }

            CurationCommand<RelationshipSuggestion> command = relationshipCurationFactory.getCommand(action);
            if (command != null) {
                CurationResult result = command.execute(rel, session, input);
                if (result.shouldQuit()) {
                    return cancelSession();
                }
                if (result.message() != null) {
                    out.println("  " + getResultIcon(result) + " " + result.message());
                }
            }
            shown++;
        }

        session.clearPendingRelationshipSuggestions();
        out.println();
        out.printf("✓ Relationship mapping complete. %d relationships confirmed.%n",
                session.buildUniverse().relationships().size());
        session.advancePhase();
        return true;
    }

    private void displayRelationshipSuggestion(RelationshipSuggestion rel) {
        out.println(rel.toDisplayString());
        if (!rel.rationale().isBlank()) {
            out.println("  └─ " + truncate(rel.rationale(), 60));
        }
    }

    private void autoProcessRemainingRelationships(List<RelationshipSuggestion> pending) {
        for (RelationshipSuggestion remaining : pending) {
            if (remaining.isHighConfidence()) {
                session.confirmRelationship(remaining);
            } else {
                session.rejectRelationship(remaining);
            }
        }
    }

    // ==================== Phase 5: Gap Analysis ====================

    private boolean runGapAnalysisPhase() throws Exception {
        printPhaseHeader("GAP ANALYSIS", 5, 8, "Identify missing topics and connections");

        TopicUniverse universe = session.buildUniverse();
        out.println("Analyzing topic coverage for gaps...");
        out.println();

        GapAnalyzer.GapAnalysisResult result = gapAnalyzer.analyzeGaps(
                universe.topics(),
                universe.relationships(),
                universe.scope()
        );

        if (!result.hasGaps()) {
            out.println("✓ No significant gaps identified.");
            session.advancePhase();
            return true;
        }

        // Show assessment summary
        if (result.assessment() != null) {
            GapAnalyzer.OverallAssessment assessment = result.assessment();
            out.println("Coverage Assessment:");
            out.printf("  Coverage:      %s%.0f%%%n", getScoreBar(assessment.coverageScore()), assessment.coverageScore() * 100);
            out.printf("  Balance:       %s%.0f%%%n", getScoreBar(assessment.balanceScore()), assessment.balanceScore() * 100);
            out.printf("  Connectedness: %s%.0f%%%n", getScoreBar(assessment.connectednessScore()), assessment.connectednessScore() * 100);
            if (!assessment.summary().isBlank()) {
                out.println();
                out.println("Summary: " + assessment.summary());
            }
            out.println();
        }

        // Show gaps
        out.printf("Found %d gaps to review:%n", result.gaps().size());
        out.println();

        for (GapAnalyzer.Gap gap : result.gaps()) {
            String severityIcon = switch (gap.severity()) {
                case CRITICAL -> "🔴";
                case MODERATE -> "🟡";
                case MINOR -> "🟢";
            };

            out.printf("%s [%s] %s%n", severityIcon, gap.type(), gap.description());
            out.println("   Resolution: " + gap.suggestedResolution());

            if (gap.hasSuggestedTopic()) {
                out.println();
                out.print("   Add suggested topic '" + gap.suggestedTopicName() + "'? [Y/n]: ");
                out.flush();

                String decision = readLine();
                if (decision == null || decision.equalsIgnoreCase("quit")) {
                    return cancelSession();
                }

                if (decision.isBlank() || decision.toLowerCase().startsWith("y")) {
                    Topic newTopic = Topic.builder(gap.suggestedTopicName())
                            .description(gap.suggestedResolution())
                            .status(TopicStatus.ACCEPTED)
                            .priority(Priority.SHOULD_HAVE)
                            .addedReason("Gap analysis: " + gap.description())
                            .build();
                    session.addressGapWithTopic(gap.description(), newTopic);
                    out.println("   ✓ Topic added");
                } else {
                    session.ignoreGap(gap.description());
                    out.println("   → Skipped");
                }
            } else {
                session.addGaps(List.of(gap.description()));
            }
            out.println();
        }

        session.clearPendingGaps();
        out.println("✓ Gap analysis complete.");
        session.advancePhase();
        return true;
    }

    // ==================== Phase 6: Depth Calibration ====================

    private boolean runDepthCalibrationPhase() throws Exception {
        printPhaseHeader("DEPTH CALIBRATION", 6, 8, "Set word counts and detail levels (optional)");

        out.println("Would you like to adjust topic depths?");

        InputResponse calibrateResponse = input.promptWithNavigationAndDefault(
                "Calibrate depths? [y/N/skip]", "n");
        if (calibrateResponse.isQuit()) {
            return cancelSession();
        }
        if (calibrateResponse.isSkip() || "n".equalsIgnoreCase(calibrateResponse.value())
                || (calibrateResponse.value() != null && calibrateResponse.value().isBlank())) {
            out.println("\n→ Using default depths.");
            session.advancePhase();
            return true;
        }
        if (calibrateResponse.isBack()) {
            session.goToPhase(DiscoveryPhase.GAP_ANALYSIS);
            return runGapAnalysisPhase();
        }

        List<Topic> topics = session.buildUniverse().getAcceptedTopics();
        out.println();
        out.println("Current topics and suggested word counts:");
        out.println();

        for (int i = 0; i < topics.size(); i++) {
            Topic topic = topics.get(i);
            int suggested = topic.complexity().getMinWords();
            out.printf("  %2d. %-30s %s (%d words)%n",
                    i + 1,
                    truncate(topic.name(), 30),
                    topic.complexity().getDisplayName(),
                    suggested);
        }

        out.println();
        out.println("Enter topic number to adjust, or press Enter to finish:");

        while (true) {
            InputResponse numResponse = input.promptOptional("Topic #", null);
            if (numResponse.isQuit()) {
                return cancelSession();
            }
            String numInput = numResponse.value();
            if (numInput == null || numInput.isBlank()) {
                break;
            }

            try {
                int idx = Integer.parseInt(numInput.trim()) - 1;
                if (idx >= 0 && idx < topics.size()) {
                    Topic topic = topics.get(idx);
                    out.printf("  Current: %s (%d words)%n", topic.complexity().getDisplayName(), topic.estimatedWords());

                    InputResponse wordResponse = input.promptInteger(
                            "  New word count", topic.estimatedWords(), 100, 10000);
                    if (wordResponse.hasValue()) {
                        int newWords = Integer.parseInt(wordResponse.value());
                        session.updateTopicDepth(topic.id(), newWords);
                        out.println("  ✓ Updated");
                    }
                }
            } catch (NumberFormatException e) {
                out.println("  Invalid input");
            }
        }

        out.println("\n✓ Depth calibration complete.");
        session.advancePhase();
        return true;
    }

    // ==================== Phase 7: Prioritization ====================

    private boolean runPrioritizationPhase() throws Exception {
        printPhaseHeader("PRIORITIZATION", 7, 8, "Assign generation priorities");

        List<Topic> topics = session.buildUniverse().getAcceptedTopics();

        out.println("Review topic priorities:");
        out.println();
        out.println("  Priority levels:");
        out.println("    1. MUST_HAVE   - Essential, generate first");
        out.println("    2. SHOULD_HAVE - Important, generate second");
        out.println("    3. NICE_TO_HAVE - Optional, generate if time permits");
        out.println("    4. BACKLOG     - Future consideration");
        out.println();

        // Group by priority
        Map<Priority, List<Topic>> byPriority = topics.stream()
                .collect(Collectors.groupingBy(Topic::priority));

        for (Priority priority : Priority.values()) {
            List<Topic> group = byPriority.getOrDefault(priority, List.of());
            if (!group.isEmpty()) {
                out.printf("  %s (%d topics):%n", priority.getDisplayName(), group.size());
                for (Topic topic : group) {
                    out.printf("    • %s%n", topic.name());
                }
                out.println();
            }
        }

        InputResponse adjustResponse = input.promptYesNo("Adjust priorities?", false);
        if (adjustResponse.isQuit()) {
            return cancelSession();
        }
        if ("yes".equals(adjustResponse.value())) {
            out.println();
            out.println("Enter topic name and new priority (e.g., 'Kafka Producers 1'):");
            out.println("Press Enter when done.");

            while (true) {
                InputResponse lineResponse = input.promptOptional(">", null);
                if (lineResponse.isQuit()) {
                    return cancelSession();
                }
                String line = lineResponse.value();
                if (line == null || line.isBlank()) {
                    break;
                }

                // Parse "Topic Name N"
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        int priorityNum = Integer.parseInt(parts[parts.length - 1]);
                        String topicName = String.join(" ", Arrays.copyOf(parts, parts.length - 1));

                        Priority newPriority = switch (priorityNum) {
                            case 1 -> Priority.MUST_HAVE;
                            case 2 -> Priority.SHOULD_HAVE;
                            case 3 -> Priority.NICE_TO_HAVE;
                            case 4 -> Priority.BACKLOG;
                            default -> null;
                        };

                        if (newPriority != null) {
                            session.buildUniverse().getTopicByName(topicName)
                                    .ifPresent(t -> session.updateTopicPriority(t.id(), newPriority));
                            out.println("  ✓ Updated");
                        }
                    } catch (NumberFormatException e) {
                        out.println("  Invalid format");
                    }
                }
            }
        }

        out.println("\n✓ Prioritization complete.");
        session.advancePhase();
        return true;
    }

    // ==================== Phase 8: Review ====================

    private boolean runReviewPhase() throws Exception {
        printPhaseHeader("REVIEW", 8, 8, "Review and finalize the topic universe");

        TopicUniverse universe = session.buildUniverse();

        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║                        DISCOVERY SUMMARY                          ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
        out.printf("  Domain:        %s%n", universe.name());
        out.printf("  Topics:        %d accepted%n", universe.getAcceptedCount());
        out.printf("  Relationships: %d mapped%n", universe.relationships().size());
        out.printf("  Backlog:       %d items%n", universe.backlog().size());
        out.println();

        // Show generation order preview
        List<Topic> order = universe.getGenerationOrder();
        out.println("  Suggested generation order:");
        int shown = Math.min(10, order.size());
        for (int i = 0; i < shown; i++) {
            Topic topic = order.get(i);
            out.printf("    %2d. %s [%s]%n", i + 1, topic.name(), topic.priority().getDisplayName());
        }
        if (order.size() > shown) {
            out.printf("    ... and %d more%n", order.size() - shown);
        }

        out.println();
        out.println("─".repeat(67));
        out.println();

        InputResponse finalizeResponse = input.promptYesNo("Finalize this topic universe?", true);
        if (finalizeResponse.isQuit()) {
            return cancelSession();
        }

        if ("yes".equals(finalizeResponse.value())) {
            out.println("\n✓ Topic universe finalized!");
            out.println();
            out.printf("Session ID: %s%n", session.getSessionId());
            out.printf("You can now generate content for these %d topics.%n", universe.getAcceptedCount());
            return true;
        } else {
            out.println("\n→ Going back to prioritization.");
            session.goToPhase(DiscoveryPhase.PRIORITIZATION);
            return runPrioritizationPhase();
        }
    }

    // ==================== Helper Methods ====================

    private void printPhaseHeader(String name, int current, int total, String description) {
        out.println();
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.printf("│  PHASE %d OF %d: %-47s │%n", current, total, name);
        out.println("├─────────────────────────────────────────────────────────────────┤");
        out.printf("│  %-63s │%n", description);
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();
    }

    private String readLine() throws Exception {
        return in.readLine();
    }

    private boolean cancelSession() {
        out.println("\n✗ Session cancelled.");
        return false;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private String getScoreBar(double score) {
        int filled = (int) (score * 10);
        return "█".repeat(filled) + "░".repeat(10 - filled) + " ";
    }

    /**
     * Get the discovery session for testing/access.
     */
    public DiscoverySession getSession() {
        return session;
    }
}
