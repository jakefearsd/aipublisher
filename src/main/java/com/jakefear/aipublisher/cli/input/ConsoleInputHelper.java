package com.jakefear.aipublisher.cli.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

/**
 * Helper class for handling console input in interactive CLI sessions.
 * <p>
 * Centralizes common input patterns:
 * - Prompting with optional defaults
 * - Handling navigation commands (quit, back, skip)
 * - Selection from numbered lists
 * - Yes/No confirmations
 * <p>
 * This reduces cyclomatic complexity in CLI session classes by extracting
 * repetitive input validation and parsing logic.
 */
public class ConsoleInputHelper {

    private static final Set<String> QUIT_COMMANDS = Set.of("quit", "q", "exit");
    private static final Set<String> BACK_COMMANDS = Set.of("back", "b");
    private static final Set<String> SKIP_COMMANDS = Set.of("skip", "s");

    private final BufferedReader in;
    private final PrintWriter out;

    public ConsoleInputHelper(BufferedReader in, PrintWriter out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Read a raw line from input.
     *
     * @return The line read, or null if EOF
     * @throws IOException if reading fails
     */
    public String readLine() throws IOException {
        return in.readLine();
    }

    /**
     * Prompt for required input with no default.
     * Recognizes quit command.
     *
     * @param prompt The prompt to display (without colon)
     * @return Response with VALUE or QUIT
     * @throws IOException if reading fails
     */
    public InputResponse promptRequired(String prompt) throws IOException {
        out.print(prompt + ": ");
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }

        return InputResponse.value(input.trim());
    }

    /**
     * Prompt for optional input with a default value.
     * Recognizes quit command.
     *
     * @param prompt The prompt to display (without colon/brackets)
     * @param defaultValue The default value shown in brackets
     * @return Response with VALUE (possibly default) or QUIT
     * @throws IOException if reading fails
     */
    public InputResponse promptOptional(String prompt, String defaultValue) throws IOException {
        if (defaultValue != null && !defaultValue.isEmpty()) {
            out.print(prompt + " [" + defaultValue + "]: ");
        } else {
            out.print(prompt + ": ");
        }
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }

