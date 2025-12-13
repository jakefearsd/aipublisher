package com.jakefear.aipublisher.domain;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TopicUniverse record.
 */
@DisplayName("TopicUniverse")
class TopicUniverseTest {

    private TopicUniverse universe;

    @BeforeEach
    void setUp() {
        universe = createTestUniverse();
    }

    private TopicUniverse createTestUniverse() {
        Topic events = Topic.builder("Events")
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.MUST_HAVE)
                .build();

        Topic kafka = Topic.builder("Apache Kafka")
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.SHOULD_HAVE)
                .build();

        Topic eventSourcing = Topic.builder("Event Sourcing")
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.MUST_HAVE)
                .complexity(ComplexityLevel.ADVANCED)
                .build();

        Topic proposed = Topic.builder("Proposed Topic")
                .status(TopicStatus.PROPOSED)
                .build();

        TopicRelationship eventsPrereqKafka = TopicRelationship.confirmed(
                "Events", "ApacheKafka", RelationshipType.PREREQUISITE_OF
        );

        TopicRelationship eventsPrereqEventSourcing = TopicRelationship.confirmed(
                "Events", "EventSourcing", RelationshipType.PREREQUISITE_OF
        );

        TopicRelationship kafkaRelatedEventSourcing = TopicRelationship.confirmed(
                "ApacheKafka", "EventSourcing", RelationshipType.RELATED_TO
        );

        return TopicUniverse.builder("Event-Driven Architecture")
                .description("Wiki about EDA")
                .addTopic(events)
                .addTopic(kafka)
                .addTopic(eventSourcing)
                .addTopic(proposed)
                .addRelationship(eventsPrereqKafka)
                .addRelationship(eventsPrereqEventSourcing)
                .addRelationship(kafkaRelatedEventSourcing)
                .scope(ScopeConfiguration.builder()
                        .addAssumedKnowledge("Basic programming")
                        .addFocusArea("Patterns")
                        .build())
                .build();
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("create() creates empty universe")
        void createCreatesEmptyUniverse() {
            TopicUniverse empty = TopicUniverse.create("Test Domain", "Description");

            assertEquals("test-domain", empty.id());
            assertEquals("Test Domain", empty.name());
            assertEquals("Description", empty.description());
            assertTrue(empty.topics().isEmpty());
            assertTrue(empty.relationships().isEmpty());
        }

        @Test
        @DisplayName("generateId creates URL-safe id")
        void generateIdCreatesUrlSafeId() {
            assertEquals("event-driven-architecture", TopicUniverse.generateId("Event-Driven Architecture"));
            assertEquals("test-domain", TopicUniverse.generateId("Test Domain"));
            assertEquals("kafka", TopicUniverse.generateId("Kafka"));
        }
    }

    @Nested
    @DisplayName("Topic queries")
    class TopicQueries {

        @Test
        @DisplayName("getAcceptedTopics returns only accepted")
        void getAcceptedTopicsReturnsOnlyAccepted() {
            List<Topic> accepted = universe.getAcceptedTopics();

            assertEquals(3, accepted.size());
            assertTrue(accepted.stream().allMatch(t -> t.status() == TopicStatus.ACCEPTED));
        }

        @Test
        @DisplayName("getProposedTopics returns only proposed")
        void getProposedTopicsReturnsOnlyProposed() {
            List<Topic> proposed = universe.getProposedTopics();

            assertEquals(1, proposed.size());
            assertEquals("Proposed Topic", proposed.get(0).name());
        }

        @Test
        @DisplayName("getTopicsByPriority filters by priority")
        void getTopicsByPriorityFiltersByPriority() {
            List<Topic> mustHave = universe.getTopicsByPriority(Priority.MUST_HAVE);

            assertEquals(2, mustHave.size());
            assertTrue(mustHave.stream().allMatch(t -> t.priority() == Priority.MUST_HAVE));
        }

        @Test
        @DisplayName("getTopicById finds existing topic")
        void getTopicByIdFindsExistingTopic() {
            Optional<Topic> found = universe.getTopicById("Events");

            assertTrue(found.isPresent());
            assertEquals("Events", found.get().name());
        }

        @Test
        @DisplayName("getTopicById returns empty for missing topic")
        void getTopicByIdReturnsEmptyForMissing() {
            Optional<Topic> found = universe.getTopicById("NonExistent");

            assertTrue(found.isEmpty());
        }

        @Test
        @DisplayName("getTopicByName finds topic case-insensitively")
        void getTopicByNameFindsCaseInsensitively() {
            Optional<Topic> found = universe.getTopicByName("EVENTS");

            assertTrue(found.isPresent());
            assertEquals("Events", found.get().name());
        }
    }

    @Nested
    @DisplayName("Relationship queries")
    class RelationshipQueries {

        @Test
        @DisplayName("getRelationshipsFor returns all relationships for topic")
        void getRelationshipsForReturnsAllRelationships() {
            List<TopicRelationship> rels = universe.getRelationshipsFor("Events");

            assertEquals(2, rels.size());
        }

        @Test
        @DisplayName("getOutgoingRelationships returns only outgoing")
        void getOutgoingRelationshipsReturnsOnlyOutgoing() {
            List<TopicRelationship> outgoing = universe.getOutgoingRelationships("Events");

            assertEquals(2, outgoing.size());
            assertTrue(outgoing.stream().allMatch(r -> r.sourceTopicId().equals("Events")));
        }

        @Test
        @DisplayName("getIncomingRelationships returns only incoming")
        void getIncomingRelationshipsReturnsOnlyIncoming() {
            List<TopicRelationship> incoming = universe.getIncomingRelationships("ApacheKafka");

            assertEquals(1, incoming.size());
            assertEquals("Events", incoming.get(0).sourceTopicId());
        }

        @Test
        @DisplayName("getPrerequisites returns prerequisite topics")
        void getPrerequisitesReturnsPrerequisiteTopics() {
            List<Topic> prereqs = universe.getPrerequisites("ApacheKafka");

            assertEquals(1, prereqs.size());
            assertEquals("Events", prereqs.get(0).name());
        }

        @Test
        @DisplayName("getRelatedTopics returns all related topics")
        void getRelatedTopicsReturnsAllRelated() {
            List<Topic> related = universe.getRelatedTopics("Events");

            assertEquals(2, related.size());
        }
    }

    @Nested
    @DisplayName("Generation ordering")
    class GenerationOrdering {

        @Test
        @DisplayName("getGenerationOrder respects prerequisites")
        void getGenerationOrderRespectsPrerequisites() {
            List<Topic> order = universe.getGenerationOrder();

            // Events should come before Kafka and EventSourcing
            int eventsIndex = findIndex(order, "Events");
            int kafkaIndex = findIndex(order, "ApacheKafka");
            int eventSourcingIndex = findIndex(order, "EventSourcing");

            assertTrue(eventsIndex < kafkaIndex, "Events should come before Kafka");
            assertTrue(eventsIndex < eventSourcingIndex, "Events should come before Event Sourcing");
        }

        @Test
        @DisplayName("getGenerationOrder excludes proposed topics")
        void getGenerationOrderExcludesProposed() {
            List<Topic> order = universe.getGenerationOrder();

            assertTrue(order.stream().noneMatch(t -> t.name().equals("Proposed Topic")));
        }

        private int findIndex(List<Topic> topics, String id) {
            for (int i = 0; i < topics.size(); i++) {
                if (topics.get(i).id().equals(id)) {
                    return i;
                }
            }
            return -1;
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("getAcceptedCount returns correct count")
        void getAcceptedCountReturnsCorrectCount() {
            assertEquals(3, universe.getAcceptedCount());
        }

        @Test
        @DisplayName("getEstimatedWordCount sums accepted topics")
        void getEstimatedWordCountSumsAcceptedTopics() {
            int total = universe.getEstimatedWordCount();

            assertTrue(total > 0);
        }

        @Test
        @DisplayName("getCountByPriority groups correctly")
        void getCountByPriorityGroupsCorrectly() {
            var counts = universe.getCountByPriority();

            assertEquals(2L, counts.get(Priority.MUST_HAVE));
            assertEquals(1L, counts.get(Priority.SHOULD_HAVE));
        }
    }

    @Nested
    @DisplayName("Modification")
    class Modification {

        @Test
        @DisplayName("addTopic creates new universe with topic")
        void addTopicCreatesNewUniverseWithTopic() {
            Topic newTopic = Topic.proposed("CQRS", "Command Query Separation");
            TopicUniverse updated = universe.addTopic(newTopic);

            assertEquals(4, universe.topics().size());
            assertEquals(5, updated.topics().size());
            assertTrue(updated.getTopicByName("CQRS").isPresent());
        }

        @Test
        @DisplayName("updateTopic modifies existing topic")
        void updateTopicModifiesExistingTopic() {
            Topic modified = universe.getTopicById("Events").get()
                    .toBuilder()
                    .description("Updated description")
                    .build();

            TopicUniverse updated = universe.updateTopic(modified);

            assertEquals("Updated description",
                    updated.getTopicById("Events").get().description());
        }

        @Test
        @DisplayName("addToBacklog adds item")
        void addToBacklogAddsItem() {
            TopicUniverse updated = universe.addToBacklog("Future topic idea");

            assertTrue(universe.backlog().isEmpty());
            assertEquals(1, updated.backlog().size());
            assertTrue(updated.backlog().contains("Future topic idea"));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects null name")
        void rejectsNullName() {
            assertThrows(NullPointerException.class, () ->
                    TopicUniverse.builder(null).build());
        }

        @Test
        @DisplayName("Rejects blank name")
        void rejectsBlankName() {
            assertThrows(IllegalArgumentException.class, () ->
                    TopicUniverse.builder("   ").build());
        }

        @Test
        @DisplayName("Applies defaults for null collections")
        void appliesDefaultsForNullCollections() {
            TopicUniverse u = new TopicUniverse(
                    "id", "Name", null, null, null, null, null, null, null
            );

            assertNotNull(u.topics());
            assertNotNull(u.relationships());
            assertNotNull(u.backlog());
            assertNotNull(u.scope());
        }
    }
}
