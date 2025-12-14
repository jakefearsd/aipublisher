package com.jakefear.aipublisher.cli.input;

/**
 * Result type for console input operations.
 * Indicates what kind of input was received from the user.
 */
public enum InputResult {
    /**
     * User provided a valid value.
     */
    VALUE,

    /**
     * User requested to quit the session.
     */
    QUIT,

    /**
     * User requested to go back to the previous phase.
     */
    BACK,

    /**
     * User requested to skip the current phase.
     */
    SKIP
}
