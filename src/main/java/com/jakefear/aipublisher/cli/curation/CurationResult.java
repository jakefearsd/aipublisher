package com.jakefear.aipublisher.cli.curation;

/**
 * Result of executing a curation command.
 *
 * @param outcome What happened as a result
 * @param message Display message for the user
 * @param shouldContinue Whether to continue processing more items
 * @param shouldQuit Whether the user requested to quit entirely
 */
public record CurationResult(
        Outcome outcome,
        String message,
        boolean shouldContinue,
        boolean shouldQuit
) {
    public enum Outcome {
        SUCCESS,
        SKIPPED,
        MODIFIED,
        ERROR
    }

    /**
     * Create a success result.
     */
    public static CurationResult success(String message) {
        return new CurationResult(Outcome.SUCCESS, message, true, false);
    }

    /**
     * Create a result that skips remaining items.
     */
    public static CurationResult skipRest(String message) {
        return new CurationResult(Outcome.SKIPPED, message, false, false);
    }

    /**
     * Create a quit result.
     */
    public static CurationResult quit() {
        return new CurationResult(Outcome.SUCCESS, null, false, true);
    }

    /**
     * Create a modification result.
     */
    public static CurationResult modified(String message) {
        return new CurationResult(Outcome.MODIFIED, message, true, false);
    }

    /**
     * Check if curation should stop (either skip rest or quit).
     */
    public boolean shouldStop() {
        return !shouldContinue || shouldQuit;
    }
}
