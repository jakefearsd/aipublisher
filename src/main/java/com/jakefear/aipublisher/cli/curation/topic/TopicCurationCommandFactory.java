package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.discovery.TopicSuggestion;

import java.util.List;

/**
 * Factory for creating topic curation commands based on user action.
 */
public class TopicCurationCommandFactory {

    private final AcceptTopicCommand acceptCommand = new AcceptTopicCommand();
    private final RejectTopicCommand rejectCommand = new RejectTopicCommand();
    private final DeferTopicCommand deferCommand = new DeferTopicCommand();
    private final ModifyTopicCommand modifyCommand = new ModifyTopicCommand();

    /**
     * Get a command for the given action.
     *
     * @param action The user's action
     * @param remainingSuggestions Remaining suggestions (for skip rest)
     * @return The command, or null for quit/unrecognized
     */
    public CurationCommand<TopicSuggestion> getCommand(
            CurationAction action,
            List<TopicSuggestion> remainingSuggestions) {

        return switch (action) {
            case ACCEPT, DEFAULT -> acceptCommand;
            case REJECT -> rejectCommand;
            case DEFER -> deferCommand;
            case MODIFY -> modifyCommand;
            case SKIP_REST -> new SkipRestTopicCommand(remainingSuggestions);
            default -> null;
        };
    }

    /**
     * Get the menu prompt for topic curation.
     */
    public String getMenuPrompt() {
        return "[A]ccept  [R]eject  [D]efer  [M]odify  [S]kip rest  [Q]uit";
    }
}
