package com.jakefear.aipublisher.cli.input;

import java.util.Optional;

/**
 * Response from a console input operation.
 * Contains both the result type and the optional value.
 *
 * @param result The type of input received
 * @param value The value if result is VALUE, empty otherwise
 */
public record InputResponse(InputResult result, String value) {

    /**
     * Create a VALUE response with the given value.
     */
    public static InputResponse value(String value) {
        return new InputResponse(InputResult.VALUE, value);
    }

    /**
     * Create a QUIT response.
     */
    public static InputResponse quit() {
        return new InputResponse(InputResult.QUIT, null);
    }

    /**
     * Create a BACK response.
     */
    public static InputResponse back() {
        return new InputResponse(InputResult.BACK, null);
    }

    /**
     * Create a SKIP response.
     */
    public static InputResponse skip() {
        return new InputResponse(InputResult.SKIP, null);
    }

    /**
     * Check if this response has a value.
     */
    public boolean hasValue() {
        return result == InputResult.VALUE && value != null;
    }

    /**
     * Check if this is a navigation command (quit, back, or skip).
     */
    public boolean isNavigation() {
        return result != InputResult.VALUE;
    }

    /**
     * Check if user wants to quit.
     */
    public boolean isQuit() {
        return result == InputResult.QUIT;
    }

    /**
     * Check if user wants to go back.
     */
    public boolean isBack() {
        return result == InputResult.BACK;
    }

    /**
     * Check if user wants to skip.
     */
    public boolean isSkip() {
        return result == InputResult.SKIP;
    }

    /**
     * Get the value as an Optional.
     */
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }

    /**
     * Get the value or a default if not present.
     */
    public String getValueOrDefault(String defaultValue) {
        if (hasValue() && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }
}
