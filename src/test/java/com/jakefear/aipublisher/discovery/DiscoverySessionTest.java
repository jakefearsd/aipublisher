package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DiscoverySession.
 */
@DisplayName("DiscoverySession")
class DiscoverySessionTest {

    private DiscoverySession session;

    @BeforeEach
    void setUp() {
        session = new DiscoverySession("Apache Kafka");
    }

    @Nested
    @DisplayName("Initialization")
    class Initialization {

        @Test
        @DisplayName("Creates session with domain name")
        void createsSessionWithDomainName() {
            assertEquals("Apache Kafka", session.getDomainName());
            assertNotNull(session.getSessionId());
            assertEquals(8, session.getSessionId().length());
            assertNotNull(session.getStartedAt());
        }

        @Test
        @DisplayName("Starts in SEED_INPUT phase")
        void startsInSeedInputPhase() {
            assertEquals(DiscoveryPhase.SEED_INPUT, session.getCurrentPhase());
            assertFalse(session.isComplete());
        }

        @Test
        @DisplayName("History records session start")
        void historyRecordsSessionStart() {
            List<DiscoverySession.SessionEvent> history = session.getHistory();
            assertEquals(1, history.size());
            assertTrue(history.get(0).description().contains("Session started"));
        }
    }

    @Nested
    @DisplayName("Phase management")
    class PhaseManagement {

        @Test
        @DisplayName("advancePhase() moves to next phase")
        void advancePhaseMovesToNext() {
            session.advancePhase();
            assertEquals(DiscoveryPhase.SCOPE_SETUP, session.getCurrentPhase());

            session.advancePhase();
            assertEquals(DiscoveryPhase.TOPIC_EXPANSION, session.getCurrentPhase());
        }

        @Test
        @DisplayName("goToPhase() jumps to specific phase")
        void goToPhaseJumpsToSpecificPhase() {
            session.goToPhase(DiscoveryPhase.GAP_ANALYSIS);
            assertEquals(DiscoveryPhase.GAP_ANALYSIS, session.getCurrentPhase());
        }

        @Test
        @DisplayName("isComplete() returns true at COMPLETE phase")
        void isCompleteReturnsTrueAtComplete() {
            session.goToPhase(DiscoveryPhase.COMPLETE);
            assertTrue(session.isComplete());
        }

        @Test
        @DisplayName("Phase changes recorded in history")
        void phaseChangesRecordedInHistory() {
            session.advancePhase();
            session.advancePhase();

            List<DiscoverySession.SessionEvent> history = session.getHistory();
            assertTrue(history.stream()
                    .anyMatch(e -> e.description().contains("Phase advanced")));
        }
    }

    @Nested
    @DisplayName("Seed topics")
    class SeedTopics {

        @Test
        @DisplayName("addSeedTopic() adds accepted topic")
        void addSeedTopicAddsAcceptedTopic() {
            session.addSeedTopic("Kafka Producers", "How to produce messages");

            TopicUniverse universe = session.buildUniverse();
            assertEquals(1, universe.topics().size());

            Topic topic = universe.topics().get(0);
            assertEquals("Kafka Producers", topic.name());
            assertEquals(TopicStatus.ACCEPTED, topic.status());
            assertEquals(Priority.MUST_HAVE, topic.priority());
            assertFalse(topic.isLandingPage());
        }

        @Test
        @DisplayName("addLandingPage() adds topic marked as landing page")
        void addLandingPageAddsMarkedTopic() {
            session.addLandingPage("Apache Kafka Overview", "Introduction to Kafka");

            TopicUniverse universe = session.buildUniverse();
            Topic topic = universe.topics().get(0);
            assertTrue(topic.isLandingPage());
            assertEquals(Priority.MUST_HAVE, topic.priority());
        }
    }

    @Nested
    @DisplayName("Topic suggestions")
    class TopicSuggestions {

        @Test
        @DisplayName("addTopicSuggestions() adds to pending list")
        void addTopicSuggestionsAddsToPending() {
            List<TopicSuggestion> suggestions = List.of(
                    TopicSuggestion.simple("Consumers", "Reading messages"),
                    TopicSuggestion.simple("Topics", "Message categories")
            );

            session.addTopicSuggestions(suggestions);

            assertEquals(2, session.getPendingTopicSuggestions().size());
        }

        @Test
        @DisplayName("acceptTopicSuggestion() converts to topic")
        void acceptTopicSuggestionConvertsToTopic() {
            TopicSuggestion suggestion = TopicSuggestion.builder("Kafka Streams")
                    .description("Stream processing")
                    .contentType(ContentType.TUTORIAL)
                    .complexity(ComplexityLevel.ADVANCED)
                    .category("Features")
                    .build();

            session.addTopicSuggestions(List.of(suggestion));
            session.acceptTopicSuggestion(suggestion);

            assertEquals(0, session.getPendingTopicSuggestions().size());

            TopicUniverse universe = session.buildUniverse();
            assertEquals(1, universe.topics().size());

            Topic topic = universe.topics().get(0);
            assertEquals("Kafka Streams", topic.name());
            assertEquals(TopicStatus.ACCEPTED, topic.status());
            assertEquals(ContentType.TUTORIAL, topic.contentType());
            assertEquals(ComplexityLevel.ADVANCED, topic.complexity());
        }

