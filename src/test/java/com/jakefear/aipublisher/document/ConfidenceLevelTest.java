package com.jakefear.aipublisher.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConfidenceLevel")
class ConfidenceLevelTest {

    @Nested
    @DisplayName("Ordering")
    class Ordering {

        @Test
        @DisplayName("LOW has lowest value")
        void lowHasLowestValue() {
            assertEquals(1, ConfidenceLevel.LOW.getValue());
        }

        @Test
        @DisplayName("MEDIUM has middle value")
        void mediumHasMiddleValue() {
            assertEquals(2, ConfidenceLevel.MEDIUM.getValue());
        }

        @Test
        @DisplayName("HIGH has highest value")
        void highHasHighestValue() {
            assertEquals(3, ConfidenceLevel.HIGH.getValue());
        }
    }

    @Nested
    @DisplayName("Minimum Comparison")
    class MinimumComparison {

        @Test
        @DisplayName("HIGH meets all minimums")
        void highMeetsAllMinimums() {
            assertTrue(ConfidenceLevel.HIGH.meetsMinimum(ConfidenceLevel.LOW));
            assertTrue(ConfidenceLevel.HIGH.meetsMinimum(ConfidenceLevel.MEDIUM));
            assertTrue(ConfidenceLevel.HIGH.meetsMinimum(ConfidenceLevel.HIGH));
        }

        @Test
        @DisplayName("MEDIUM meets LOW and MEDIUM minimums")
        void mediumMeetsLowAndMedium() {
            assertTrue(ConfidenceLevel.MEDIUM.meetsMinimum(ConfidenceLevel.LOW));
            assertTrue(ConfidenceLevel.MEDIUM.meetsMinimum(ConfidenceLevel.MEDIUM));
            assertFalse(ConfidenceLevel.MEDIUM.meetsMinimum(ConfidenceLevel.HIGH));
        }

        @Test
        @DisplayName("LOW only meets LOW minimum")
        void lowOnlyMeetsLow() {
            assertTrue(ConfidenceLevel.LOW.meetsMinimum(ConfidenceLevel.LOW));
            assertFalse(ConfidenceLevel.LOW.meetsMinimum(ConfidenceLevel.MEDIUM));
            assertFalse(ConfidenceLevel.LOW.meetsMinimum(ConfidenceLevel.HIGH));
        }
    }

    @Nested
    @DisplayName("String Parsing")
    class StringParsing {

        @ParameterizedTest
        @ValueSource(strings = {"HIGH", "high", "High", "  HIGH  "})
        @DisplayName("Parses HIGH variations")
        void parsesHighVariations(String input) {
            assertEquals(ConfidenceLevel.HIGH, ConfidenceLevel.fromString(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"MEDIUM", "medium", "Medium", "  medium  "})
        @DisplayName("Parses MEDIUM variations")
        void parsesMediumVariations(String input) {
            assertEquals(ConfidenceLevel.MEDIUM, ConfidenceLevel.fromString(input));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "invalid", "UNKNOWN"})
        @DisplayName("Defaults to LOW for invalid values")
        void defaultsToLowForInvalid(String input) {
            assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.fromString(input));
        }

        @ParameterizedTest
        @ValueSource(strings = {"LOW", "low", "Low"})
        @DisplayName("Parses LOW variations")
        void parsesLowVariations(String input) {
            assertEquals(ConfidenceLevel.LOW, ConfidenceLevel.fromString(input));
        }
    }
}
