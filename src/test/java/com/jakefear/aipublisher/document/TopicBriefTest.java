package com.jakefear.aipublisher.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TopicBrief")
class TopicBriefTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates with all fields")
        void createsWithAllFields() {
            var brief = new TopicBrief(
                    "Apache Kafka",
                    "developers",
                    1500,
                    List.of("Introduction", "Core Concepts"),
                    List.of("EventStreaming"),
                    List.of("https://kafka.apache.org")
            );

            assertEquals("Apache Kafka", brief.topic());
            assertEquals("developers", brief.targetAudience());
            assertEquals(1500, brief.targetWordCount());
            assertEquals(2, brief.requiredSections().size());
            assertEquals(1, brief.relatedPages().size());
            assertEquals(1, brief.sourceUrls().size());
        }

        @Test
        @DisplayName("Creates simple topic brief")
        void createsSimpleTopicBrief() {
            var brief = TopicBrief.simple("Apache Kafka", "developers", 1500);

            assertEquals("Apache Kafka", brief.topic());
            assertEquals("developers", brief.targetAudience());
            assertEquals(1500, brief.targetWordCount());
            assertTrue(brief.requiredSections().isEmpty());
            assertTrue(brief.relatedPages().isEmpty());
            assertTrue(brief.sourceUrls().isEmpty());
        }

        @Test
        @DisplayName("Handles null lists by converting to empty lists")
        void handlesNullLists() {
            var brief = new TopicBrief(
                    "Topic",
                    "audience",
                    1000,
                    null,
                    null,
                    null
            );

            assertNotNull(brief.requiredSections());
            assertNotNull(brief.relatedPages());
            assertNotNull(brief.sourceUrls());
            assertTrue(brief.requiredSections().isEmpty());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Throws on null topic")
        void throwsOnNullTopic() {
            assertThrows(NullPointerException.class, () ->
                    new TopicBrief(null, "audience", 1000, List.of(), List.of(), List.of()));
        }

        @Test
        @DisplayName("Throws on blank topic")
        void throwsOnBlankTopic() {
            assertThrows(IllegalArgumentException.class, () ->
                    new TopicBrief("   ", "audience", 1000, List.of(), List.of(), List.of()));
        }

        @Test
        @DisplayName("Throws on negative word count")
        void throwsOnNegativeWordCount() {
            assertThrows(IllegalArgumentException.class, () ->
                    new TopicBrief("Topic", "audience", -100, List.of(), List.of(), List.of()));
        }

        @Test
        @DisplayName("Allows zero word count")
        void allowsZeroWordCount() {
            var brief = new TopicBrief("Topic", "audience", 0, List.of(), List.of(), List.of());
            assertEquals(0, brief.targetWordCount());
        }
    }

    @Nested
    @DisplayName("Immutability")
    class Immutability {

        @Test
        @DisplayName("Lists are immutable")
        void listsAreImmutable() {
            var brief = TopicBrief.simple("Topic", "audience", 1000);

            assertThrows(UnsupportedOperationException.class, () ->
                    brief.requiredSections().add("New Section"));

            assertThrows(UnsupportedOperationException.class, () ->
                    brief.relatedPages().add("NewPage"));

            assertThrows(UnsupportedOperationException.class, () ->
                    brief.sourceUrls().add("http://example.com"));
        }

        @Test
        @DisplayName("Defensive copy is made from input lists")
        void defensiveCopyIsMade() {
            var sections = new java.util.ArrayList<>(List.of("Section1"));
            var brief = new TopicBrief(
                    "Topic",
                    "audience",
                    1000,
                    sections,
                    List.of(),
                    List.of()
            );

            sections.add("Section2"); // Modify original list
            assertEquals(1, brief.requiredSections().size()); // Should still be 1
        }
    }
}
