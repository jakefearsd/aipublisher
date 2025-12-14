package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.TopicSuggestion;

/**
 * Command to defer a topic suggestion to backlog.
 */
public class DeferTopicCommand implements CurationCommand<TopicSuggestion> {

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.deferTopicSuggestion(suggestion);
        return CurationResult.success("Deferred to backlog");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.DEFER;
    }
}
