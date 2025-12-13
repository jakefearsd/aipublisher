package com.jakefear.aipublisher.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SourceReliability")
class SourceReliabilityTest {

    @Nested
    @DisplayName("isTrustworthy")
    class IsTrustworthy {

        @Test
        @DisplayName("OFFICIAL is trustworthy")
        void officialIsTrustworthy() {
            assertTrue(SourceReliability.OFFICIAL.isTrustworthy());
        }

        @Test
        @DisplayName("ACADEMIC is trustworthy")
        void academicIsTrustworthy() {
            assertTrue(SourceReliability.ACADEMIC.isTrustworthy());
        }

        @Test
        @DisplayName("AUTHORITATIVE is trustworthy")
        void authoritativeIsTrustworthy() {
            assertTrue(SourceReliability.AUTHORITATIVE.isTrustworthy());
        }

        @Test
        @DisplayName("REPUTABLE is not trustworthy")
        void reputableIsNotTrustworthy() {
            assertFalse(SourceReliability.REPUTABLE.isTrustworthy());
        }

        @Test
        @DisplayName("COMMUNITY is not trustworthy")
        void communityIsNotTrustworthy() {
            assertFalse(SourceReliability.COMMUNITY.isTrustworthy());
        }

        @Test
        @DisplayName("UNCERTAIN is not trustworthy")
        void uncertainIsNotTrustworthy() {
            assertFalse(SourceReliability.UNCERTAIN.isTrustworthy());
        }
    }

    @Nested
    @DisplayName("getScore")
    class GetScore {

        @Test
        @DisplayName("OFFICIAL has highest score (1.0)")
        void officialHasHighestScore() {
            assertEquals(1.0, SourceReliability.OFFICIAL.getScore());
        }

        @Test
        @DisplayName("ACADEMIC has second highest score (0.95)")
        void academicHasSecondHighestScore() {
            assertEquals(0.95, SourceReliability.ACADEMIC.getScore());
        }

        @Test
        @DisplayName("AUTHORITATIVE has 0.85 score")
        void authoritativeScore() {
            assertEquals(0.85, SourceReliability.AUTHORITATIVE.getScore());
        }

        @Test
        @DisplayName("REPUTABLE has 0.7 score")
        void reputableScore() {
            assertEquals(0.7, SourceReliability.REPUTABLE.getScore());
        }

        @Test
        @DisplayName("COMMUNITY has 0.5 score")
        void communityScore() {
            assertEquals(0.5, SourceReliability.COMMUNITY.getScore());
        }

        @Test
        @DisplayName("UNCERTAIN has lowest score (0.3)")
        void uncertainHasLowestScore() {
            assertEquals(0.3, SourceReliability.UNCERTAIN.getScore());
        }

        @ParameterizedTest
        @EnumSource(SourceReliability.class)
        @DisplayName("All scores are between 0 and 1")
        void scoresInValidRange(SourceReliability reliability) {
            double score = reliability.getScore();
            assertTrue(score >= 0.0 && score <= 1.0,
                    "Score " + score + " for " + reliability + " should be between 0 and 1");
        }
    }

    @Nested
    @DisplayName("getDescription")
    class GetDescription {

        @ParameterizedTest
        @EnumSource(SourceReliability.class)
        @DisplayName("All reliability levels have descriptions")
        void allHaveDescriptions(SourceReliability reliability) {
            assertNotNull(reliability.getDescription());
            assertFalse(reliability.getDescription().isBlank());
        }
    }
}
