package com.jakefear.aipublisher.cli.curation.relationship;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.RelationshipSuggestion;

/**
 * Command to confirm a relationship suggestion.
 */
public class ConfirmRelationshipCommand implements CurationCommand<RelationshipSuggestion> {

    @Override
    public CurationResult execute(RelationshipSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.confirmRelationship(suggestion);
        return CurationResult.success("Confirmed");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.CONFIRM;
    }
}
