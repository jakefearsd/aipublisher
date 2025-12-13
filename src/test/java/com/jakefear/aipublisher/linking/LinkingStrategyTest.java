package com.jakefear.aipublisher.linking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LinkingStrategy.
 */
@DisplayName("LinkingStrategy")
class LinkingStrategyTest {

    @Nested
    @DisplayName("Default values")
    class DefaultValues {

        @Test
        @DisplayName("Has expected default link density")
        void hasExpectedDefaultLinkDensity() {
            LinkingStrategy strategy = new LinkingStrategy();

            assertEquals(0.03, strategy.getMinLinkDensity(), 0.001);
            assertEquals(0.08, strategy.getMaxLinkDensity(), 0.001);
        }

        @Test
        @DisplayName("Has expected default max links")
        void hasExpectedDefaultMaxLinks() {
            LinkingStrategy strategy = new LinkingStrategy();
            assertEquals(20, strategy.getMaxLinksPerArticle());
        }

        @Test
        @DisplayName("Has expected default word spacing")
        void hasExpectedDefaultWordSpacing() {
            LinkingStrategy strategy = new LinkingStrategy();
            assertEquals(50, strategy.getMinWordsBetweenLinks());
        }

        @Test
        @DisplayName("First mention only is true by default")
        void firstMentionOnlyIsTrueByDefault() {
            LinkingStrategy strategy = new LinkingStrategy();
            assertTrue(strategy.isFirstMentionOnly());
        }

        @Test
        @DisplayName("Has expected default min relevance score")
        void hasExpectedDefaultMinRelevanceScore() {
            LinkingStrategy strategy = new LinkingStrategy();
            assertEquals(0.5, strategy.getMinRelevanceScore(), 0.001);
        }

        @Test
        @DisplayName("Prefer popular pages is true by default")
        void preferPopularPagesIsTrueByDefault() {
            LinkingStrategy strategy = new LinkingStrategy();
            assertTrue(strategy.isPreferPopularPages());
        }

        @Test
        @DisplayName("Has expected default weights")
        void hasExpectedDefaultWeights() {
            LinkingStrategy strategy = new LinkingStrategy();

            assertEquals(0.4, strategy.getFirstMentionWeight(), 0.001);
            assertEquals(0.4, strategy.getRelevanceWeight(), 0.001);
            assertEquals(0.2, strategy.getPopularityWeight(), 0.001);
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("defaults() creates standard strategy")
        void defaultsCreatesStandardStrategy() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            assertEquals(0.03, strategy.getMinLinkDensity(), 0.001);
            assertEquals(0.08, strategy.getMaxLinkDensity(), 0.001);
            assertEquals(20, strategy.getMaxLinksPerArticle());
        }

        @Test
        @DisplayName("minimal() creates conservative strategy")
        void minimalCreatesConservativeStrategy() {
            LinkingStrategy strategy = LinkingStrategy.minimal();

            assertEquals(0.01, strategy.getMinLinkDensity(), 0.001);
            assertEquals(0.03, strategy.getMaxLinkDensity(), 0.001);
            assertEquals(10, strategy.getMaxLinksPerArticle());
            assertEquals(0.7, strategy.getMinRelevanceScore(), 0.001);
        }

        @Test
        @DisplayName("aggressive() creates high-density strategy")
        void aggressiveCreatesHighDensityStrategy() {
            LinkingStrategy strategy = LinkingStrategy.aggressive();

            assertEquals(0.05, strategy.getMinLinkDensity(), 0.001);
            assertEquals(0.12, strategy.getMaxLinkDensity(), 0.001);
            assertEquals(30, strategy.getMaxLinksPerArticle());
            assertEquals(0.3, strategy.getMinRelevanceScore(), 0.001);
            assertEquals(30, strategy.getMinWordsBetweenLinks());
        }
    }

    @Nested
    @DisplayName("calculateTargetLinkCount()")
    class CalculateTargetLinkCount {

        @Test
        @DisplayName("Calculates based on average density")
        void calculatesBasedOnAverageDensity() {
            LinkingStrategy strategy = LinkingStrategy.defaults();
            // Average density is (0.03 + 0.08) / 2 = 0.055
            // For 1000 words: 1000 * 0.055 = 55, capped at 20
            assertEquals(20, strategy.calculateTargetLinkCount(1000));
        }

