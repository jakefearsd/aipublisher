package com.jakefear.aipublisher.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TopicRelationship record.
 */
@DisplayName("TopicRelationship")
class TopicRelationshipTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("suggested() creates suggested relationship")
        void suggestedCreatesSuggestedRelationship() {
            TopicRelationship rel = TopicRelationship.suggested(
                    "Java", "SpringBoot", RelationshipType.PREREQUISITE_OF, 0.9
            );

            assertEquals("Java", rel.sourceTopicId());
            assertEquals("SpringBoot", rel.targetTopicId());
            assertEquals(RelationshipType.PREREQUISITE_OF, rel.type());
            assertEquals(TopicRelationship.RelationshipStatus.SUGGESTED, rel.status());
            assertEquals(0.9, rel.confidence());
        }

        @Test
        @DisplayName("confirmed() creates confirmed relationship")
        void confirmedCreatesConfirmedRelationship() {
            TopicRelationship rel = TopicRelationship.confirmed(
                    "Events", "EventSourcing", RelationshipType.PART_OF
            );

            assertEquals(TopicRelationship.RelationshipStatus.CONFIRMED, rel.status());
            assertEquals(1.0, rel.confidence());
        }
    }

    @Nested
    @DisplayName("Status transitions")
    class StatusTransitions {

        @Test
        @DisplayName("confirm() changes status to CONFIRMED")
        void confirmChangesStatusToConfirmed() {
            TopicRelationship suggested = TopicRelationship.suggested(
                    "A", "B", RelationshipType.RELATED_TO, 0.7
            );
            TopicRelationship confirmed = suggested.confirm();

            assertEquals(TopicRelationship.RelationshipStatus.SUGGESTED, suggested.status());
            assertEquals(TopicRelationship.RelationshipStatus.CONFIRMED, confirmed.status());
            assertEquals(1.0, confirmed.confidence());
        }

        @Test
        @DisplayName("confirmAs() changes type and status")
        void confirmAsChangesTypeAndStatus() {
            TopicRelationship suggested = TopicRelationship.suggested(
                    "CQRS", "EventSourcing", RelationshipType.PREREQUISITE_OF, 0.6
            );
            TopicRelationship modified = suggested.confirmAs(RelationshipType.PAIRS_WITH);

            assertEquals(TopicRelationship.RelationshipStatus.MODIFIED, modified.status());
            assertEquals(RelationshipType.PAIRS_WITH, modified.type());
        }

        @Test
        @DisplayName("reject() changes status to REJECTED")
        void rejectChangesStatusToRejected() {
            TopicRelationship suggested = TopicRelationship.suggested(
                    "A", "B", RelationshipType.RELATED_TO, 0.5
            );
            TopicRelationship rejected = suggested.reject();

            assertEquals(TopicRelationship.RelationshipStatus.REJECTED, rejected.status());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("isActive returns true for confirmed relationships")
        void isActiveReturnsTrueForConfirmed() {
            TopicRelationship confirmed = TopicRelationship.confirmed(
                    "A", "B", RelationshipType.RELATED_TO
            );
            assertTrue(confirmed.isActive());
        }

        @Test
        @DisplayName("isActive returns false for suggested relationships")
        void isActiveReturnsFalseForSuggested() {
            TopicRelationship suggested = TopicRelationship.suggested(
                    "A", "B", RelationshipType.RELATED_TO, 0.5
            );
            assertFalse(suggested.isActive());
        }

        @Test
        @DisplayName("impliesOrdering returns true for ordering relationships")
        void impliesOrderingReturnsTrueForOrderingRelationships() {
            TopicRelationship prereq = TopicRelationship.confirmed(
                    "Java", "Spring", RelationshipType.PREREQUISITE_OF
            );
            assertTrue(prereq.impliesOrdering());

            TopicRelationship partOf = TopicRelationship.confirmed(
                    "Consumer", "Kafka", RelationshipType.PART_OF
            );
            assertTrue(partOf.impliesOrdering());
        }

        @Test
        @DisplayName("impliesOrdering returns false for non-ordering relationships")
        void impliesOrderingReturnsFalseForNonOrderingRelationships() {
            TopicRelationship related = TopicRelationship.confirmed(
                    "Kafka", "RabbitMQ", RelationshipType.RELATED_TO
            );
            assertFalse(related.impliesOrdering());
        }

        @Test
        @DisplayName("describe generates readable description")
        void describeGeneratesReadableDescription() {
            TopicRelationship rel = TopicRelationship.confirmed(
                    "Java", "SpringBoot", RelationshipType.PREREQUISITE_OF
            );
            assertEquals("Java prerequisite of SpringBoot", rel.describe());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Rejects null sourceTopicId")
        void rejectsNullSourceTopicId() {
            assertThrows(NullPointerException.class, () ->
                    TopicRelationship.confirmed(null, "B", RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Rejects null targetTopicId")
        void rejectsNullTargetTopicId() {
            assertThrows(NullPointerException.class, () ->
                    TopicRelationship.confirmed("A", null, RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Rejects self-referential relationship")
        void rejectsSelfReferentialRelationship() {
            assertThrows(IllegalArgumentException.class, () ->
                    TopicRelationship.confirmed("A", "A", RelationshipType.RELATED_TO));
        }

        @Test
        @DisplayName("Generates ID automatically")
        void generatesIdAutomatically() {
            TopicRelationship rel = TopicRelationship.confirmed(
                    "Java", "Spring", RelationshipType.PREREQUISITE_OF
            );
            assertEquals("Java_PREREQUISITE_OF_Spring", rel.id());
        }
    }

    @Nested
    @DisplayName("withNote")
    class WithNote {

        @Test
        @DisplayName("Adds user note to relationship")
        void addsUserNoteToRelationship() {
            TopicRelationship rel = TopicRelationship.confirmed(
                    "A", "B", RelationshipType.RELATED_TO
            );
            TopicRelationship withNote = rel.withNote("User explanation");

            assertEquals("", rel.userNote());
            assertEquals("User explanation", withNote.userNote());
        }
    }
}
