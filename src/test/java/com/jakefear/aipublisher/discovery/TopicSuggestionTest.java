package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.domain.ComplexityLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TopicSuggestion record.
 */
@DisplayName("TopicSuggestion")
class TopicSuggestionTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("simple() creates suggestion with defaults")
        void simpleCreatesWithDefaults() {
            TopicSuggestion suggestion = TopicSuggestion.simple("Kafka", "Message streaming platform");

            assertEquals("Kafka", suggestion.name());
            assertEquals("Message streaming platform", suggestion.description());
            assertEquals("", suggestion.category());
            assertEquals(ContentType.CONCEPT, suggestion.suggestedContentType());
            assertEquals(ComplexityLevel.INTERMEDIATE, suggestion.suggestedComplexity());
            assertEquals(1000, suggestion.suggestedWordCount());
            assertEquals(0.5, suggestion.relevanceScore());
        }

        @Test
        @DisplayName("analyzed() creates full suggestion")
        void analyzedCreatesFullSuggestion() {
            TopicSuggestion suggestion = TopicSuggestion.analyzed(
                    "Kafka Producers",
                    "How to produce messages",
                    "Components",
                    ContentType.TUTORIAL,
                    ComplexityLevel.BEGINNER,
                    0.9,
                    "Essential for any Kafka user"
            );

            assertEquals("Kafka Producers", suggestion.name());
            assertEquals("How to produce messages", suggestion.description());
            assertEquals("Components", suggestion.category());
            assertEquals(ContentType.TUTORIAL, suggestion.suggestedContentType());
            assertEquals(ComplexityLevel.BEGINNER, suggestion.suggestedComplexity());
            assertEquals(0.9, suggestion.relevanceScore());
            assertEquals("Essential for any Kafka user", suggestion.rationale());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates complete suggestion")
        void builderCreatesCompleteSuggestion() {
            TopicSuggestion suggestion = TopicSuggestion.builder("Event Sourcing")
                    .description("Store state as events")
                    .category("Patterns")
                    .contentType(ContentType.CONCEPT)
                    .complexity(ComplexityLevel.ADVANCED)
                    .wordCount(2000)
                    .relevance(0.85)
                    .rationale("Foundation for event-driven systems")
                    .sourceContext("Expanded from CQRS")
                    .build();

            assertEquals("Event Sourcing", suggestion.name());
            assertEquals("Store state as events", suggestion.description());
            assertEquals("Patterns", suggestion.category());
            assertEquals(ContentType.CONCEPT, suggestion.suggestedContentType());
            assertEquals(ComplexityLevel.ADVANCED, suggestion.suggestedComplexity());
            assertEquals(2000, suggestion.suggestedWordCount());
            assertEquals(0.85, suggestion.relevanceScore());
            assertEquals("Foundation for event-driven systems", suggestion.rationale());
            assertEquals("Expanded from CQRS", suggestion.sourceContext());
        }

        @Test
        @DisplayName("Builder complexity sets default word count")
        void complexitySetsDefaultWordCount() {
            TopicSuggestion beginner = TopicSuggestion.builder("Topic")
                    .complexity(ComplexityLevel.BEGINNER)
                    .build();

            TopicSuggestion advanced = TopicSuggestion.builder("Topic")
                    .complexity(ComplexityLevel.ADVANCED)
                    .build();

            assertEquals(ComplexityLevel.BEGINNER.getMinWords(), beginner.suggestedWordCount());
            assertEquals(ComplexityLevel.ADVANCED.getMinWords(), advanced.suggestedWordCount());
        }

        @Test
        @DisplayName("Builder wordCount overrides complexity default")
        void wordCountOverridesComplexityDefault() {
            TopicSuggestion suggestion = TopicSuggestion.builder("Topic")
                    .complexity(ComplexityLevel.BEGINNER)
                    .wordCount(5000)
                    .build();

            assertEquals(5000, suggestion.suggestedWordCount());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects null name")
        void rejectsNullName() {
            assertThrows(NullPointerException.class, () ->
                    TopicSuggestion.simple(null, "Description"));
        }

        @Test
        @DisplayName("Rejects blank name")
        void rejectsBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                    TopicSuggestion.simple("  ", "Description"));
        }

        @Test
        @DisplayName("Normalizes relevance score to valid range")
        void normalizesRelevanceScore() {
            TopicSuggestion tooHigh = new TopicSuggestion(
                    "Test", "", "", ContentType.CONCEPT,
                    ComplexityLevel.INTERMEDIATE, 1000, 1.5, "", "", -1.0
            );
            assertEquals(0.5, tooHigh.relevanceScore());

            TopicSuggestion tooLow = new TopicSuggestion(
                    "Test", "", "", ContentType.CONCEPT,
                    ComplexityLevel.INTERMEDIATE, 1000, -0.5, "", "", -1.0
            );
            assertEquals(0.5, tooLow.relevanceScore());
        }

        @Test
        @DisplayName("Applies defaults for null values")
        void appliesDefaultsForNulls() {
            TopicSuggestion suggestion = new TopicSuggestion(
                    "Test", null, null, null, null, 0, 0.5, null, null, -1.0
            );

            assertEquals("", suggestion.description());
            assertEquals("", suggestion.category());
            assertEquals(ContentType.CONCEPT, suggestion.suggestedContentType());
            assertEquals(ComplexityLevel.INTERMEDIATE, suggestion.suggestedComplexity());
            assertTrue(suggestion.suggestedWordCount() > 0);
        }
    }

    @Nested
    @DisplayName("Display methods")
    class DisplayMethods {

        @Test
        @DisplayName("getRelevanceIndicator shows bars proportional to score")
        void relevanceIndicatorShowsBars() {
            TopicSuggestion highRelevance = TopicSuggestion.builder("Test")
                    .relevance(1.0)
                    .build();
            assertEquals("██████████", highRelevance.getRelevanceIndicator());

            TopicSuggestion lowRelevance = TopicSuggestion.builder("Test")
                    .relevance(0.0)
                    .build();
            assertEquals("░░░░░░░░░░", lowRelevance.getRelevanceIndicator());

            TopicSuggestion midRelevance = TopicSuggestion.builder("Test")
                    .relevance(0.5)
                    .build();
            assertEquals("█████░░░░░", midRelevance.getRelevanceIndicator());
        }

        @Test
        @DisplayName("getSummary includes key information")
        void summaryIncludesKeyInfo() {
            TopicSuggestion suggestion = TopicSuggestion.builder("Kafka Streams")
                    .contentType(ContentType.TUTORIAL)
                    .complexity(ComplexityLevel.ADVANCED)
                    .wordCount(2500)
                    .build();

            String summary = suggestion.getSummary();
            assertTrue(summary.contains("Kafka Streams"));
            assertTrue(summary.contains("Tutorial"));
            assertTrue(summary.contains("Advanced"));
            assertTrue(summary.contains("2500"));
        }
    }
}
