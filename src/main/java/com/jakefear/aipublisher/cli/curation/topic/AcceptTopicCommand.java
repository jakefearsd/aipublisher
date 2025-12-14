package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.TopicSuggestion;

/**
 * Command to accept a topic suggestion as-is.
 */
public class AcceptTopicCommand implements CurationCommand<TopicSuggestion> {

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.acceptTopicSuggestion(suggestion);
        return CurationResult.success("Accepted");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.ACCEPT;
    }
}