        String value = input.isBlank() ? defaultValue : input.trim();
        return InputResponse.value(value);
    }

    /**
     * Prompt with full navigation support (quit, back, skip).
     *
     * @param prompt The prompt to display
     * @return Response with VALUE, QUIT, BACK, or SKIP
     * @throws IOException if reading fails
     */
    public InputResponse promptWithNavigation(String prompt) throws IOException {
        out.print(prompt + ": ");
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }
        if (isBackCommand(input)) {
            return InputResponse.back();
        }
        if (isSkipCommand(input)) {
            return InputResponse.skip();
        }

        return InputResponse.value(input.trim());
    }

    /**
     * Prompt with navigation and a default value.
     *
     * @param prompt The prompt to display
     * @param defaultValue Default shown in brackets
     * @return Response with result and value
     * @throws IOException if reading fails
     */
    public InputResponse promptWithNavigationAndDefault(String prompt, String defaultValue)
            throws IOException {
        String fullPrompt = defaultValue != null && !defaultValue.isEmpty()
                ? prompt + " [" + defaultValue + "]"
                : prompt;
        out.print(fullPrompt + ": ");
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }
        if (isBackCommand(input)) {
            return InputResponse.back();
        }
        if (isSkipCommand(input)) {
            return InputResponse.skip();
        }

        String value = input.isBlank() ? defaultValue : input.trim();
        return InputResponse.value(value);
    }

    /**
     * Prompt for a yes/no confirmation.
     *
     * @param prompt The question to ask
     * @param defaultYes If true, Enter defaults to yes; if false, defaults to no
     * @return Response with "yes" or "no" as value, or QUIT
     * @throws IOException if reading fails
     */
    public InputResponse promptYesNo(String prompt, boolean defaultYes) throws IOException {
        String suffix = defaultYes ? " [Y/n]" : " [y/N]";
        out.print(prompt + suffix + ": ");
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }

        boolean isYes;
        if (input.isBlank()) {
            isYes = defaultYes;
        } else {
            isYes = input.trim().toLowerCase().startsWith("y");
        }

        return InputResponse.value(isYes ? "yes" : "no");
    }

    /**
     * Prompt for selection from a numbered list.
     *
     * @param prompt The prompt to display
     * @param options List of option labels
     * @param defaultIndex Default selection (0-based), or -1 for no default
     * @return Response with selected index as string, or navigation
     * @throws IOException if reading fails
     */
    public InputResponse promptSelection(String prompt, List<String> options, int defaultIndex)
            throws IOException {
        // Display options
        for (int i = 0; i < options.size(); i++) {
            String marker = (i == defaultIndex) ? " ← default" : "";
            out.printf("  %d. %s%s%n", i + 1, options.get(i), marker);
        }
        out.println();

        String defaultStr = (defaultIndex >= 0 && defaultIndex < options.size())
                ? String.valueOf(defaultIndex + 1)
                : null;

        if (defaultStr != null) {
            out.print(prompt + " [" + defaultStr + "]: ");
        } else {
            out.print(prompt + ": ");
        }
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }

        if (input.isBlank() && defaultIndex >= 0) {
            return InputResponse.value(String.valueOf(defaultIndex));
        }

        try {
            int selection = Integer.parseInt(input.trim()) - 1;
            if (selection >= 0 && selection < options.size()) {
                return InputResponse.value(String.valueOf(selection));
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }

        // Invalid input - return default or -1
        return InputResponse.value(String.valueOf(defaultIndex >= 0 ? defaultIndex : -1));
    }

    /**
     * Prompt for an integer value.
     *
     * @param prompt The prompt to display
     * @param defaultValue Default value, or null for no default
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Response with integer as string, or navigation
     * @throws IOException if reading fails
     */
    public InputResponse promptInteger(String prompt, Integer defaultValue, int min, int max)
            throws IOException {
        String defaultStr = defaultValue != null ? String.valueOf(defaultValue) : null;

        if (defaultStr != null) {
            out.print(prompt + " [" + defaultStr + "]: ");
        } else {
            out.print(prompt + ": ");
        }
        out.flush();

        String input = readLine();
        if (input == null || isQuitCommand(input)) {
            return InputResponse.quit();
        }

        if (input.isBlank() && defaultValue != null) {
            return InputResponse.value(String.valueOf(defaultValue));
        }

        try {
            int value = Integer.parseInt(input.trim());
            if (value < min) {
                out.println("Minimum is " + min + ". Using minimum.");
                value = min;
            } else if (value > max) {
                out.println("Maximum is " + max + ". Using maximum.");
                value = max;
            }
            return InputResponse.value(String.valueOf(value));
        } catch (NumberFormatException e) {
            if (defaultValue != null) {
                out.println("Invalid number. Using default: " + defaultValue);
                return InputResponse.value(String.valueOf(defaultValue));
            }
            return InputResponse.value(null);
        }
    }

    /**
     * Print a message to output.
     */
    public void println(String message) {
        out.println(message);
    }

    /**
     * Print an empty line.
     */
    public void println() {
        out.println();
    }

    /**
     * Print a formatted message.
     */
    public void printf(String format, Object... args) {
        out.printf(format, args);
    }

    /**
     * Flush output.
     */
    public void flush() {
        out.flush();
    }

    // Command detection helpers

    private boolean isQuitCommand(String input) {
        return QUIT_COMMANDS.contains(input.trim().toLowerCase());
    }

    private boolean isBackCommand(String input) {
        return BACK_COMMANDS.contains(input.trim().toLowerCase());
    }

    private boolean isSkipCommand(String input) {
        return SKIP_COMMANDS.contains(input.trim().toLowerCase());
    }
}
