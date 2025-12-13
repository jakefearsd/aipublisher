package com.jakefear.aipublisher.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExampleType enum.
 */
@DisplayName("ExampleType")
class ExampleTypeTest {

    @Test
    @DisplayName("ANTI_PATTERN is negative example")
    void antiPatternIsNegativeExample() {
        assertTrue(ExampleType.ANTI_PATTERN.isNegativeExample());
        assertFalse(ExampleType.MINIMAL.isNegativeExample());
        assertFalse(ExampleType.REALISTIC.isNegativeExample());
    }

    @Test
    @DisplayName("PROGRESSIVE is progressive")
    void progressiveIsProgressive() {
        assertTrue(ExampleType.PROGRESSIVE.isProgressive());
        assertFalse(ExampleType.MINIMAL.isProgressive());
        assertFalse(ExampleType.REALISTIC.isProgressive());
    }

    @Test
    @DisplayName("COMPLETE and REALISTIC require full context")
    void completeAndRealisticRequireFullContext() {
        assertTrue(ExampleType.COMPLETE.requiresFullContext());
        assertTrue(ExampleType.REALISTIC.requiresFullContext());
        assertFalse(ExampleType.MINIMAL.requiresFullContext());
        assertFalse(ExampleType.ANTI_PATTERN.requiresFullContext());
    }

    @Test
    @DisplayName("All types have display names and descriptions")
    void allTypesHaveDisplayNamesAndDescriptions() {
        for (ExampleType type : ExampleType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isBlank());
            assertNotNull(type.getDescription());
            assertFalse(type.getDescription().isBlank());
        }
    }
}