        @Test
        @DisplayName("rejectTopicSuggestion() removes from pending")
        void rejectTopicSuggestionRemovesFromPending() {
            TopicSuggestion suggestion = TopicSuggestion.simple("Not Relevant", "Skip this");
            session.addTopicSuggestions(List.of(suggestion));

            session.rejectTopicSuggestion(suggestion);

            assertEquals(0, session.getPendingTopicSuggestions().size());
            assertEquals(0, session.buildUniverse().topics().size());
        }

        @Test
        @DisplayName("deferTopicSuggestion() moves to backlog")
        void deferTopicSuggestionMovesToBacklog() {
            TopicSuggestion suggestion = TopicSuggestion.simple("Maybe Later", "Consider later");
            session.addTopicSuggestions(List.of(suggestion));

            session.deferTopicSuggestion(suggestion);

            assertEquals(0, session.getPendingTopicSuggestions().size());
            assertEquals(1, session.buildUniverse().backlog().size());
        }

        @Test
        @DisplayName("modifyAndAcceptTopic() allows customization")
        void modifyAndAcceptTopicAllowsCustomization() {
            TopicSuggestion suggestion = TopicSuggestion.simple("Topic", "Original description");
            session.addTopicSuggestions(List.of(suggestion));

            Topic.Builder modifications = Topic.builder("Custom Topic Name")
                    .description("Custom description")
                    .status(TopicStatus.ACCEPTED)
                    .priority(Priority.MUST_HAVE);

            Topic result = session.modifyAndAcceptTopic(suggestion, modifications);

            assertEquals("Custom Topic Name", result.name());
            assertEquals("Custom description", result.description());
            assertEquals(0, session.getPendingTopicSuggestions().size());
        }
    }

    @Nested
    @DisplayName("Relationship suggestions")
    class RelationshipSuggestions {

        @Test
        @DisplayName("addRelationshipSuggestions() adds to pending")
        void addRelationshipSuggestionsAddsToPending() {
            List<RelationshipSuggestion> suggestions = List.of(
                    RelationshipSuggestion.simple("A", "B", RelationshipType.PREREQUISITE_OF),
                    RelationshipSuggestion.simple("B", "C", RelationshipType.RELATED_TO)
            );

            session.addRelationshipSuggestions(suggestions);

            assertEquals(2, session.getPendingRelationshipSuggestions().size());
        }

        @Test
        @DisplayName("confirmRelationship() creates confirmed relationship")
        void confirmRelationshipCreatesConfirmed() {
            // First add topics so relationship is valid
            session.addSeedTopic("Java Basics", "Java fundamentals");
            session.addSeedTopic("Spring Framework", "Spring ecosystem");

            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "Java Basics", "Spring Framework", RelationshipType.PREREQUISITE_OF);
            session.addRelationshipSuggestions(List.of(suggestion));

            session.confirmRelationship(suggestion);

            assertEquals(0, session.getPendingRelationshipSuggestions().size());

            TopicUniverse universe = session.buildUniverse();
            assertEquals(1, universe.relationships().size());
            assertEquals(RelationshipType.PREREQUISITE_OF, universe.relationships().get(0).type());
        }

        @Test
        @DisplayName("confirmRelationshipAs() changes type")
        void confirmRelationshipAsChangesType() {
            session.addSeedTopic("A", "Topic A");
            session.addSeedTopic("B", "Topic B");

            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.RELATED_TO);
            session.addRelationshipSuggestions(List.of(suggestion));

            session.confirmRelationshipAs(suggestion, RelationshipType.PART_OF);

