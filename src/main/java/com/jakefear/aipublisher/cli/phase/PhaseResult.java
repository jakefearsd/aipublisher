package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.discovery.DiscoveryPhase;

/**
 * Result of executing a discovery phase.
 *
 * @param success Whether the phase completed successfully
 * @param nextPhase Optional: If set, go to this phase instead of the normal next phase
 * @param cancelled Whether the user cancelled the session
 */
public record PhaseResult(
        boolean success,
        DiscoveryPhase nextPhase,
        boolean cancelled
) {
    /**
     * Create a success result, proceeding to the next phase normally.
     */
    public static PhaseResult continueToNext() {
        return new PhaseResult(true, null, false);
    }

    /**
     * Create a result that goes to a specific phase.
     */
    public static PhaseResult goTo(DiscoveryPhase phase) {
        return new PhaseResult(true, phase, false);
    }

    /**
     * Create a cancelled result.
     */
    public static PhaseResult cancel() {
        return new PhaseResult(false, null, true);
    }

    /**
     * Check if the phase was successful.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Check if the user cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Check if there's a specific phase to go to.
     */
    public boolean hasNextPhase() {
        return nextPhase != null;
    }
}
