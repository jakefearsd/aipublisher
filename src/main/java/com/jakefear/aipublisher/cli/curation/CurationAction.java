package com.jakefear.aipublisher.cli.curation;

/**
 * Represents a curation action the user can take.
 * Part of the Command pattern for handling suggestion curation.
 */
public enum CurationAction {
    ACCEPT("a", "accept", "Accept"),
    REJECT("r", "reject", "Reject"),
    DEFER("d", "defer", "Defer"),
    MODIFY("m", "modify", "Modify"),
    SKIP_REST("s", "skip", "Skip rest"),
    CONFIRM("c", "confirm", "Confirm"),
    TYPE_CHANGE("t", "type", "Type change"),
    QUIT("q", "quit", "Quit"),
    DEFAULT("", "", "");

    private final String shortKey;
    private final String longKey;
    private final String displayName;

    CurationAction(String shortKey, String longKey, String displayName) {
        this.shortKey = shortKey;
        this.longKey = longKey;
        this.displayName = displayName;
    }

    public String getShortKey() {
        return shortKey;
    }

    public String getLongKey() {
        return longKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse user input into a CurationAction.
     * Returns DEFAULT for empty input or null for unrecognized input.
     */
    public static CurationAction parse(String input) {
        if (input == null) {
            return QUIT;
        }

        String normalized = input.trim().toLowerCase();

        if (normalized.isEmpty()) {
            return DEFAULT;
        }

        for (CurationAction action : values()) {
            if (action.shortKey.equals(normalized) || action.longKey.equals(normalized)) {
                return action;
            }
        }

        // Check for quit specifically
        if ("quit".equals(normalized)) {
            return QUIT;
        }

        return null; // Unrecognized
    }
}