            TopicUniverse universe = session.buildUniverse();
            assertEquals(RelationshipType.PART_OF, universe.relationships().get(0).type());
        }

        @Test
        @DisplayName("rejectRelationship() removes from pending")
        void rejectRelationshipRemovesFromPending() {
            RelationshipSuggestion suggestion = RelationshipSuggestion.simple(
                    "A", "B", RelationshipType.RELATED_TO);
            session.addRelationshipSuggestions(List.of(suggestion));

            session.rejectRelationship(suggestion);

            assertEquals(0, session.getPendingRelationshipSuggestions().size());
        }
    }

    @Nested
    @DisplayName("Gap management")
    class GapManagement {

        @Test
        @DisplayName("addGaps() adds to pending gaps")
        void addGapsAddsToPending() {
            session.addGaps(List.of("Missing prerequisite for X", "Coverage gap in Y"));

            assertEquals(2, session.getPendingGaps().size());
        }

        @Test
        @DisplayName("addressGapWithTopic() adds topic and removes gap")
        void addressGapWithTopicAddsTopicAndRemovesGap() {
            session.addGaps(List.of("Missing: Consumer Groups"));

            Topic topic = Topic.builder("Consumer Groups")
                    .description("Managing consumer groups")
                    .status(TopicStatus.ACCEPTED)
                    .build();

            session.addressGapWithTopic("Missing: Consumer Groups", topic);

            assertEquals(0, session.getPendingGaps().size());
            assertEquals(1, session.buildUniverse().topics().size());
        }

        @Test
        @DisplayName("ignoreGap() removes gap without action")
        void ignoreGapRemovesWithoutAction() {
            session.addGaps(List.of("Minor gap"));

            session.ignoreGap("Minor gap");

            assertEquals(0, session.getPendingGaps().size());
        }
    }

    @Nested
    @DisplayName("Topic modifications")
    class TopicModifications {

        @BeforeEach
        void addTopic() {
            session.addSeedTopic("Test Topic", "A test topic");
        }

        @Test
        @DisplayName("updateTopicPriority() changes priority")
        void updateTopicPriorityChangesPriority() {
            TopicUniverse universe = session.buildUniverse();
            String topicId = universe.topics().get(0).id();

            session.updateTopicPriority(topicId, Priority.NICE_TO_HAVE);

            universe = session.buildUniverse();
            assertEquals(Priority.NICE_TO_HAVE, universe.topics().get(0).priority());
        }

        @Test
        @DisplayName("updateTopicDepth() changes word count")
        void updateTopicDepthChangesWordCount() {
            TopicUniverse universe = session.buildUniverse();
            String topicId = universe.topics().get(0).id();

            session.updateTopicDepth(topicId, 3000);

            universe = session.buildUniverse();
            assertEquals(3000, universe.topics().get(0).estimatedWords());
        }

        @Test
        @DisplayName("addTopicEmphasis() adds emphasis point")
        void addTopicEmphasisAddsEmphasis() {
            TopicUniverse universe = session.buildUniverse();
            String topicId = universe.topics().get(0).id();

            session.addTopicEmphasis(topicId, "performance considerations");

            universe = session.buildUniverse();
            assertTrue(universe.topics().get(0).emphasize().contains("performance considerations"));
        }

        @Test
        @DisplayName("addTopicSkip() adds skip directive")
        void addTopicSkipAddsSkip() {
            TopicUniverse universe = session.buildUniverse();
            String topicId = universe.topics().get(0).id();

            session.addTopicSkip(topicId, "legacy approaches");

            universe = session.buildUniverse();
            assertTrue(universe.topics().get(0).skip().contains("legacy approaches"));
        }
    }

    @Nested
    @DisplayName("Expansion tracking")
    class ExpansionTracking {

        @Test
        @DisplayName("setExpansionSource() tracks source and depth")
        void setExpansionSourceTracksSourceAndDepth() {
            session.setExpansionSource("Root Topic");
            assertEquals("Root Topic", session.getCurrentExpansionSource());
            assertEquals(1, session.getExpansionDepth());

            session.setExpansionSource("Subtopic");
            assertEquals("Subtopic", session.getCurrentExpansionSource());
            assertEquals(2, session.getExpansionDepth());
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("getAcceptedTopicCount() returns accepted count")
        void getAcceptedTopicCountReturnsAcceptedCount() {
            assertEquals(0, session.getAcceptedTopicCount());

            session.addSeedTopic("Topic 1", "Description");
            session.addSeedTopic("Topic 2", "Description");

            assertEquals(2, session.getAcceptedTopicCount());
        }

        @Test
        @DisplayName("getPendingSuggestionCount() sums all pending items")
        void getPendingSuggestionCountSumsAllPending() {
            session.addTopicSuggestions(List.of(
                    TopicSuggestion.simple("A", ""),
                    TopicSuggestion.simple("B", "")
            ));
            session.addRelationshipSuggestions(List.of(
                    RelationshipSuggestion.simple("A", "B", RelationshipType.RELATED_TO)
            ));
            session.addGaps(List.of("Gap 1", "Gap 2", "Gap 3"));

            assertEquals(6, session.getPendingSuggestionCount());
        }
    }

    @Nested
    @DisplayName("Scope configuration")
    class ScopeConfigurationTests {

        @Test
        @DisplayName("configureScope() sets scope on universe")
        void configureScopeSetsOnUniverse() {
            ScopeConfiguration scope = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java")
                    .addFocusArea("Streaming")
                    .audienceDescription("Senior developers")
                    .build();

            session.configureScope(scope);

            ScopeConfiguration retrieved = session.getScope();
            assertTrue(retrieved.assumedKnowledge().contains("Java"));
            assertTrue(retrieved.focusAreas().contains("Streaming"));
            assertEquals("Senior developers", retrieved.audienceDescription());
        }
    }
}
