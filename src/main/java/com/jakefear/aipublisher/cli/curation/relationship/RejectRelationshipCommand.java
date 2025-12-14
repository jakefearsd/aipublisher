package com.jakefear.aipublisher.cli.curation.relationship;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.RelationshipSuggestion;

/**
 * Command to reject a relationship suggestion.
 */
public class RejectRelationshipCommand implements CurationCommand<RelationshipSuggestion> {

    @Override
    public CurationResult execute(RelationshipSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) {
        session.rejectRelationship(suggestion);
        return CurationResult.success("Rejected");
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.REJECT;
    }
}
