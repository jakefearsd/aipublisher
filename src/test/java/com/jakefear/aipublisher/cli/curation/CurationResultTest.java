package com.jakefear.aipublisher.cli.curation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CurationResult record.
 */
@DisplayName("CurationResult")
class CurationResultTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("success creates result with SUCCESS outcome")
        void successCreatesSuccessOutcome() {
            CurationResult result = CurationResult.success("Test message");

            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Test message", result.message());
            assertTrue(result.shouldContinue());
            assertFalse(result.shouldQuit());
            assertFalse(result.shouldStop());
        }

        @Test
        @DisplayName("skipRest creates result that stops continuation")
        void skipRestStopsContinuation() {
            CurationResult result = CurationResult.skipRest("Skipped rest");

            assertEquals(CurationResult.Outcome.SKIPPED, result.outcome());
            assertEquals("Skipped rest", result.message());
            assertFalse(result.shouldContinue());
            assertFalse(result.shouldQuit());
            assertTrue(result.shouldStop());
        }

        @Test
        @DisplayName("quit creates result with shouldQuit true")
        void quitSetsShouldQuit() {
            CurationResult result = CurationResult.quit();

            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertNull(result.message());
            assertFalse(result.shouldContinue());
            assertTrue(result.shouldQuit());
            assertTrue(result.shouldStop());
        }

        @Test
        @DisplayName("modified creates result with MODIFIED outcome")
        void modifiedCreatesModifiedOutcome() {
            CurationResult result = CurationResult.modified("Modified item");

            assertEquals(CurationResult.Outcome.MODIFIED, result.outcome());
            assertEquals("Modified item", result.message());
            assertTrue(result.shouldContinue());
            assertFalse(result.shouldQuit());
            assertFalse(result.shouldStop());
        }
    }

    @Nested
    @DisplayName("shouldStop")
    class ShouldStop {

        @Test
        @DisplayName("Returns true when shouldContinue is false")
        void returnsTrueWhenContinueFalse() {
            CurationResult result = new CurationResult(
                    CurationResult.Outcome.SUCCESS, "msg", false, false);
            assertTrue(result.shouldStop());
        }

        @Test
        @DisplayName("Returns true when shouldQuit is true")
        void returnsTrueWhenQuitTrue() {
            CurationResult result = new CurationResult(
                    CurationResult.Outcome.SUCCESS, "msg", true, true);
            assertTrue(result.shouldStop());
        }

        @Test
        @DisplayName("Returns false when shouldContinue true and shouldQuit false")
        void returnsFalseWhenContinuingAndNotQuitting() {
            CurationResult result = new CurationResult(
                    CurationResult.Outcome.SUCCESS, "msg", true, false);
            assertFalse(result.shouldStop());
        }
    }

    @Nested
    @DisplayName("Outcome enum")
    class OutcomeTest {

        @Test
        @DisplayName("Has all expected values")
        void hasAllExpectedValues() {
            CurationResult.Outcome[] outcomes = CurationResult.Outcome.values();
            assertEquals(4, outcomes.length);
            assertNotNull(CurationResult.Outcome.valueOf("SUCCESS"));
            assertNotNull(CurationResult.Outcome.valueOf("SKIPPED"));
            assertNotNull(CurationResult.Outcome.valueOf("MODIFIED"));
            assertNotNull(CurationResult.Outcome.valueOf("ERROR"));
        }
    }
}
