package com.jakefear.aipublisher.cli.curation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CurationAction enum.
 */
@DisplayName("CurationAction")
class CurationActionTest {

    @Nested
    @DisplayName("parse")
    class Parse {

        @ParameterizedTest
        @CsvSource({
                "a, ACCEPT",
                "accept, ACCEPT",
                "A, ACCEPT",
                "ACCEPT, ACCEPT",
                "r, REJECT",
                "reject, REJECT",
                "R, REJECT",
                "d, DEFER",
                "defer, DEFER",
                "D, DEFER",
                "m, MODIFY",
                "modify, MODIFY",
                "M, MODIFY",
                "s, SKIP_REST",
                "skip, SKIP_REST",
                "S, SKIP_REST",
                "c, CONFIRM",
                "confirm, CONFIRM",
                "C, CONFIRM",
                "t, TYPE_CHANGE",
                "type, TYPE_CHANGE",
                "T, TYPE_CHANGE",
                "q, QUIT",
                "quit, QUIT",
                "Q, QUIT"
        })
        @DisplayName("Parses valid inputs correctly")
        void parsesValidInputs(String input, CurationAction expected) {
            assertEquals(expected, CurationAction.parse(input));
        }

        @Test
        @DisplayName("Returns DEFAULT for empty string")
        void returnsDefaultForEmpty() {
            assertEquals(CurationAction.DEFAULT, CurationAction.parse(""));
        }

        @Test
        @DisplayName("Returns DEFAULT for whitespace only")
        void returnsDefaultForWhitespace() {
            assertEquals(CurationAction.DEFAULT, CurationAction.parse("   "));
        }

        @ParameterizedTest
        @NullSource
        @DisplayName("Returns QUIT for null input")
        void returnsQuitForNull(String input) {
            assertEquals(CurationAction.QUIT, CurationAction.parse(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"x", "unknown", "abc", "123"})
        @DisplayName("Returns null for unrecognized input")
        void returnsNullForUnrecognized(String input) {
            assertNull(CurationAction.parse(input));
        }
    }

    @Nested
    @DisplayName("Properties")
    class Properties {

        @Test
        @DisplayName("ACCEPT has correct keys")
        void acceptHasCorrectKeys() {
            CurationAction action = CurationAction.ACCEPT;
            assertEquals("a", action.getShortKey());
            assertEquals("accept", action.getLongKey());
            assertEquals("Accept", action.getDisplayName());
        }

        @Test
        @DisplayName("REJECT has correct keys")
        void rejectHasCorrectKeys() {
            CurationAction action = CurationAction.REJECT;
            assertEquals("r", action.getShortKey());
            assertEquals("reject", action.getLongKey());
            assertEquals("Reject", action.getDisplayName());
        }

        @Test
        @DisplayName("CONFIRM has correct keys")
        void confirmHasCorrectKeys() {
            CurationAction action = CurationAction.CONFIRM;
            assertEquals("c", action.getShortKey());
            assertEquals("confirm", action.getLongKey());
            assertEquals("Confirm", action.getDisplayName());
        }

        @Test
        @DisplayName("DEFAULT has empty keys")
        void defaultHasEmptyKeys() {
            CurationAction action = CurationAction.DEFAULT;
            assertEquals("", action.getShortKey());
            assertEquals("", action.getLongKey());
            assertEquals("", action.getDisplayName());
        }
    }
}
