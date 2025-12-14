package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import com.jakefear.aipublisher.discovery.DiscoverySession;

/**
 * Handler interface for a discovery phase.
 * Each phase of the interactive discovery session implements this interface.
 * Part of the State pattern for managing discovery workflow.
 */
public interface PhaseHandler {

    /**
     * Get the phase this handler manages.
     */
    DiscoveryPhase getPhase();

    /**
     * Execute this phase.
     *
     * @param context The phase execution context
     * @return The result indicating success/failure and next phase
     * @throws Exception if the phase execution fails
     */
    PhaseResult execute(PhaseContext context) throws Exception;

    /**
     * Get the phase number (1-8).
     */
    default int getPhaseNumber() {
        return getPhase().ordinal() + 1;
    }

    /**
     * Check if this phase is optional (can be skipped).
     */
    default boolean isOptional() {
        return false;
    }
}
