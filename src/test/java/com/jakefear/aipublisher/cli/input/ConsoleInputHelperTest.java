package com.jakefear.aipublisher.cli.input;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ConsoleInputHelper.
 */
@DisplayName("ConsoleInputHelper")
class ConsoleInputHelperTest {

    private StringWriter outputBuffer;
    private PrintWriter out;

    @BeforeEach
    void setUp() {
        outputBuffer = new StringWriter();
        out = new PrintWriter(outputBuffer);
    }

    private ConsoleInputHelper createHelper(String... inputs) {
        String input = String.join("\n", inputs) + "\n";
        BufferedReader in = new BufferedReader(new StringReader(input));
        return new ConsoleInputHelper(in, out);
    }

    @Nested
    @DisplayName("promptRequired")
    class PromptRequired {

        @Test
        @DisplayName("Returns value when user enters text")
        void returnsValueWhenEntered() throws Exception {
            ConsoleInputHelper helper = createHelper("test value");
            InputResponse response = helper.promptRequired("Enter name");

            assertEquals(InputResult.VALUE, response.result());
            assertEquals("test value", response.value());
        }

        @Test
        @DisplayName("Returns QUIT when user enters quit")
        void returnsQuitWhenQuit() throws Exception {
            ConsoleInputHelper helper = createHelper("quit");
            InputResponse response = helper.promptRequired("Enter name");

            assertEquals(InputResult.QUIT, response.result());
        }

        @Test
        @DisplayName("Returns QUIT for various quit commands")
        void recognizesQuitVariations() throws Exception {
            for (String cmd : List.of("quit", "QUIT", "q", "Q", "exit", "EXIT")) {
                ConsoleInputHelper helper = createHelper(cmd);
                InputResponse response = helper.promptRequired("Enter name");
                assertEquals(InputResult.QUIT, response.result(),
                        "Should recognize '" + cmd + "' as quit");
            }
        }

        @Test
        @DisplayName("Displays prompt with colon")
        void displaysPromptWithColon() throws Exception {
            ConsoleInputHelper helper = createHelper("test");
            helper.promptRequired("Enter name");

            assertTrue(outputBuffer.toString().contains("Enter name:"));
        }
    }

    @Nested
    @DisplayName("promptOptional")
    class PromptOptional {

        @Test
        @DisplayName("Returns value when user enters text")
        void returnsValueWhenEntered() throws Exception {
            ConsoleInputHelper helper = createHelper("custom value");
            InputResponse response = helper.promptOptional("Enter name", "default");

            assertEquals("custom value", response.value());
        }

        @Test
        @DisplayName("Returns default when user presses Enter")
        void returnsDefaultWhenEmpty() throws Exception {
            ConsoleInputHelper helper = createHelper("");
            InputResponse response = helper.promptOptional("Enter name", "default value");

            assertEquals("default value", response.value());
        }

        @Test
        @DisplayName("Displays default in brackets")
        void displaysDefaultInBrackets() throws Exception {
            ConsoleInputHelper helper = createHelper("test");
            helper.promptOptional("Enter name", "mydefault");

            assertTrue(outputBuffer.toString().contains("[mydefault]"));
        }
    }

    @Nested
    @DisplayName("promptWithNavigation")
    class PromptWithNavigation {

        @Test
        @DisplayName("Returns BACK when user enters back")
        void returnsBackWhenBack() throws Exception {
            for (String cmd : List.of("back", "BACK", "b", "B")) {
                ConsoleInputHelper helper = createHelper(cmd);
                InputResponse response = helper.promptWithNavigation("Choose");
                assertEquals(InputResult.BACK, response.result(),
                        "Should recognize '" + cmd + "' as back");
            }
        }

        @Test
        @DisplayName("Returns SKIP when user enters skip")
        void returnsSkipWhenSkip() throws Exception {
            for (String cmd : List.of("skip", "SKIP", "s", "S")) {
                ConsoleInputHelper helper = createHelper(cmd);
                InputResponse response = helper.promptWithNavigation("Choose");
                assertEquals(InputResult.SKIP, response.result(),
                        "Should recognize '" + cmd + "' as skip");
            }
        }

        @Test
        @DisplayName("Returns VALUE for regular input")
        void returnsValueForRegularInput() throws Exception {
            ConsoleInputHelper helper = createHelper("something");
            InputResponse response = helper.promptWithNavigation("Choose");

            assertEquals(InputResult.VALUE, response.result());
            assertEquals("something", response.value());
        }
    }

    @Nested
    @DisplayName("promptYesNo")
    class PromptYesNo {

        @Test
        @DisplayName("Returns yes when user enters y")
        void returnsYesForY() throws Exception {
            for (String input : List.of("y", "Y", "yes", "YES", "Yeah")) {
                ConsoleInputHelper helper = createHelper(input);
                InputResponse response = helper.promptYesNo("Continue?", false);
                assertEquals("yes", response.value(),
                        "Should interpret '" + input + "' as yes");
            }
        }

        @Test
        @DisplayName("Returns no when user enters n")
        void returnsNoForN() throws Exception {
            for (String input : List.of("n", "N", "no", "NO")) {
                ConsoleInputHelper helper = createHelper(input);
                InputResponse response = helper.promptYesNo("Continue?", true);
                assertEquals("no", response.value(),
                        "Should interpret '" + input + "' as no");
            }
        }

        @Test
        @DisplayName("Returns default yes when empty and defaultYes is true")
        void returnsDefaultYes() throws Exception {
            ConsoleInputHelper helper = createHelper("");
            InputResponse response = helper.promptYesNo("Continue?", true);

            assertEquals("yes", response.value());
        }

