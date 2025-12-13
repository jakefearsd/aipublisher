package com.jakefear.aipublisher.linking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LinkCandidate record.
 */
@DisplayName("LinkCandidate")
class LinkCandidateTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid link candidate")
        void createsValidLinkCandidate() {
            LinkCandidate candidate = new LinkCandidate(
                    "VersionControl",
                    "version control",
                    100,
                    true,
                    "Learn about version control systems",
                    0.8
            );

            assertEquals("VersionControl", candidate.targetPage());
            assertEquals("version control", candidate.anchorText());
            assertEquals(100, candidate.position());
            assertTrue(candidate.firstMention());
            assertEquals("Learn about version control systems", candidate.context());
            assertEquals(0.8, candidate.relevanceScore(), 0.001);
        }

        @Test
        @DisplayName("Rejects null target page")
        void rejectsNullTargetPage() {
            assertThrows(NullPointerException.class, () ->
                    new LinkCandidate(null, "text", 0, true, "", 0.5));
        }

        @Test
        @DisplayName("Rejects blank target page")
        void rejectsBlankTargetPage() {
            assertThrows(IllegalArgumentException.class, () ->
                    new LinkCandidate("  ", "text", 0, true, "", 0.5));
        }

        @Test
        @DisplayName("Rejects null anchor text")
        void rejectsNullAnchorText() {
            assertThrows(NullPointerException.class, () ->
                    new LinkCandidate("Page", null, 0, true, "", 0.5));
        }

        @Test
        @DisplayName("Rejects blank anchor text")
        void rejectsBlankAnchorText() {
            assertThrows(IllegalArgumentException.class, () ->
                    new LinkCandidate("Page", "  ", 0, true, "", 0.5));
        }

        @Test
        @DisplayName("Rejects negative position")
        void rejectsNegativePosition() {
            assertThrows(IllegalArgumentException.class, () ->
                    new LinkCandidate("Page", "text", -1, true, "", 0.5));
        }

        @Test
        @DisplayName("Rejects score below zero")
        void rejectsScoreBelowZero() {
            assertThrows(IllegalArgumentException.class, () ->
                    new LinkCandidate("Page", "text", 0, true, "", -0.1));
        }

        @Test
        @DisplayName("Rejects score above one")
        void rejectsScoreAboveOne() {
            assertThrows(IllegalArgumentException.class, () ->
                    new LinkCandidate("Page", "text", 0, true, "", 1.1));
        }

        @Test
        @DisplayName("Converts null context to empty string")
        void convertsNullContextToEmptyString() {
            LinkCandidate candidate = new LinkCandidate("Page", "text", 0, true, null, 0.5);
            assertEquals("", candidate.context());
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create() makes simple candidate")
        void createMakesSimpleCandidate() {
            LinkCandidate candidate = LinkCandidate.create("TestPage", "test", 50);

            assertEquals("TestPage", candidate.targetPage());
            assertEquals("test", candidate.anchorText());
            assertEquals(50, candidate.position());
            assertTrue(candidate.firstMention());
            assertEquals("", candidate.context());
            assertEquals(0.5, candidate.relevanceScore(), 0.001);
        }

        @Test
        @DisplayName("withFirstMention() creates with high score for first mention")
        void withFirstMentionCreatesWithHighScoreForFirstMention() {
            LinkCandidate candidate = LinkCandidate.withFirstMention("Page", "text", 100, true);

            assertTrue(candidate.firstMention());
            assertEquals(0.8, candidate.relevanceScore(), 0.001);
        }

        @Test
        @DisplayName("withFirstMention() creates with low score for later mention")
        void withFirstMentionCreatesWithLowScoreForLaterMention() {
            LinkCandidate candidate = LinkCandidate.withFirstMention("Page", "text", 100, false);

            assertFalse(candidate.firstMention());
            assertEquals(0.3, candidate.relevanceScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("isHighValue()")
    class IsHighValue {

        @Test
        @DisplayName("Returns true for first mention with high score")
        void returnsTrueForFirstMentionWithHighScore() {
            LinkCandidate candidate = new LinkCandidate("Page", "text", 0, true, "", 0.7);
            assertTrue(candidate.isHighValue());
        }

        @Test
        @DisplayName("Returns false for non-first mention")
        void returnsFalseForNonFirstMention() {
            LinkCandidate candidate = new LinkCandidate("Page", "text", 0, false, "", 0.8);
            assertFalse(candidate.isHighValue());
        }

        @Test
        @DisplayName("Returns false for low score")
        void returnsFalseForLowScore() {
            LinkCandidate candidate = new LinkCandidate("Page", "text", 0, true, "", 0.5);
            assertFalse(candidate.isHighValue());
        }

        @Test
        @DisplayName("Returns true at exact threshold")
        void returnsTrueAtExactThreshold() {
            LinkCandidate candidate = new LinkCandidate("Page", "text", 0, true, "", 0.6);
            assertTrue(candidate.isHighValue());
        }
    }

    @Nested
    @DisplayName("toWikiLink()")
    class ToWikiLink {

        @Test
        @DisplayName("Generates simple link when anchor matches page")
        void generatesSimpleLinkWhenAnchorMatchesPage() {
            LinkCandidate candidate = new LinkCandidate("TestPage", "TestPage", 0, true, "", 0.5);
            assertEquals("[TestPage]", candidate.toWikiLink());
        }

        @Test
        @DisplayName("Generates simple link for case-insensitive match")
        void generatesSimpleLinkForCaseInsensitiveMatch() {
            LinkCandidate candidate = new LinkCandidate("TestPage", "testpage", 0, true, "", 0.5);
            assertEquals("[TestPage]", candidate.toWikiLink());
        }

        @Test
        @DisplayName("Generates simple link when anchor is spaced version")
        void generatesSimpleLinkWhenAnchorIsSpacedVersion() {
            LinkCandidate candidate = new LinkCandidate("VersionControl", "version control", 0, true, "", 0.5);
            assertEquals("[VersionControl]", candidate.toWikiLink());
        }

        @Test
        @DisplayName("Generates piped link when anchor differs from page")
        void generatesPipedLinkWhenAnchorDiffersFromPage() {
            LinkCandidate candidate = new LinkCandidate("VersionControl", "VCS", 0, true, "", 0.5);
            assertEquals("[VCS|VersionControl]", candidate.toWikiLink());
        }

        @Test
        @DisplayName("Generates piped link for different text")
        void generatesPipedLinkForDifferentText() {
            LinkCandidate candidate = new LinkCandidate("GitCommands", "git command line tools", 0, true, "", 0.5);
            assertEquals("[git command line tools|GitCommands]", candidate.toWikiLink());
        }
    }
}
