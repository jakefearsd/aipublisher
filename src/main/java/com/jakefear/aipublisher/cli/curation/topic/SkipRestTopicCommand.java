package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.TopicSuggestion;

import java.util.List;

/**
 * Command to skip remaining topic suggestions.
 * Accepts high-relevance topics and defers low-relevance ones.
 */
public class SkipRestTopicCommand implements CurationCommand<TopicSuggestion> {

    private static final double HIGH_RELEVANCE_THRESHOLD = 0.7;

    private final List<TopicSuggestion> remainingSuggestions;

    public SkipRestTopicCommand(List<TopicSuggestion> remainingSuggestions) {
        this.remainingSuggestions = remainingSuggestions;
    }

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        // Process current suggestion first
        processBasedOnRelevance(suggestion, session);

        // Then process all remaining suggestions
        for (TopicSuggestion remaining : remainingSuggestions) {
            processBasedOnRelevance(remaining, session);
        }

        return CurationResult.skipRest("Accepted high-relevance, deferred others");
    }

    private void processBasedOnRelevance(TopicSuggestion suggestion, DiscoverySession session) {
        if (suggestion.relevanceScore() >= HIGH_RELEVANCE_THRESHOLD) {
            session.acceptTopicSuggestion(suggestion);
        } else {
            session.deferTopicSuggestion(suggestion);
        }
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.SKIP_REST;
    }
}
