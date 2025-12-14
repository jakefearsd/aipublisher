package com.jakefear.aipublisher.cli.curation;

import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.discovery.DiscoverySession;

/**
 * Command interface for curation actions.
 * Implements the Command pattern for handling user curation decisions.
 *
 * @param <T> The type of suggestion being curated
 */
public interface CurationCommand<T> {

    /**
     * Execute the curation command.
     *
     * @param suggestion The suggestion being curated
     * @param session The discovery session
     * @param input The console input helper for user interaction
     * @return The result of executing the command
     * @throws Exception if interaction fails
     */
    CurationResult execute(T suggestion, DiscoverySession session, ConsoleInputHelper input)
            throws Exception;

    /**
     * Get the action this command handles.
     */
    CurationAction getAction();
}
