package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.domain.ComplexityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CostProfile record.
 */
@DisplayName("CostProfile")
class CostProfileTest {

    @Nested
    @DisplayName("Predefined Profiles")
    class PredefinedProfiles {

        @Test
        @DisplayName("MINIMAL has correct settings")
        void minimalHasCorrectSettings() {
            CostProfile minimal = CostProfile.MINIMAL;

            assertEquals("Minimal", minimal.name());
            assertEquals(1, minimal.maxExpansionRounds());
            assertEquals(2, minimal.topicsPerRound());
            assertEquals(4, minimal.suggestionsPerTopic());
            assertEquals(ComplexityLevel.INTERMEDIATE, minimal.maxComplexity());
            assertEquals(0.6, minimal.wordCountMultiplier(), 0.001);
            assertTrue(minimal.skipGapAnalysis());
            assertEquals(RelationshipDepth.CORE_ONLY, minimal.relationshipDepth());
            assertEquals(0.9, minimal.autoAcceptThreshold(), 0.001);
        }

        @Test
        @DisplayName("BALANCED has correct settings")
        void balancedHasCorrectSettings() {
            CostProfile balanced = CostProfile.BALANCED;

            assertEquals("Balanced", balanced.name());
            assertEquals(3, balanced.maxExpansionRounds());
            assertEquals(3, balanced.topicsPerRound());
            assertEquals(7, balanced.suggestionsPerTopic());
            assertEquals(ComplexityLevel.ADVANCED, balanced.maxComplexity());
            assertEquals(1.0, balanced.wordCountMultiplier(), 0.001);
            assertFalse(balanced.skipGapAnalysis());
            assertEquals(RelationshipDepth.IMPORTANT, balanced.relationshipDepth());
            assertEquals(0.8, balanced.autoAcceptThreshold(), 0.001);
        }

        @Test
        @DisplayName("COMPREHENSIVE has correct settings")
        void comprehensiveHasCorrectSettings() {
            CostProfile comprehensive = CostProfile.COMPREHENSIVE;

            assertEquals("Comprehensive", comprehensive.name());
            assertEquals(5, comprehensive.maxExpansionRounds());
            assertEquals(5, comprehensive.topicsPerRound());
            assertEquals(12, comprehensive.suggestionsPerTopic());
            assertEquals(ComplexityLevel.EXPERT, comprehensive.maxComplexity());
            assertEquals(1.5, comprehensive.wordCountMultiplier(), 0.001);
            assertFalse(comprehensive.skipGapAnalysis());
            assertEquals(RelationshipDepth.ALL, comprehensive.relationshipDepth());
            assertEquals(0.7, comprehensive.autoAcceptThreshold(), 0.001);
        }

        @Test
        @DisplayName("values() returns all three profiles")
        void valuesReturnsAllProfiles() {
            CostProfile[] profiles = CostProfile.values();

            assertEquals(3, profiles.length);
            assertEquals(CostProfile.MINIMAL, profiles[0]);
            assertEquals(CostProfile.BALANCED, profiles[1]);
            assertEquals(CostProfile.COMPREHENSIVE, profiles[2]);
        }
    }

    @Nested
    @DisplayName("fromName lookup")
    class FromNameLookup {

        @ParameterizedTest
        @ValueSource(strings = {"MINIMAL", "minimal", "Minimal", "MIN", "min", "1"})
        @DisplayName("finds MINIMAL profile by various names")
        void findsMinimalByName(String name) {
            assertEquals(CostProfile.MINIMAL, CostProfile.fromName(name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"BALANCED", "balanced", "Balanced", "BAL", "bal", "2"})
        @DisplayName("finds BALANCED profile by various names")
        void findsBalancedByName(String name) {
            assertEquals(CostProfile.BALANCED, CostProfile.fromName(name));
        }

        @ParameterizedTest
        @ValueSource(strings = {"COMPREHENSIVE", "comprehensive", "Comprehensive", "COMP", "comp", "FULL", "full", "3"})
        @DisplayName("finds COMPREHENSIVE profile by various names")
        void findsComprehensiveByName(String name) {
            assertEquals(CostProfile.COMPREHENSIVE, CostProfile.fromName(name));
        }

        @Test
        @DisplayName("returns null for unknown names")
        void returnsNullForUnknown() {
            assertNull(CostProfile.fromName("unknown"));
            assertNull(CostProfile.fromName("4"));
            assertNull(CostProfile.fromName(""));
        }

        @Test
        @DisplayName("returns null for null input")
        void returnsNullForNull() {
            assertNull(CostProfile.fromName(null));
        }

        @Test
        @DisplayName("trims whitespace from name")
        void trimsWhitespace() {
            assertEquals(CostProfile.BALANCED, CostProfile.fromName("  balanced  "));
        }
    }