        @Test
        @DisplayName("Returns proportional count for shorter articles")
        void returnsProportionalCountForShorterArticles() {
            LinkingStrategy strategy = LinkingStrategy.defaults();
            // For 200 words: 200 * 0.055 = 11
            assertEquals(11, strategy.calculateTargetLinkCount(200));
        }

        @Test
        @DisplayName("Respects max links cap")
        void respectsMaxLinksCap() {
            LinkingStrategy strategy = new LinkingStrategy();
            strategy.setMaxLinksPerArticle(5);

            // Would calculate more than 5, but capped at 5
            assertEquals(5, strategy.calculateTargetLinkCount(500));
        }

        @Test
        @DisplayName("Returns zero for zero words")
        void returnsZeroForZeroWords() {
            LinkingStrategy strategy = LinkingStrategy.defaults();
            assertEquals(0, strategy.calculateTargetLinkCount(0));
        }
    }

    @Nested
    @DisplayName("isLinkDensityAcceptable()")
    class IsLinkDensityAcceptable {

        @Test
        @DisplayName("Returns true for density within range")
        void returnsTrueForDensityWithinRange() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            // 5 links in 100 words = 5% density (within 3-8%)
            assertTrue(strategy.isLinkDensityAcceptable(5, 100));
        }

        @Test
        @DisplayName("Returns false for density below minimum")
        void returnsFalseForDensityBelowMinimum() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            // 1 link in 100 words = 1% density (below 3%)
            assertFalse(strategy.isLinkDensityAcceptable(1, 100));
        }

        @Test
        @DisplayName("Returns false for density above maximum")
        void returnsFalseForDensityAboveMaximum() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            // 15 links in 100 words = 15% density (above 8%)
            assertFalse(strategy.isLinkDensityAcceptable(15, 100));
        }

        @Test
        @DisplayName("Returns true for density at minimum boundary")
        void returnsTrueForDensityAtMinimumBoundary() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            // 3 links in 100 words = exactly 3%
            assertTrue(strategy.isLinkDensityAcceptable(3, 100));
        }

        @Test
        @DisplayName("Returns true for density at maximum boundary")
        void returnsTrueForDensityAtMaximumBoundary() {
            LinkingStrategy strategy = LinkingStrategy.defaults();

            // 8 links in 100 words = exactly 8%
            assertTrue(strategy.isLinkDensityAcceptable(8, 100));
        }

        @Test
        @DisplayName("Returns true for zero links with zero words")
        void returnsTrueForZeroLinksWithZeroWords() {
            LinkingStrategy strategy = LinkingStrategy.defaults();
            assertTrue(strategy.isLinkDensityAcceptable(0, 0));
        }

        @Test
        @DisplayName("Returns false for non-zero links with zero words")
        void returnsFalseForNonZeroLinksWithZeroWords() {
            LinkingStrategy strategy = LinkingStrategy.defaults();
            assertFalse(strategy.isLinkDensityAcceptable(5, 0));
        }
    }

    @Nested
    @DisplayName("Setters")
    class Setters {

        @Test
        @DisplayName("All setters work correctly")
        void allSettersWorkCorrectly() {
            LinkingStrategy strategy = new LinkingStrategy();

            strategy.setMinLinkDensity(0.01);
            strategy.setMaxLinkDensity(0.10);
            strategy.setMaxLinksPerArticle(15);
            strategy.setMinWordsBetweenLinks(75);
            strategy.setFirstMentionOnly(false);
            strategy.setMinRelevanceScore(0.6);
            strategy.setPreferPopularPages(false);
            strategy.setFirstMentionWeight(0.5);
            strategy.setRelevanceWeight(0.3);
            strategy.setPopularityWeight(0.2);

            assertEquals(0.01, strategy.getMinLinkDensity(), 0.001);
            assertEquals(0.10, strategy.getMaxLinkDensity(), 0.001);
            assertEquals(15, strategy.getMaxLinksPerArticle());
            assertEquals(75, strategy.getMinWordsBetweenLinks());
            assertFalse(strategy.isFirstMentionOnly());
            assertEquals(0.6, strategy.getMinRelevanceScore(), 0.001);
            assertFalse(strategy.isPreferPopularPages());
            assertEquals(0.5, strategy.getFirstMentionWeight(), 0.001);
            assertEquals(0.3, strategy.getRelevanceWeight(), 0.001);
            assertEquals(0.2, strategy.getPopularityWeight(), 0.001);
        }
    }
}
