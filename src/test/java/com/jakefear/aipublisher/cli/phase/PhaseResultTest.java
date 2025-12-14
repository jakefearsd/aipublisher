package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhaseResult record.
 */
@DisplayName("PhaseResult")
class PhaseResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("continueToNext creates success result")
        void continueToNextCreatesSuccess() {
            PhaseResult result = PhaseResult.continueToNext();

            assertTrue(result.isSuccess());
            assertFalse(result.isCancelled());
            assertNull(result.nextPhase());
            assertFalse(result.hasNextPhase());
        }

        @Test
        @DisplayName("goTo creates result with specific next phase")
        void goToCreatesWithNextPhase() {
            PhaseResult result = PhaseResult.goTo(DiscoveryPhase.SEED_INPUT);

            assertTrue(result.isSuccess());
            assertFalse(result.isCancelled());
            assertEquals(DiscoveryPhase.SEED_INPUT, result.nextPhase());
            assertTrue(result.hasNextPhase());
        }

        @Test
        @DisplayName("cancel creates cancelled result")
        void cancelCreatesCancelledResult() {
            PhaseResult result = PhaseResult.cancel();

            assertFalse(result.isSuccess());
            assertTrue(result.isCancelled());
            assertNull(result.nextPhase());
            assertFalse(result.hasNextPhase());
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("hasNextPhase returns true when nextPhase is set")
        void hasNextPhaseReturnsTrue() {
            PhaseResult result = new PhaseResult(true, DiscoveryPhase.REVIEW, false);
            assertTrue(result.hasNextPhase());
        }

        @Test
        @DisplayName("hasNextPhase returns false when nextPhase is null")
        void hasNextPhaseReturnsFalse() {
            PhaseResult result = new PhaseResult(true, null, false);
            assertFalse(result.hasNextPhase());
        }
    }
}
