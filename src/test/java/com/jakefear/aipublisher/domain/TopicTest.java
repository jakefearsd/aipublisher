package com.jakefear.aipublisher.domain;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Topic record.
 */
@DisplayName("Topic")
class TopicTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("proposed() creates proposed topic with defaults")
        void proposedCreatesProposedTopic() {
            Topic topic = Topic.proposed("Apache Kafka", "High-throughput messaging");

            assertEquals("ApacheKafka", topic.id());
            assertEquals("Apache Kafka", topic.name());
            assertEquals("High-throughput messaging", topic.description());
            assertEquals(TopicStatus.PROPOSED, topic.status());
            assertEquals(ContentType.CONCEPT, topic.contentType());
            assertEquals(ComplexityLevel.INTERMEDIATE, topic.complexity());
            assertEquals(Priority.SHOULD_HAVE, topic.priority());
            assertNotNull(topic.createdAt());
        }

        @Test
        @DisplayName("generateId removes special characters")
        void generateIdRemovesSpecialCharacters() {
            assertEquals("ApacheKafka", Topic.generateId("Apache Kafka"));
            assertEquals("EventDrivenArchitecture", Topic.generateId("Event-Driven Architecture"));
            assertEquals("CQRSPattern", Topic.generateId("CQRS Pattern"));
            assertEquals("AWSAzure", Topic.generateId("AWS/Azure")); // Keeps both, just removes /
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates complete topic")
        void builderCreatesCompleteTopic() {
            Topic topic = Topic.builder("Event Sourcing")
                    .description("Store state as events")
                    .status(TopicStatus.ACCEPTED)
                    .contentType(ContentType.CONCEPT)
                    .complexity(ComplexityLevel.ADVANCED)
                    .priority(Priority.MUST_HAVE)
                    .estimatedWords(2000)
                    .addEmphasis("practical examples")
                    .addSkip("theory")
                    .userNotes("Focus on patterns")
                    .isLandingPage(false)
                    .category("Patterns")
                    .build();

            assertEquals("EventSourcing", topic.id());
            assertEquals("Event Sourcing", topic.name());
            assertEquals(TopicStatus.ACCEPTED, topic.status());
            assertEquals(2000, topic.estimatedWords());
            assertTrue(topic.emphasize().contains("practical examples"));
            assertTrue(topic.skip().contains("theory"));
        }

        @Test
        @DisplayName("toBuilder creates modifiable copy")
        void toBuilderCreatesModifiableCopy() {
            Topic original = Topic.proposed("Kafka", "Messaging");
            Topic modified = original.toBuilder()
                    .status(TopicStatus.ACCEPTED)
                    .priority(Priority.MUST_HAVE)
                    .build();

            assertEquals(TopicStatus.PROPOSED, original.status());
            assertEquals(TopicStatus.ACCEPTED, modified.status());
            assertEquals(Priority.MUST_HAVE, modified.priority());
            assertEquals(original.name(), modified.name());
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("accept() changes status to ACCEPTED")
        void acceptChangesStatusToAccepted() {
            Topic proposed = Topic.proposed("Kafka", "Messaging");
            Topic accepted = proposed.accept();

            assertEquals(TopicStatus.PROPOSED, proposed.status());
            assertEquals(TopicStatus.ACCEPTED, accepted.status());
        }

        @Test
        @DisplayName("reject() changes status to REJECTED")
        void rejectChangesStatusToRejected() {
            Topic proposed = Topic.proposed("Legacy Topic", "Not relevant");
            Topic rejected = proposed.reject();

            assertEquals(TopicStatus.REJECTED, rejected.status());
        }

        @Test
        @DisplayName("defer() changes status and priority")
        void deferChangesStatusAndPriority() {
            Topic proposed = Topic.proposed("Maybe Later", "Deferred");
            Topic deferred = proposed.defer();

            assertEquals(TopicStatus.DEFERRED, deferred.status());
            assertEquals(Priority.BACKLOG, deferred.priority());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("isReadyForGeneration returns true for accepted topics")
        void isReadyForGenerationReturnsTrueForAccepted() {
            Topic accepted = Topic.builder("Kafka")
                    .status(TopicStatus.ACCEPTED)
                    .priority(Priority.MUST_HAVE)
                    .build();

            assertTrue(accepted.isReadyForGeneration());
        }

        @Test
        @DisplayName("isReadyForGeneration returns false for proposed topics")
        void isReadyForGenerationReturnsFalseForProposed() {
            Topic proposed = Topic.proposed("Kafka", "Description");

            assertFalse(proposed.isReadyForGeneration());
        }

        @Test
        @DisplayName("isReadyForGeneration returns false for backlog topics")
        void isReadyForGenerationReturnsFalseForBacklog() {
            Topic backlog = Topic.builder("Kafka")
                    .status(TopicStatus.ACCEPTED)
                    .priority(Priority.BACKLOG)
                    .build();

            assertFalse(backlog.isReadyForGeneration());
        }

        @Test
        @DisplayName("hasUserGuidance detects custom guidance")
        void hasUserGuidanceDetectsCustomGuidance() {
            Topic noGuidance = Topic.proposed("Basic", "No guidance");
            assertFalse(noGuidance.hasUserGuidance());

            Topic withEmphasis = noGuidance.toBuilder()
                    .addEmphasis("performance")
                    .build();
            assertTrue(withEmphasis.hasUserGuidance());

            Topic withNotes = noGuidance.toBuilder()
                    .userNotes("Important context")
                    .build();
            assertTrue(withNotes.hasUserGuidance());
        }

        @Test
        @DisplayName("getWikiPageName generates proper name")
        void getWikiPageNameGeneratesProperName() {
            Topic topic = Topic.proposed("Event-Driven Architecture", "Description");
            assertEquals("EventDrivenArchitecture", topic.getWikiPageName());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects null name")
        void rejectsNullName() {
            assertThrows(NullPointerException.class, () ->
                    Topic.builder(null).build());
        }

        @Test
        @DisplayName("Rejects blank name")
        void rejectsBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                    Topic.builder("   ").build());
        }

        @Test
        @DisplayName("Applies defaults for null values")
        void appliesDefaultsForNullValues() {
            Topic topic = new Topic(
                    "id", "Name", null, TopicStatus.PROPOSED,
                    null, null, null, 0, null, null, null,
                    false, false, null, null, null, null, null
            );

            assertEquals("", topic.description());
            assertEquals(ContentType.CONCEPT, topic.contentType());
            assertEquals(ComplexityLevel.INTERMEDIATE, topic.complexity());
            assertNotNull(topic.emphasize());
            assertNotNull(topic.skip());
        }
    }
}
