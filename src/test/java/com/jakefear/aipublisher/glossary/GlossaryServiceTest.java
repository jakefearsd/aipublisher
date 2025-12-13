package com.jakefear.aipublisher.glossary;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GlossaryService")
class GlossaryServiceTest {

    private GlossaryService service;

    @BeforeEach
    void setUp() {
        service = new GlossaryService();
    }

    @Nested
    @DisplayName("addEntry")
    class AddEntry {

        @Test
        @DisplayName("Adds new entry")
        void addsNewEntry() {
            GlossaryEntry entry = GlossaryEntry.simple("Kafka", "A distributed event streaming platform");

            GlossaryEntry existing = service.addEntry(entry);

            assertNull(existing);
            assertEquals(1, service.getEntryCount());
        }

        @Test
        @DisplayName("Returns existing entry when updating")
        void returnsExistingWhenUpdating() {
            GlossaryEntry first = GlossaryEntry.simple("Kafka", "Definition 1");
            GlossaryEntry second = GlossaryEntry.simple("Kafka", "Definition 2");

            service.addEntry(first);
            GlossaryEntry existing = service.addEntry(second);

            assertNotNull(existing);
            assertEquals("Definition 1", existing.definition());
            assertEquals(1, service.getEntryCount());
        }

        @Test
        @DisplayName("Indexes by category")
        void indexesByCategory() {
            service.addEntry(GlossaryEntry.withCategory("Kafka", "Event streaming", "Messaging"));
            service.addEntry(GlossaryEntry.withCategory("RabbitMQ", "Message broker", "Messaging"));
            service.addEntry(GlossaryEntry.withCategory("Docker", "Container runtime", "DevOps"));

            List<GlossaryEntry> messaging = service.getByCategory("Messaging");
            assertEquals(2, messaging.size());

            List<GlossaryEntry> devops = service.getByCategory("DevOps");
            assertEquals(1, devops.size());
        }
    }

    @Nested
    @DisplayName("addFromMap")
    class AddFromMap {

        @Test
        @DisplayName("Adds entries from map")
        void addsEntriesFromMap() {
            Map<String, String> glossary = Map.of(
                    "Kafka", "Event streaming platform",
                    "Zookeeper", "Distributed coordination service"
            );

            service.addFromMap(glossary, "KafkaArticle");

            assertEquals(2, service.getEntryCount());
            assertTrue(service.lookup("Kafka").isPresent());
            assertTrue(service.lookup("Zookeeper").isPresent());
        }

        @Test
        @DisplayName("Handles null map")
        void handlesNullMap() {
            service.addFromMap(null, "Article");

            assertEquals(0, service.getEntryCount());
        }
    }

    @Nested
    @DisplayName("lookup")
    class Lookup {

        @Test
        @DisplayName("Finds entry by exact term")
        void findsByExactTerm() {
            service.addEntry(GlossaryEntry.simple("Event Sourcing", "A pattern"));

            Optional<GlossaryEntry> result = service.lookup("Event Sourcing");

            assertTrue(result.isPresent());
            assertEquals("Event Sourcing", result.get().term());
        }

        @Test
        @DisplayName("Finds entry case-insensitively")
        void findsCaseInsensitively() {
            service.addEntry(GlossaryEntry.simple("CQRS", "Command Query Responsibility Segregation"));

            assertTrue(service.lookup("cqrs").isPresent());
            assertTrue(service.lookup("CQRS").isPresent());
            assertTrue(service.lookup("Cqrs").isPresent());
        }

        @Test
        @DisplayName("Finds entry by alias")
        void findsByAlias() {
            GlossaryEntry entry = new GlossaryEntry(
                    "Event Sourcing",
                    null,
                    "A pattern",
                    List.of("ES"),
                    null,
                    null,
                    List.of(),
                    true
            );
            service.addEntry(entry);

            Optional<GlossaryEntry> result = service.lookup("ES");

            assertTrue(result.isPresent());
            assertEquals("Event Sourcing", result.get().term());
        }

        @Test
        @DisplayName("Returns empty for unknown term")
        void returnsEmptyForUnknown() {
            service.addEntry(GlossaryEntry.simple("Kafka", "Definition"));

            assertTrue(service.lookup("RabbitMQ").isEmpty());
            assertTrue(service.lookup("").isEmpty());
            assertTrue(service.lookup(null).isEmpty());
        }
    }

    @Nested
    @DisplayName("getByCategory")
    class GetByCategory {

        @Test
        @DisplayName("Returns entries in category sorted alphabetically")
        void returnsEntriesSorted() {
            service.addEntry(GlossaryEntry.withCategory("Zookeeper", "Def", "Infrastructure"));
            service.addEntry(GlossaryEntry.withCategory("Kafka", "Def", "Infrastructure"));
            service.addEntry(GlossaryEntry.withCategory("API Gateway", "Def", "Infrastructure"));

            List<GlossaryEntry> entries = service.getByCategory("Infrastructure");

            assertEquals(3, entries.size());
            assertEquals("API Gateway", entries.get(0).term());
            assertEquals("Kafka", entries.get(1).term());
            assertEquals("Zookeeper", entries.get(2).term());
        }

        @Test
        @DisplayName("Returns empty list for unknown category")
        void returnsEmptyForUnknown() {
            service.addEntry(GlossaryEntry.withCategory("Kafka", "Def", "Messaging"));

            List<GlossaryEntry> entries = service.getByCategory("Unknown");

            assertTrue(entries.isEmpty());
        }

