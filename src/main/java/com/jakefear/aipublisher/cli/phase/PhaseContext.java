package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.cli.curation.relationship.RelationshipCurationCommandFactory;
import com.jakefear.aipublisher.cli.curation.topic.TopicCurationCommandFactory;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.GapAnalyzer;
import com.jakefear.aipublisher.discovery.RelationshipSuggester;
import com.jakefear.aipublisher.discovery.TopicExpander;

import java.io.PrintWriter;

/**
 * Context object providing dependencies to phase handlers.
 * Encapsulates all the services and state needed during phase execution.
 */
public class PhaseContext {

    private final ConsoleInputHelper input;
    private final PrintWriter out;
    private DiscoverySession session;
    private final TopicExpander topicExpander;
    private final RelationshipSuggester relationshipSuggester;
    private final GapAnalyzer gapAnalyzer;
    private final TopicCurationCommandFactory topicCurationFactory;
    private final RelationshipCurationCommandFactory relationshipCurationFactory;

    public PhaseContext(
            ConsoleInputHelper input,
            PrintWriter out,
            DiscoverySession session,
            TopicExpander topicExpander,
            RelationshipSuggester relationshipSuggester,
            GapAnalyzer gapAnalyzer,
            TopicCurationCommandFactory topicCurationFactory,
            RelationshipCurationCommandFactory relationshipCurationFactory) {
        this.input = input;
        this.out = out;
        this.session = session;
        this.topicExpander = topicExpander;
        this.relationshipSuggester = relationshipSuggester;
        this.gapAnalyzer = gapAnalyzer;
        this.topicCurationFactory = topicCurationFactory;
        this.relationshipCurationFactory = relationshipCurationFactory;
    }

    public ConsoleInputHelper input() {
        return input;
    }

    public PrintWriter out() {
        return out;
    }

    public DiscoverySession session() {
        return session;
    }

    /**
     * Set the discovery session. Used during the seed input phase.
     */
    public void setSession(DiscoverySession session) {
        this.session = session;
    }

    public TopicExpander topicExpander() {
        return topicExpander;
    }

    public RelationshipSuggester relationshipSuggester() {
        return relationshipSuggester;
    }

    public GapAnalyzer gapAnalyzer() {
        return gapAnalyzer;
    }

    public TopicCurationCommandFactory topicCurationFactory() {
        return topicCurationFactory;
    }

    public RelationshipCurationCommandFactory relationshipCurationFactory() {
        return relationshipCurationFactory;
    }

    /**
     * Print a phase header.
     */
    public void printPhaseHeader(String name, int current, int total, String description) {
        out.println();
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.printf("│  PHASE %d OF %d: %-47s │%n", current, total, name);
        out.println("├─────────────────────────────────────────────────────────────────┤");
        out.printf("│  %-63s │%n", description);
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();
    }

    /**
     * Print the session cancelled message.
     */
    public void printCancelled() {
        out.println("\n✗ Session cancelled.");
    }

    /**
     * Truncate text to max length.
     */
    public String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    /**
     * Get a score bar visualization.
     */
    public String getScoreBar(double score) {
        int filled = (int) (score * 10);
        return "█".repeat(filled) + "░".repeat(10 - filled) + " ";
    }
}
