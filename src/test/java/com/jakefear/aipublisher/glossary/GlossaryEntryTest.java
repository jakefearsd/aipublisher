package com.jakefear.aipublisher.glossary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlossaryEntry")
class GlossaryEntryTest {

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("Creates simple entry")
        void createsSimpleEntry() {
            GlossaryEntry entry = GlossaryEntry.simple("API Gateway", "A service that handles API routing");

            assertEquals("API Gateway", entry.term());
            assertEquals("api gateway", entry.canonicalForm());
            assertEquals("A service that handles API routing", entry.definition());
            assertTrue(entry.primary());
        }

        @Test
        @DisplayName("Creates entry with category")
        void createsEntryWithCategory() {
            GlossaryEntry entry = GlossaryEntry.withCategory(
                    "Event Sourcing",
                    "A pattern for storing state changes as events",
                    "Architecture"
            );

            assertEquals("Event Sourcing", entry.term());
            assertEquals("Architecture", entry.category());
        }

        @Test
        @DisplayName("Normalizes canonical form to lowercase")
        void normalizesCanonicalForm() {
            GlossaryEntry entry = GlossaryEntry.simple("REST API", "An architectural style");

            assertEquals("rest api", entry.canonicalForm());
        }

        @Test
        @DisplayName("Throws on null term")
        void throwsOnNullTerm() {
            assertThrows(NullPointerException.class, () ->
                    GlossaryEntry.simple(null, "Definition")
            );
        }

        @Test
        @DisplayName("Throws on blank term")
        void throwsOnBlankTerm() {
            assertThrows(IllegalArgumentException.class, () ->
                    GlossaryEntry.simple("  ", "Definition")
            );
        }

        @Test
        @DisplayName("Throws on null definition")
        void throwsOnNullDefinition() {
            assertThrows(NullPointerException.class, () ->
                    GlossaryEntry.simple("Term", null)
            );
        }

        @Test
        @DisplayName("Throws on blank definition")
        void throwsOnBlankDefinition() {
            assertThrows(IllegalArgumentException.class, () ->
                    GlossaryEntry.simple("Term", "  ")
            );
        }
    }

    @Nested
    @DisplayName("Matching")
    class Matching {

        @Test
        @DisplayName("Matches canonical form")
        void matchesCanonicalForm() {
            GlossaryEntry entry = GlossaryEntry.simple("Event Sourcing", "A pattern");

            assertTrue(entry.matches("Event Sourcing"));
            assertTrue(entry.matches("event sourcing"));
            assertTrue(entry.matches("EVENT SOURCING"));
        }

        @Test
        @DisplayName("Matches aliases")
        void matchesAliases() {
            GlossaryEntry entry = new GlossaryEntry(
                    "Event Sourcing",
                    null,
                    "A pattern for storing state changes",
                    List.of("ES", "Event Store"),
                    null,
                    null,
                    List.of(),
                    true,
                    null
            );

            assertTrue(entry.matches("ES"));
            assertTrue(entry.matches("es"));
            assertTrue(entry.matches("Event Store"));
        }

        @Test
        @DisplayName("Does not match unrelated terms")
        void doesNotMatchUnrelated() {
            GlossaryEntry entry = GlossaryEntry.simple("Microservices", "An architecture pattern");

            assertFalse(entry.matches("Monolith"));
            assertFalse(entry.matches(""));
            assertFalse(entry.matches(null));
        }
    }

    @Nested
    @DisplayName("Wiki Integration")
    class WikiIntegration {

        @Test
        @DisplayName("Generates wiki link without spaces")
        void generatesWikiLinkWithoutSpaces() {
            GlossaryEntry entry = GlossaryEntry.simple("Event Driven Architecture", "A pattern");

            assertEquals("EventDrivenArchitecture", entry.getWikiLink());
        }

        @Test
        @DisplayName("Handles single word terms")
        void handlesSingleWordTerms() {
            GlossaryEntry entry = GlossaryEntry.simple("Kafka", "A message broker");

            assertEquals("Kafka", entry.getWikiLink());
        }
    }
}