        @Test
        @DisplayName("Is case-insensitive for category lookup")
        void isCaseInsensitiveForCategory() {
            service.addEntry(GlossaryEntry.withCategory("Kafka", "Def", "Messaging"));

            assertEquals(1, service.getByCategory("messaging").size());
            assertEquals(1, service.getByCategory("MESSAGING").size());
        }
    }

    @Nested
    @DisplayName("findTermsInText")
    class FindTermsInText {

        @Test
        @DisplayName("Finds terms mentioned in text")
        void findsTermsInText() {
            service.addEntry(GlossaryEntry.simple("Kafka", "Event streaming"));
            service.addEntry(GlossaryEntry.simple("Docker", "Container runtime"));
            service.addEntry(GlossaryEntry.simple("Kubernetes", "Container orchestration"));

            List<GlossaryEntry> found = service.findTermsInText(
                    "We use Kafka for messaging and Docker for containerization.");

            assertEquals(2, found.size());
            assertTrue(found.stream().anyMatch(e -> e.term().equals("Docker")));
            assertTrue(found.stream().anyMatch(e -> e.term().equals("Kafka")));
        }

        @Test
        @DisplayName("Is case-insensitive")
        void isCaseInsensitive() {
            service.addEntry(GlossaryEntry.simple("API", "Application Programming Interface"));

            List<GlossaryEntry> found = service.findTermsInText("We expose a REST api.");

            assertEquals(1, found.size());
        }

        @Test
        @DisplayName("Finds by alias")
        void findsByAlias() {
            GlossaryEntry entry = new GlossaryEntry(
                    "Command Query Responsibility Segregation",
                    null,
                    "A pattern",
                    List.of("CQRS"),
                    null,
                    null,
                    List.of(),
                    true
            );
            service.addEntry(entry);

            List<GlossaryEntry> found = service.findTermsInText("We implement CQRS pattern.");

            assertEquals(1, found.size());
        }

        @Test
        @DisplayName("Returns empty for text with no matches")
        void returnsEmptyForNoMatches() {
            service.addEntry(GlossaryEntry.simple("Kafka", "Definition"));

            List<GlossaryEntry> found = service.findTermsInText("We use RabbitMQ for messaging.");

            assertTrue(found.isEmpty());
        }
    }

    @Nested
    @DisplayName("generateGlossarySection")
    class GenerateGlossarySection {

        @Test
        @DisplayName("Generates JSPWiki-formatted glossary")
        void generatesJspwikiGlossary() {
            List<GlossaryEntry> entries = List.of(
                    GlossaryEntry.simple("API", "Application Programming Interface"),
                    GlossaryEntry.simple("REST", "Representational State Transfer")
            );

            String section = service.generateGlossarySection(entries);

            assertTrue(section.contains("!! Glossary"));
            assertTrue(section.contains(";API"));
            assertTrue(section.contains(":Application Programming Interface"));
            assertTrue(section.contains(";REST"));
        }

        @Test
        @DisplayName("Returns empty string for empty list")
        void returnsEmptyForEmptyList() {
            String section = service.generateGlossarySection(List.of());

            assertEquals("", section);
        }

        @Test
        @DisplayName("Returns empty string for null")
        void returnsEmptyForNull() {
            String section = service.generateGlossarySection(null);

            assertEquals("", section);
        }
    }

    @Nested
    @DisplayName("getCategoryCounts")
    class GetCategoryCounts {

        @Test
        @DisplayName("Returns category counts")
        void returnsCategoryCounts() {
            service.addEntry(GlossaryEntry.withCategory("Kafka", "Def", "Messaging"));
            service.addEntry(GlossaryEntry.withCategory("RabbitMQ", "Def", "Messaging"));
            service.addEntry(GlossaryEntry.withCategory("Docker", "Def", "DevOps"));

            Map<String, Integer> counts = service.getCategoryCounts();

            assertEquals(2, counts.size());
            assertEquals(2, counts.get("messaging"));
            assertEquals(1, counts.get("devops"));
        }
    }

    @Nested
    @DisplayName("getAllEntries")
    class GetAllEntries {

        @Test
        @DisplayName("Returns all entries sorted alphabetically")
        void returnsAllEntriesSorted() {
            service.addEntry(GlossaryEntry.simple("Zookeeper", "Def"));
            service.addEntry(GlossaryEntry.simple("Kafka", "Def"));
            service.addEntry(GlossaryEntry.simple("API", "Def"));

            List<GlossaryEntry> all = service.getAllEntries();

            assertEquals(3, all.size());
            assertEquals("API", all.get(0).term());
            assertEquals("Kafka", all.get(1).term());
            assertEquals("Zookeeper", all.get(2).term());
        }
    }

    @Nested
    @DisplayName("clear")
    class Clear {

        @Test
        @DisplayName("Removes all entries")
        void removesAllEntries() {
            service.addEntry(GlossaryEntry.simple("Kafka", "Def"));
            service.addEntry(GlossaryEntry.withCategory("Docker", "Def", "DevOps"));

            service.clear();

            assertEquals(0, service.getEntryCount());
            assertTrue(service.getAllEntries().isEmpty());
            assertTrue(service.getCategoryCounts().isEmpty());
        }
    }
}
