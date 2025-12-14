package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.domain.RelationshipType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RelationshipSuggestion record.
 */
@DisplayName("RelationshipSuggestion")
class RelationshipSuggestionTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("simple() creates suggestion with defaults")
        void simpleCreatesWithDefaults() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "Kafka", "Event Sourcing", RelationshipType.IMPLEMENTS);

            assertEquals("Kafka", suggestion.sourceTopicName());
            assertEquals("Event Sourcing", suggestion.targetTopicName());
            assertEquals(RelationshipType.IMPLEMENTS, suggestion.suggestedType());
            assertEquals(0.5, suggestion.confidence());
            assertEquals("", suggestion.rationale());
        }

        @Test
        @DisplayName("withConfidence() creates suggestion with custom confidence")
        void withConfidenceCreatesWithConfidence() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.withConfidence(
                    "Java Basics", "Spring Framework", RelationshipType.PREREQUISITE_OF, 0.95);

            assertEquals("Java Basics", suggestion.sourceTopicName());
            assertEquals("Spring Framework", suggestion.targetTopicName());
            assertEquals(RelationshipType.PREREQUISITE_OF, suggestion.suggestedType());
            assertEquals(0.95, suggestion.confidence());
        }

        @Test
        @DisplayName("full() creates complete suggestion")
        void fullCreatesCompleteSuggestion() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.full(
                    "REST API", "GraphQL",
                    RelationshipType.CONTRASTS_WITH,
                    0.85,
                    "Both are API paradigms but with different approaches");

            assertEquals("REST API", suggestion.sourceTopicName());
            assertEquals("GraphQL", suggestion.targetTopicName());
            assertEquals(RelationshipType.CONTRASTS_WITH, suggestion.suggestedType());
            assertEquals(0.85, suggestion.confidence());
            assertEquals("Both are API paradigms but with different approaches", suggestion.rationale());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects null source topic")
        void rejectsNullSource() {
            assertThrows(NullPointerException.class, () ->
                    RelationshipSuggestion.simple(null, "Target", RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Rejects null target topic")
        void rejectsNullTarget() {
            assertThrows(NullPointerException.class, () ->
                    RelationshipSuggestion.simple("Source", null, RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Rejects null relationship type")
        void rejectsNullType() {
            assertThrows(NullPointerException.class, () ->
                    RelationshipSuggestion.simple("Source", "Target", null));
        }

        @Test
        @DisplayName("Rejects self-referential relationship")
        void rejectsSelfReference() {
            assertThrows(IllegalArgumentException.class, () ->
                    RelationshipSuggestion.simple("Kafka", "Kafka", RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Normalizes out-of-range confidence to 0.5")
        void normalizesConfidence() {
            RelationshipSuggestion tooHigh = new RelationshipSuggestion(
                    "A", "B", RelationshipType.RELATED_TO, 1.5, "");
            assertEquals(0.5, tooHigh.confidence());

            RelationshipSuggestion tooLow = new RelationshipSuggestion(
                    "A", "B", RelationshipType.RELATED_TO, -0.5, "");
            assertEquals(0.5, tooLow.confidence());
        }

        @Test
        @DisplayName("Null rationale becomes empty string")
        void nullRationaleBecomesEmpty() {
            RelationshipSuggestion suggestion = new RelationshipSuggestion(
                    "A", "B", RelationshipType.RELATED_TO, 0.5, null);
            assertEquals("", suggestion.rationale());
        }
    }

    @Nested
    @DisplayName("Display methods")
    class DisplayMethods {

        @Test
        @DisplayName("describe() returns human-readable description")
        void describeReturnsReadableDescription() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "Spring Boot", "Spring Framework", RelationshipType.PART_OF);

            String description = suggestion.describe();
            assertTrue(description.contains("Spring Boot"));
            assertTrue(description.contains("Spring Framework"));
            assertTrue(description.contains("part of"));
        }

        @Test
        @DisplayName("toDisplayString() shows confidence indicator")
        void displayStringShowsConfidenceIndicator() {
            RelationshipSuggestion highConfidence = RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.PREREQUISITE_OF, 0.9);
            assertTrue(highConfidence.toDisplayString().contains("●"));

            RelationshipSuggestion mediumConfidence = RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.PREREQUISITE_OF, 0.6);
            assertTrue(mediumConfidence.toDisplayString().contains("◐"));

            RelationshipSuggestion lowConfidence = RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.PREREQUISITE_OF, 0.3);
            assertTrue(lowConfidence.toDisplayString().contains("○"));
        }

        @Test
        @DisplayName("toDisplayString() includes topics and type")
        void displayStringIncludesTopicsAndType() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "Kafka", "ZooKeeper", RelationshipType.PAIRS_WITH);

            String display = suggestion.toDisplayString();
            assertTrue(display.contains("Kafka"));
            assertTrue(display.contains("ZooKeeper"));
            assertTrue(display.contains("PAIRS_WITH"));
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("isHighConfidence() returns true for 0.8+")
        void isHighConfidenceReturnsTrueForHighValues() {
            assertTrue(RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.RELATED_TO, 0.8).isHighConfidence());
            assertTrue(RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.RELATED_TO, 0.95).isHighConfidence());
        }

        @Test
        @DisplayName("isHighConfidence() returns false for below 0.8")
        void isHighConfidenceReturnsFalseForLowValues() {
            assertFalse(RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.RELATED_TO, 0.79).isHighConfidence());
            assertFalse(RelationshipSuggestion.withConfidence(
                    "A", "B", RelationshipType.RELATED_TO, 0.5).isHighConfidence());
        }

        @Test
        @DisplayName("impliesOrdering() delegates to relationship type")
        void impliesOrderingDelegatesToType() {
            assertTrue(RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.PREREQUISITE_OF).impliesOrdering());
            assertTrue(RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.PART_OF).impliesOrdering());

            assertFalse(RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.RELATED_TO).impliesOrdering());
            assertFalse(RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.CONTRASTS_WITH).impliesOrdering());
        }
    }
}
