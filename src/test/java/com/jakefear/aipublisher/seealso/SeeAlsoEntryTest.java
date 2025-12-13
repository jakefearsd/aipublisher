package com.jakefear.aipublisher.seealso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeeAlsoEntry record.
 */
@DisplayName("SeeAlsoEntry")
class SeeAlsoEntryTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid entry")
        void createsValidEntry() {
            SeeAlsoEntry entry = new SeeAlsoEntry(
                    "Spring Boot",
                    SeeAlsoType.RELATED,
                    "SpringBoot",
                    "Popular Java framework",
                    0.8
            );

            assertEquals("Spring Boot", entry.title());
            assertEquals(SeeAlsoType.RELATED, entry.type());
            assertEquals("SpringBoot", entry.wikiPage());
            assertEquals("Popular Java framework", entry.description());
            assertEquals(0.8, entry.relevanceScore());
        }

        @Test
        @DisplayName("Rejects null title")
        void rejectsNullTitle() {
            assertThrows(NullPointerException.class, () ->
                    new SeeAlsoEntry(null, SeeAlsoType.RELATED, "Page", "", 0.5));
        }

        @Test
        @DisplayName("Rejects blank title")
        void rejectsBlankTitle() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SeeAlsoEntry("  ", SeeAlsoType.RELATED, "Page", "", 0.5));
        }

        @Test
        @DisplayName("Rejects null type")
        void rejectsNullType() {
            assertThrows(NullPointerException.class, () ->
                    new SeeAlsoEntry("Title", null, "Page", "", 0.5));
        }

        @Test
        @DisplayName("Rejects invalid relevance score")
        void rejectsInvalidRelevanceScore() {
            assertThrows(IllegalArgumentException.class, () ->
                    new SeeAlsoEntry("Title", SeeAlsoType.RELATED, "Page", "", -0.1));
            assertThrows(IllegalArgumentException.class, () ->
                    new SeeAlsoEntry("Title", SeeAlsoType.RELATED, "Page", "", 1.1));
        }

        @Test
        @DisplayName("Handles null description")
        void handlesNullDescription() {
            SeeAlsoEntry entry = new SeeAlsoEntry("Title", SeeAlsoType.RELATED, "Page", null, 0.5);
            assertEquals("", entry.description());
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("related() creates related entry")
        void relatedCreatesRelatedEntry() {
            SeeAlsoEntry entry = SeeAlsoEntry.related("Spring", "Spring", "Framework");

            assertEquals("Spring", entry.title());
            assertEquals(SeeAlsoType.RELATED, entry.type());
            assertEquals("Spring", entry.wikiPage());
            assertEquals("Framework", entry.description());
        }

        @Test
        @DisplayName("broader() creates broader entry")
        void broaderCreatesBroaderEntry() {
            SeeAlsoEntry entry = SeeAlsoEntry.broader("Java", "Java", "Language");

            assertEquals(SeeAlsoType.BROADER, entry.type());
        }

        @Test
        @DisplayName("narrower() creates narrower entry")
        void narrowerCreatesNarrowerEntry() {
            SeeAlsoEntry entry = SeeAlsoEntry.narrower("Spring Boot", "SpringBoot", "Specific framework");

            assertEquals(SeeAlsoType.NARROWER, entry.type());
        }

        @Test
        @DisplayName("tutorial() creates tutorial entry")
        void tutorialCreatesTutorialEntry() {
            SeeAlsoEntry entry = SeeAlsoEntry.tutorial("Getting Started", "GettingStarted", "Tutorial");

            assertEquals(SeeAlsoType.TUTORIAL, entry.type());
        }

        @Test
        @DisplayName("external() creates external entry")
        void externalCreatesExternalEntry() {
            SeeAlsoEntry entry = SeeAlsoEntry.external("Official Docs", "https://example.com", "Documentation");

            assertEquals(SeeAlsoType.EXTERNAL, entry.type());
            assertEquals("https://example.com", entry.wikiPage());
        }

        @Test
        @DisplayName("internal() creates internal entry with type")
        void internalCreatesInternalEntryWithType() {
            SeeAlsoEntry entry = SeeAlsoEntry.internal("API Reference", SeeAlsoType.REFERENCE, "ApiRef", "Reference docs");

            assertEquals(SeeAlsoType.REFERENCE, entry.type());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("hasWikiPage() returns correct value")
        void hasWikiPageReturnsCorrectValue() {
            assertTrue(SeeAlsoEntry.related("Title", "Page", "").hasWikiPage());
            assertFalse(new SeeAlsoEntry("Title", SeeAlsoType.RELATED, null, "", 0.5).hasWikiPage());
            assertFalse(new SeeAlsoEntry("Title", SeeAlsoType.RELATED, "  ", "", 0.5).hasWikiPage());
        }

        @Test
        @DisplayName("hasDescription() returns correct value")
        void hasDescriptionReturnsCorrectValue() {
            assertTrue(SeeAlsoEntry.related("Title", "Page", "Description").hasDescription());
            assertFalse(SeeAlsoEntry.related("Title", "Page", "").hasDescription());
        }
    }

    @Nested
    @DisplayName("toWikiText()")
    class ToWikiText {

        @Test
        @DisplayName("Generates simple link when title matches wiki page")
        void generatesSimpleLinkWhenTitleMatchesWikiPage() {
            SeeAlsoEntry entry = SeeAlsoEntry.related("Java", "Java", "");
            assertEquals("[Java]", entry.toWikiText());
        }

        @Test
        @DisplayName("Generates simple link when normalized title matches wiki page")
        void generatesSimpleLinkWhenNormalizedTitleMatchesWikiPage() {
            // "Spring Boot" normalizes to "SpringBoot" which matches the wiki page
            SeeAlsoEntry entry = SeeAlsoEntry.related("Spring Boot", "SpringBoot", "");
            assertEquals("[SpringBoot]", entry.toWikiText());
        }

        @Test
        @DisplayName("Generates piped link when title really differs")
        void generatesPipedLinkWhenTitleReallyDiffers() {
            // Title and wiki page are truly different
            SeeAlsoEntry entry = SeeAlsoEntry.related("Spring Framework", "SpringBoot", "");
            assertEquals("[Spring Framework|SpringBoot]", entry.toWikiText());
        }

        @Test
        @DisplayName("Adds description after link")
        void addsDescriptionAfterLink() {
            SeeAlsoEntry entry = SeeAlsoEntry.related("Java", "Java", "Programming language");
            assertEquals("[Java] - Programming language", entry.toWikiText());
        }

        @Test
        @DisplayName("Returns plain title when no wiki page")
        void returnsPlainTitleWhenNoWikiPage() {
            SeeAlsoEntry entry = new SeeAlsoEntry("Some Topic", SeeAlsoType.RELATED, null, "", 0.5);
            assertEquals("Some Topic", entry.toWikiText());
        }

        @Test
        @DisplayName("Generates external link correctly")
        void generatesExternalLinkCorrectly() {
            SeeAlsoEntry entry = SeeAlsoEntry.external("Official Site", "https://example.com", "");
            assertEquals("[Official Site|https://example.com]", entry.toWikiText());
        }
    }

    @Nested
    @DisplayName("withRelevance()")
    class WithRelevance {

        @Test
        @DisplayName("Creates copy with new relevance score")
        void createsCopyWithNewRelevanceScore() {
            SeeAlsoEntry original = SeeAlsoEntry.related("Java", "Java", "Language");
            SeeAlsoEntry updated = original.withRelevance(0.9);

            assertEquals(0.5, original.relevanceScore());
            assertEquals(0.9, updated.relevanceScore());
            assertEquals(original.title(), updated.title());
            assertEquals(original.type(), updated.type());
        }
    }
}
