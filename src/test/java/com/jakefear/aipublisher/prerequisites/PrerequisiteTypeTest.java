package com.jakefear.aipublisher.prerequisites;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrerequisiteType enum.
 */
@DisplayName("PrerequisiteType")
class PrerequisiteTypeTest {

    @Test
    @DisplayName("HARD is prominent")
    void hardIsProminent() {
        assertTrue(PrerequisiteType.HARD.isProminent());
        assertFalse(PrerequisiteType.SOFT.isProminent());
        assertFalse(PrerequisiteType.ASSUMED.isProminent());
    }

    @Test
    @DisplayName("HARD and SOFT should link")
    void hardAndSoftShouldLink() {
        assertTrue(PrerequisiteType.HARD.shouldLink());
        assertTrue(PrerequisiteType.SOFT.shouldLink());
        assertFalse(PrerequisiteType.ASSUMED.shouldLink());
    }

    @Test
    @DisplayName("All types have display names and descriptions")
    void allTypesHaveDisplayNamesAndDescriptions() {
        for (PrerequisiteType type : PrerequisiteType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isBlank());
            assertNotNull(type.getDescription());
            assertFalse(type.getDescription().isBlank());
        }
    }
}
