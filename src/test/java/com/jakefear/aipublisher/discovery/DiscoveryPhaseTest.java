package com.jakefear.aipublisher.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiscoveryPhase enum.
 */
@DisplayName("DiscoveryPhase")
class DiscoveryPhaseTest {

    @Nested
    @DisplayName("Phase progression")
    class PhaseProgression {

        @Test
        @DisplayName("next() follows expected workflow order")
        void nextFollowsExpectedOrder() {
            assertEquals(DiscoveryPhase.SCOPE_SETUP, DiscoveryPhase.SEED_INPUT.next());
            assertEquals(DiscoveryPhase.TOPIC_EXPANSION, DiscoveryPhase.SCOPE_SETUP.next());
            assertEquals(DiscoveryPhase.RELATIONSHIP_MAPPING, DiscoveryPhase.TOPIC_EXPANSION.next());
            assertEquals(DiscoveryPhase.GAP_ANALYSIS, DiscoveryPhase.RELATIONSHIP_MAPPING.next());
            assertEquals(DiscoveryPhase.DEPTH_CALIBRATION, DiscoveryPhase.GAP_ANALYSIS.next());
            assertEquals(DiscoveryPhase.PRIORITIZATION, DiscoveryPhase.DEPTH_CALIBRATION.next());
            assertEquals(DiscoveryPhase.REVIEW, DiscoveryPhase.PRIORITIZATION.next());
            assertEquals(DiscoveryPhase.COMPLETE, DiscoveryPhase.REVIEW.next());
        }

        @Test
        @DisplayName("COMPLETE.next() stays at COMPLETE")
        void completeStaysAtComplete() {
            assertEquals(DiscoveryPhase.COMPLETE, DiscoveryPhase.COMPLETE.next());
        }

        @Test
        @DisplayName("previous() reverses workflow order")
        void previousReversesOrder() {
            assertEquals(DiscoveryPhase.SEED_INPUT, DiscoveryPhase.SCOPE_SETUP.previous());
            assertEquals(DiscoveryPhase.SCOPE_SETUP, DiscoveryPhase.TOPIC_EXPANSION.previous());
            assertEquals(DiscoveryPhase.TOPIC_EXPANSION, DiscoveryPhase.RELATIONSHIP_MAPPING.previous());
            assertEquals(DiscoveryPhase.RELATIONSHIP_MAPPING, DiscoveryPhase.GAP_ANALYSIS.previous());
            assertEquals(DiscoveryPhase.GAP_ANALYSIS, DiscoveryPhase.DEPTH_CALIBRATION.previous());
            assertEquals(DiscoveryPhase.DEPTH_CALIBRATION, DiscoveryPhase.PRIORITIZATION.previous());
            assertEquals(DiscoveryPhase.PRIORITIZATION, DiscoveryPhase.REVIEW.previous());
        }

        @Test
        @DisplayName("SEED_INPUT.previous() stays at SEED_INPUT")
        void seedInputStaysAtSeedInput() {
            assertEquals(DiscoveryPhase.SEED_INPUT, DiscoveryPhase.SEED_INPUT.previous());
        }
    }

    @Nested
    @DisplayName("Phase properties")
    class PhaseProperties {

        @Test
        @DisplayName("All phases have display names")
        void allPhasesHaveDisplayNames() {
            for (DiscoveryPhase phase : DiscoveryPhase.values()) {
                assertNotNull(phase.getDisplayName());
                assertFalse(phase.getDisplayName().isBlank());
            }
        }

        @Test
        @DisplayName("All phases have descriptions")
        void allPhasesHaveDescriptions() {
            for (DiscoveryPhase phase : DiscoveryPhase.values()) {
                assertNotNull(phase.getDescription());
                assertFalse(phase.getDescription().isBlank());
            }
        }

        @Test
        @DisplayName("Only SCOPE_SETUP and DEPTH_CALIBRATION are skippable")
        void onlyExpectedPhasesAreSkippable() {
            assertTrue(DiscoveryPhase.SCOPE_SETUP.isSkippable());
            assertTrue(DiscoveryPhase.DEPTH_CALIBRATION.isSkippable());

            assertFalse(DiscoveryPhase.SEED_INPUT.isSkippable());
            assertFalse(DiscoveryPhase.TOPIC_EXPANSION.isSkippable());
            assertFalse(DiscoveryPhase.RELATIONSHIP_MAPPING.isSkippable());
            assertFalse(DiscoveryPhase.GAP_ANALYSIS.isSkippable());
            assertFalse(DiscoveryPhase.PRIORITIZATION.isSkippable());
            assertFalse(DiscoveryPhase.REVIEW.isSkippable());
            assertFalse(DiscoveryPhase.COMPLETE.isSkippable());
        }

        @Test
        @DisplayName("Only COMPLETE is terminal")
        void onlyCompleteIsTerminal() {
            assertTrue(DiscoveryPhase.COMPLETE.isTerminal());

            for (DiscoveryPhase phase : DiscoveryPhase.values()) {
                if (phase != DiscoveryPhase.COMPLETE) {
                    assertFalse(phase.isTerminal(), phase + " should not be terminal");
                }
            }
        }
    }
}
