package com.jakefear.aipublisher.cli.curation.relationship;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationCommand;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.RelationshipSuggestion;
import com.jakefear.aipublisher.domain.RelationshipType;

import java.util.Arrays;
import java.util.List;

/**
 * Command to change the relationship type before confirming.
 */
public class ChangeTypeRelationshipCommand implements CurationCommand<RelationshipSuggestion> {

    @Override
    public CurationResult execute(RelationshipSuggestion suggestion, DiscoverySession session,
                                  ConsoleInputHelper input) throws Exception {
        RelationshipType newType = promptForRelationshipType(suggestion, input);
        session.confirmRelationshipAs(suggestion, newType);
        return CurationResult.modified("Confirmed as " + newType);
    }

    private RelationshipType promptForRelationshipType(RelationshipSuggestion current,
                                                        ConsoleInputHelper input) throws Exception {
        input.println();
        input.println("  Select relationship type:");

        RelationshipType[] types = RelationshipType.values();
        List<String> options = Arrays.stream(types)
                .map(t -> t.getDisplayName())
                .toList();

        int defaultIndex = Arrays.asList(types).indexOf(current.suggestedType());
        if (defaultIndex < 0) defaultIndex = 0;

        InputResponse response = input.promptSelection("Selection", options, defaultIndex);
        if (response.isQuit()) {
            return current.suggestedType();
        }

        try {
            int idx = Integer.parseInt(response.value());
            if (idx >= 0 && idx < types.length) {
                return types[idx];
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }

        return current.suggestedType();
    }

    @Override
    public CurationAction getAction() {
        return CurationAction.TYPE_CHANGE;
    }
}
