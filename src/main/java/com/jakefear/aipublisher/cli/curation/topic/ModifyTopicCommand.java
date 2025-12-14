package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.TopicSuggestion;
import com.jakefear.aipublisher.domain.Priority;
import com.jakefear.aipublisher.domain.Topic;
import com.jakefear.aipublisher.domain.TopicStatus;

/**
 * Command to modify a topic suggestion before accepting.
 */
public class ModifyTopicCommand implements CurationCommand<TopicSuggestion> {

    @Override
    public CurationResult execute(TopicSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) throws Exception {
        input.println();
        input.printf("  Current name: %s%n", suggestion.name());

        InputResponse nameResponse = input.promptOptional("  New name", suggestion.name());
        if (nameResponse.isQuit()) {
            return CurationResult.quit();
        }
        String newName = nameResponse.getValueOrDefault(suggestion.name());

        input.printf("  Current description: %s%n", suggestion.description());

        InputResponse descResponse = input.promptOptional("  New description", suggestion.description());
        if (descResponse.isQuit()) {
            return CurationResult.quit();
        }
        String newDesc = descResponse.getValueOrDefault(suggestion.description());

        Topic.Builder builder = Topic.builder(newName)
                .description(newDesc)
                .status(TopicStatus.ACCEPTED)
                .contentType(suggestion.suggestedContentType())
                .complexity(suggestion.suggestedComplexity())
                .priority(Priority.SHOULD_HAVE)
                .category(suggestion.category());

        session.modifyAndAcceptTopic(suggestion, builder);
        return CurationResult.modified("Modified and accepted");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.MODIFY;
    }
}