        @Test
        @DisplayName("Returns default no when empty and defaultYes is false")
        void returnsDefaultNo() throws Exception {
            ConsoleInputHelper helper = createHelper("");
            InputResponse response = helper.promptYesNo("Continue?", false);

            assertEquals("no", response.value());
        }

        @Test
        @DisplayName("Displays [Y/n] when defaultYes is true")
        void displaysYnForDefaultYes() throws Exception {
            ConsoleInputHelper helper = createHelper("y");
            helper.promptYesNo("Continue?", true);

            assertTrue(outputBuffer.toString().contains("[Y/n]"));
        }

        @Test
        @DisplayName("Displays [y/N] when defaultYes is false")
        void displaysyNForDefaultNo() throws Exception {
            ConsoleInputHelper helper = createHelper("n");
            helper.promptYesNo("Continue?", false);

            assertTrue(outputBuffer.toString().contains("[y/N]"));
        }
    }

    @Nested
    @DisplayName("promptSelection")
    class PromptSelection {

        @Test
        @DisplayName("Returns selected index")
        void returnsSelectedIndex() throws Exception {
            ConsoleInputHelper helper = createHelper("2");
            InputResponse response = helper.promptSelection(
                    "Select", List.of("Option A", "Option B", "Option C"), 0);

            assertEquals("1", response.value()); // 0-based index
        }

        @Test
        @DisplayName("Returns default when empty")
        void returnsDefaultWhenEmpty() throws Exception {
            ConsoleInputHelper helper = createHelper("");
            InputResponse response = helper.promptSelection(
                    "Select", List.of("Option A", "Option B"), 1);

            assertEquals("1", response.value());
        }

        @Test
        @DisplayName("Displays options with numbers")
        void displaysOptionsWithNumbers() throws Exception {
            ConsoleInputHelper helper = createHelper("1");
            helper.promptSelection("Select", List.of("Alpha", "Beta"), 0);

            String output = outputBuffer.toString();
            assertTrue(output.contains("1. Alpha"));
            assertTrue(output.contains("2. Beta"));
        }

        @Test
        @DisplayName("Marks default option")
        void marksDefaultOption() throws Exception {
            ConsoleInputHelper helper = createHelper("1");
            helper.promptSelection("Select", List.of("Alpha", "Beta"), 1);

            assertTrue(outputBuffer.toString().contains("Beta") &&
                    outputBuffer.toString().contains("default"));
        }
    }

    @Nested
    @DisplayName("promptInteger")
    class PromptInteger {

        @Test
        @DisplayName("Returns valid integer")
        void returnsValidInteger() throws Exception {
            ConsoleInputHelper helper = createHelper("42");
            InputResponse response = helper.promptInteger("Count", null, 1, 100);

            assertEquals("42", response.value());
        }

        @Test
        @DisplayName("Returns default when empty")
        void returnsDefaultWhenEmpty() throws Exception {
            ConsoleInputHelper helper = createHelper("");
            InputResponse response = helper.promptInteger("Count", 25, 1, 100);

            assertEquals("25", response.value());
        }

        @Test
        @DisplayName("Clamps to minimum")
        void clampsToMinimum() throws Exception {
            ConsoleInputHelper helper = createHelper("5");
            InputResponse response = helper.promptInteger("Count", null, 10, 100);

            assertEquals("10", response.value());
            assertTrue(outputBuffer.toString().contains("Minimum is 10"));
        }

        @Test
        @DisplayName("Clamps to maximum")
        void clampsToMaximum() throws Exception {
            ConsoleInputHelper helper = createHelper("500");
            InputResponse response = helper.promptInteger("Count", null, 1, 100);

            assertEquals("100", response.value());
            assertTrue(outputBuffer.toString().contains("Maximum is 100"));
        }

        @Test
        @DisplayName("Uses default for invalid input")
        void usesDefaultForInvalidInput() throws Exception {
            ConsoleInputHelper helper = createHelper("abc");
            InputResponse response = helper.promptInteger("Count", 50, 1, 100);

            assertEquals("50", response.value());
            assertTrue(outputBuffer.toString().contains("Invalid number"));
        }
    }

    @Nested
    @DisplayName("InputResponse")
    class InputResponseTests {

        @Test
        @DisplayName("hasValue returns true for VALUE with content")
        void hasValueReturnsTrue() {
            InputResponse response = InputResponse.value("test");
            assertTrue(response.hasValue());
        }

        @Test
        @DisplayName("hasValue returns false for QUIT")
        void hasValueReturnsFalseForQuit() {
            InputResponse response = InputResponse.quit();
            assertFalse(response.hasValue());
        }

        @Test
        @DisplayName("isNavigation returns true for non-VALUE")
        void isNavigationReturnsTrueForNonValue() {
            assertTrue(InputResponse.quit().isNavigation());
            assertTrue(InputResponse.back().isNavigation());
            assertTrue(InputResponse.skip().isNavigation());
        }

        @Test
        @DisplayName("isNavigation returns false for VALUE")
        void isNavigationReturnsFalseForValue() {
            assertFalse(InputResponse.value("test").isNavigation());
        }

        @Test
        @DisplayName("getValueOrDefault returns value when present")
        void getValueOrDefaultReturnsValue() {
            InputResponse response = InputResponse.value("actual");
            assertEquals("actual", response.getValueOrDefault("default"));
        }

        @Test
        @DisplayName("getValueOrDefault returns default when blank")
        void getValueOrDefaultReturnsDefaultWhenBlank() {
            InputResponse response = InputResponse.value("  ");
            assertEquals("default", response.getValueOrDefault("default"));
        }
    }
}
