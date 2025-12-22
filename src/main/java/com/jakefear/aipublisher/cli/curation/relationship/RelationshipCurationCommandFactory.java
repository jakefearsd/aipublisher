package com.jakefear.aipublisher.cli.curation.relationship;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.discovery.RelationshipSuggestion;

/**
 * Factory for creating relationship curation commands based on user action.
 */
public class RelationshipCurationCommandFactory {

    private final ConfirmRelationshipCommand confirmCommand = new ConfirmRelationshipCommand();
    private final RejectRelationshipCommand rejectCommand = new RejectRelationshipCommand();
    private final ChangeTypeRelationshipCommand changeTypeCommand = new ChangeTypeRelationshipCommand();

    /**
     * Get a command for the given action.
     *
     * @param action The user's action
     * @return The command, or null for quit/unrecognized
     */
    public CurationCommand<RelationshipSuggestion> getCommand(CurationAction action) {
        if (action == null) {
            return confirmCommand; // Default to confirm for unrecognized input
        }
        return switch (action) {
            case CONFIRM, DEFAULT -> confirmCommand;
            case REJECT -> rejectCommand;
            case TYPE_CHANGE -> changeTypeCommand;
            default -> null;
        };
    }

    /**
     * Get the menu prompt for relationship curation.
     */
    public String getMenuPrompt() {
        return "[C]onfirm  [R]eject  [T]ype change";
    }
}
