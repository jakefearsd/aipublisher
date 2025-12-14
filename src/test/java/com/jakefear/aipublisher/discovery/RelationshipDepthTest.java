package com.jakefear.aipublisher.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RelationshipDepth enum.
 */
@DisplayName("RelationshipDepth")
class RelationshipDepthTest {

    @Test
    @DisplayName("CORE_ONLY has correct settings")
    void coreOnlyHasCorrectSettings() {
        RelationshipDepth depth = RelationshipDepth.CORE_ONLY;

        assertEquals("Core Only", depth.getDisplayName());
        assertEquals("Only core topic relationships", depth.getDescription());
        assertEquals(0.9, depth.getMinRelevanceThreshold(), 0.001);
    }

    @Test
    @DisplayName("IMPORTANT has correct settings")
    void importantHasCorrectSettings() {
        RelationshipDepth depth = RelationshipDepth.IMPORTANT;

        assertEquals("Important", depth.getDisplayName());
        assertEquals("Relationships for important topics", depth.getDescription());
        assertEquals(0.7, depth.getMinRelevanceThreshold(), 0.001);
    }

    @Test
    @DisplayName("ALL has correct settings")
    void allHasCorrectSettings() {
        RelationshipDepth depth = RelationshipDepth.ALL;

        assertEquals("All", depth.getDisplayName());
        assertEquals("Complete relationship analysis", depth.getDescription());
        assertEquals(0.0, depth.getMinRelevanceThreshold(), 0.001);
    }

    @Test
    @DisplayName("Thresholds are ordered correctly")
    void thresholdsAreOrdered() {
        assertTrue(RelationshipDepth.CORE_ONLY.getMinRelevanceThreshold() >
                   RelationshipDepth.IMPORTANT.getMinRelevanceThreshold());
        assertTrue(RelationshipDepth.IMPORTANT.getMinRelevanceThreshold() >
                   RelationshipDepth.ALL.getMinRelevanceThreshold());
    }
}