    @Nested
    @DisplayName("Suggestions Range")
    class SuggestionsRange {

        @Test
        @DisplayName("MINIMAL suggestions range is 2-6")
        void minimalSuggestionsRange() {
            assertEquals("2-6", CostProfile.MINIMAL.getSuggestionsRange());
        }

        @Test
        @DisplayName("BALANCED suggestions range is 5-9")
        void balancedSuggestionsRange() {
            assertEquals("5-9", CostProfile.BALANCED.getSuggestionsRange());
        }

        @Test
        @DisplayName("COMPREHENSIVE suggestions range is 10-14")
        void comprehensiveSuggestionsRange() {
            assertEquals("10-14", CostProfile.COMPREHENSIVE.getSuggestionsRange());
        }
    }

    @Nested
    @DisplayName("Adjusted Word Count")
    class AdjustedWordCount {

        @Test
        @DisplayName("MINIMAL applies 0.6 multiplier and caps at INTERMEDIATE")
        void minimalAdjustsWordCount() {
            // INTERMEDIATE minWords is 800
            assertEquals(480, CostProfile.MINIMAL.getAdjustedWordCount(ComplexityLevel.INTERMEDIATE));

            // Should cap ADVANCED to INTERMEDIATE (800 * 0.6 = 480)
            assertEquals(480, CostProfile.MINIMAL.getAdjustedWordCount(ComplexityLevel.ADVANCED));

            // BEGINNER minWords is 500
            assertEquals(300, CostProfile.MINIMAL.getAdjustedWordCount(ComplexityLevel.BEGINNER));
        }

        @Test
        @DisplayName("BALANCED applies 1.0 multiplier and caps at ADVANCED")
        void balancedAdjustsWordCount() {
            // ADVANCED minWords is 1200
            assertEquals(1200, CostProfile.BALANCED.getAdjustedWordCount(ComplexityLevel.ADVANCED));

            // Should cap EXPERT to ADVANCED (1200 * 1.0 = 1200)
            assertEquals(1200, CostProfile.BALANCED.getAdjustedWordCount(ComplexityLevel.EXPERT));
        }

        @Test
        @DisplayName("COMPREHENSIVE applies 1.5 multiplier and allows EXPERT")
        void comprehensiveAdjustsWordCount() {
            // EXPERT minWords is 1500
            assertEquals(2250, CostProfile.COMPREHENSIVE.getAdjustedWordCount(ComplexityLevel.EXPERT));
        }
    }

    @Nested
    @DisplayName("Estimated Topic Range")
    class EstimatedTopicRange {

        @Test
        @DisplayName("MINIMAL estimates 2-4 topics")
        void minimalEstimates() {
            assertEquals("2-4", CostProfile.MINIMAL.getEstimatedTopicRange());
        }

        @Test
        @DisplayName("BALANCED estimates 9-31 topics")
        void balancedEstimates() {
            assertEquals("9-31", CostProfile.BALANCED.getEstimatedTopicRange());
        }

        @Test
        @DisplayName("COMPREHENSIVE estimates 25-150 topics")
        void comprehensiveEstimates() {
            assertEquals("25-150", CostProfile.COMPREHENSIVE.getEstimatedTopicRange());
        }
    }

    @Nested
    @DisplayName("Cost Estimate")
    class CostEstimate {

        @Test
        @DisplayName("returns appropriate cost ranges")
        void returnsCostRanges() {
            assertTrue(CostProfile.MINIMAL.getCostEstimate().contains("$0.50-2"));
            assertTrue(CostProfile.BALANCED.getCostEstimate().contains("$2-5"));
            assertTrue(CostProfile.COMPREHENSIVE.getCostEstimate().contains("$5-15"));
        }
    }

    @Nested
    @DisplayName("Display Methods")
    class DisplayMethods {

        @Test
        @DisplayName("toDisplayString includes all key info")
        void toDisplayStringIncludesKeyInfo() {
            String display = CostProfile.BALANCED.toDisplayString();

            assertTrue(display.contains("BALANCED"));
            assertTrue(display.contains("topics"));
            assertTrue(display.contains("$"));
        }

        @Test
        @DisplayName("toString returns profile name")
        void toStringReturnsName() {
            assertEquals("Minimal", CostProfile.MINIMAL.toString());
            assertEquals("Balanced", CostProfile.BALANCED.toString());
            assertEquals("Comprehensive", CostProfile.COMPREHENSIVE.toString());
        }
    }
}
