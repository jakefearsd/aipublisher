package com.jakefear.aipublisher.seealso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeeAlsoSection record.
 */
@DisplayName("SeeAlsoSection")
class SeeAlsoSectionTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid section")
        void createsValidSection() {
            List<SeeAlsoEntry> entries = List.of(
                    SeeAlsoEntry.related("Java", "Java", "Language"),
                    SeeAlsoEntry.broader("Programming", "Programming", "Broader topic")
            );

            SeeAlsoSection section = new SeeAlsoSection("Spring Boot", entries);

            assertEquals("Spring Boot", section.sourceTopic());
            assertEquals(2, section.size());
        }

        @Test
        @DisplayName("Rejects null topic")
        void rejectsNullTopic() {
            assertThrows(NullPointerException.class, () ->
                    new SeeAlsoSection(null, List.of()));
        }

        @Test
        @DisplayName("Rejects blank topic")
        void rejectsBlankTopic() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SeeAlsoSection("  ", List.of()));
        }

        @Test
        @DisplayName("Handles null entries list")
        void handlesNullEntriesList() {
            SeeAlsoSection section = new SeeAlsoSection("Topic", null);
            assertNotNull(section.entries());
            assertTrue(section.isEmpty());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        private SeeAlsoSection createTestSection() {
            return SeeAlsoSection.builder("Test Topic")
                    .addBroader("Programming", "Programming", "Broader")
                    .addNarrower("Java EE", "JavaEE", "Narrower")
                    .addRelated("Python", "Python", "Related")
                    .addTutorial("Getting Started", "GettingStarted", "Tutorial")
                    .addExternal("Official Docs", "https://example.com", "External")
                    .build();
        }

        @Test
        @DisplayName("getEntriesByType() returns correct entries")
        void getEntriesByTypeReturnsCorrectEntries() {
            SeeAlsoSection section = createTestSection();

            assertEquals(1, section.getEntriesByType(SeeAlsoType.BROADER).size());
            assertEquals(1, section.getEntriesByType(SeeAlsoType.NARROWER).size());
            assertEquals(1, section.getEntriesByType(SeeAlsoType.RELATED).size());
            assertEquals(1, section.getEntriesByType(SeeAlsoType.TUTORIAL).size());
            assertEquals(1, section.getEntriesByType(SeeAlsoType.EXTERNAL).size());
            assertEquals(0, section.getEntriesByType(SeeAlsoType.COMPARISON).size());
        }

        @Test
        @DisplayName("getMainEntries() returns broader, narrower, related")
        void getMainEntriesReturnsBroaderNarrowerRelated() {
            SeeAlsoSection section = createTestSection();

            List<SeeAlsoEntry> main = section.getMainEntries();

            assertEquals(3, main.size());
            assertTrue(main.stream().allMatch(e -> e.type().isMainSection()));
        }

        @Test
        @DisplayName("getInternalEntries() excludes external")
        void getInternalEntriesExcludesExternal() {
            SeeAlsoSection section = createTestSection();

            List<SeeAlsoEntry> internal = section.getInternalEntries();

            assertEquals(4, internal.size());
            assertTrue(internal.stream().allMatch(e -> e.type().isInternal()));
        }

        @Test
        @DisplayName("getExternalEntries() returns only external")
        void getExternalEntriesReturnsOnlyExternal() {
            SeeAlsoSection section = createTestSection();

            List<SeeAlsoEntry> external = section.getExternalEntries();

            assertEquals(1, external.size());
            assertTrue(external.stream().noneMatch(e -> e.type().isInternal()));
        }

        @Test
        @DisplayName("getEntriesByRelevance() sorts correctly")
        void getEntriesByRelevanceSortsCorrectly() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .add(SeeAlsoEntry.related("Low", "Low", "").withRelevance(0.3))
                    .add(SeeAlsoEntry.related("High", "High", "").withRelevance(0.9))
                    .add(SeeAlsoEntry.related("Medium", "Medium", "").withRelevance(0.6))
                    .build();

            List<SeeAlsoEntry> sorted = section.getEntriesByRelevance();

            assertEquals("High", sorted.get(0).title());
            assertEquals("Medium", sorted.get(1).title());
            assertEquals("Low", sorted.get(2).title());
        }

        @Test
        @DisplayName("getTopEntries() returns correct count")
        void getTopEntriesReturnsCorrectCount() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .add(SeeAlsoEntry.related("A", "A", "").withRelevance(0.9))
                    .add(SeeAlsoEntry.related("B", "B", "").withRelevance(0.8))
                    .add(SeeAlsoEntry.related("C", "C", "").withRelevance(0.7))
                    .add(SeeAlsoEntry.related("D", "D", "").withRelevance(0.6))
                    .build();

            List<SeeAlsoEntry> top2 = section.getTopEntries(2);

            assertEquals(2, top2.size());
            assertEquals("A", top2.get(0).title());
            assertEquals("B", top2.get(1).title());
        }

        @Test
        @DisplayName("isEmpty() returns correct value")
        void isEmptyReturnsCorrectValue() {
            assertTrue(SeeAlsoSection.empty("Topic").isEmpty());
            assertFalse(SeeAlsoSection.builder("Topic")
                    .addRelated("Java", "Java", "")
                    .build()
                    .isEmpty());
        }
    }

    @Nested
    @DisplayName("toWikiText()")
    class ToWikiText {

        @Test
        @DisplayName("Returns empty string for empty section")
        void returnsEmptyStringForEmptySection() {
            SeeAlsoSection section = SeeAlsoSection.empty("Topic");
            assertEquals("", section.toWikiText());
        }

        @Test
        @DisplayName("Includes section heading")
        void includesSectionHeading() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .addRelated("Java", "Java", "")
                    .build();

            String wiki = section.toWikiText();

            assertTrue(wiki.startsWith("!!! See Also"));
        }

        @Test
        @DisplayName("Groups entries by type when multiple types present")
        void groupsEntriesByTypeWhenMultipleTypesPresent() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .addBroader("Broad", "Broad", "")
                    .addRelated("Related", "Related", "")
                    .build();

            String wiki = section.toWikiText();

            assertTrue(wiki.contains("Broader Topics"));
            assertTrue(wiki.contains("Related Topics"));
        }

        @Test
        @DisplayName("Generates bullet list entries")
        void generatesBulletListEntries() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .addRelated("Java", "Java", "")
                    .build();

            String wiki = section.toWikiText();

            assertTrue(wiki.contains("* [Java]"));
        }
    }

    @Nested
    @DisplayName("toSimpleWikiText()")
    class ToSimpleWikiText {

        @Test
        @DisplayName("Does not include sub-headings")
        void doesNotIncludeSubHeadings() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .addBroader("Broad", "Broad", "")
                    .addRelated("Related", "Related", "")
                    .build();

            String wiki = section.toSimpleWikiText();

            assertFalse(wiki.contains("Broader Topics"));
            assertFalse(wiki.contains("Related Topics"));
            assertTrue(wiki.contains("!!! See Also"));
        }
    }

    @Nested
    @DisplayName("toPromptFormat()")
    class ToPromptFormat {

        @Test
        @DisplayName("Returns message for empty section")
        void returnsMessageForEmptySection() {
            SeeAlsoSection section = SeeAlsoSection.empty("Topic");
            assertTrue(section.toPromptFormat().contains("No suggested"));
        }

        @Test
        @DisplayName("Includes topic name")
        void includesTopicName() {
            SeeAlsoSection section = SeeAlsoSection.builder("Spring Boot")
                    .addRelated("Java", "Java", "")
                    .build();

            String prompt = section.toPromptFormat();

            assertTrue(prompt.contains("Spring Boot"));
        }

        @Test
        @DisplayName("Groups by type with descriptions")
        void groupsByTypeWithDescriptions() {
            SeeAlsoSection section = SeeAlsoSection.builder("Topic")
                    .addRelated("Java", "Java", "Programming language")
                    .build();

            String prompt = section.toPromptFormat();

            assertTrue(prompt.contains("Related topics"));
            assertTrue(prompt.contains("Java"));
            assertTrue(prompt.contains("Programming language"));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("Builds complete section")
        void buildsCompleteSection() {
            SeeAlsoSection section = SeeAlsoSection.builder("Spring Boot")
                    .addBroader("Java", "Java", "Language")
                    .addNarrower("Spring Security", "SpringSecurity", "Subtopic")
                    .addRelated("Django", "Django", "Similar framework")
                    .addTutorial("Getting Started", "GettingStarted", "Tutorial")
                    .addExternal("Spring.io", "https://spring.io", "Official site")
                    .add(SeeAlsoEntry.internal("API Docs", SeeAlsoType.REFERENCE, "ApiDocs", "Reference"))
                    .build();

            assertEquals("Spring Boot", section.sourceTopic());
            assertEquals(6, section.size());
        }
    }
}
