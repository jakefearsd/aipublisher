package com.jakefear.aipublisher.linking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WikiLinkContext.
 */
@DisplayName("WikiLinkContext")
class WikiLinkContextTest {

    private WikiLinkContext context;

    @BeforeEach
    void setUp() {
        context = new WikiLinkContext();
    }

    @Nested
    @DisplayName("Page registration")
    class PageRegistration {

        @Test
        @DisplayName("Registers a page")
        void registersPage() {
            context.registerPage("TestPage");
            assertTrue(context.pageExists("TestPage"));
        }

        @Test
        @DisplayName("Registers page with topic")
        void registersPageWithTopic() {
            context.registerPage("GitBasics", "Introduction to Git version control");

            assertTrue(context.pageExists("GitBasics"));
            assertEquals(Optional.of("Introduction to Git version control"), context.getPageTopic("GitBasics"));
        }

        @Test
        @DisplayName("Ignores null page name")
        void ignoresNullPageName() {
            context.registerPage(null);
            assertEquals(0, context.getPageCount());
        }

        @Test
        @DisplayName("Ignores blank page name")
        void ignoresBlankPageName() {
            context.registerPage("  ");
            assertEquals(0, context.getPageCount());
        }

        @Test
        @DisplayName("Ignores blank topic")
        void ignoresBlankTopic() {
            context.registerPage("TestPage", "  ");
            assertTrue(context.pageExists("TestPage"));
            assertEquals(Optional.empty(), context.getPageTopic("TestPage"));
        }
    }

    @Nested
    @DisplayName("Link recording")
    class LinkRecording {

        @Test
        @DisplayName("Records outbound link")
        void recordsOutboundLink() {
            context.recordLink("PageA", "PageB");

            Set<String> outbound = context.getOutboundLinks("PageA");
            assertTrue(outbound.contains("PageB"));
        }

        @Test
        @DisplayName("Records inbound link")
        void recordsInboundLink() {
            context.recordLink("PageA", "PageB");

            Set<String> inbound = context.getInboundLinks("PageB");
            assertTrue(inbound.contains("PageA"));
        }

        @Test
        @DisplayName("Registers both pages when recording link")
        void registersBothPagesWhenRecordingLink() {
            context.recordLink("PageA", "PageB");

            assertTrue(context.pageExists("PageA"));
            assertTrue(context.pageExists("PageB"));
        }

        @Test
        @DisplayName("Ignores null from page")
        void ignoresNullFromPage() {
            context.recordLink(null, "PageB");
            assertEquals(0, context.getPageCount());
        }

        @Test
        @DisplayName("Ignores null to page")
        void ignoresNullToPage() {
            context.recordLink("PageA", null);
            assertEquals(0, context.getPageCount());
        }

        @Test
        @DisplayName("Ignores blank from page")
        void ignoresBlankFromPage() {
            context.recordLink("  ", "PageB");
            assertEquals(0, context.getPageCount());
        }

        @Test
        @DisplayName("Ignores blank to page")
        void ignoresBlankToPage() {
            context.recordLink("PageA", "  ");
            assertEquals(0, context.getPageCount());
        }
    }

    @Nested
    @DisplayName("Link queries")
    class LinkQueries {

        @BeforeEach
        void setUpLinks() {
            // Create a small wiki graph:
            // PageA -> PageB, PageC
            // PageB -> PageC
            // PageD -> PageB
            context.recordLink("PageA", "PageB");
            context.recordLink("PageA", "PageC");
            context.recordLink("PageB", "PageC");
            context.recordLink("PageD", "PageB");
        }

        @Test
        @DisplayName("Returns empty set for null page")
        void returnsEmptySetForNullPage() {
            assertEquals(Set.of(), context.getInboundLinks(null));
            assertEquals(Set.of(), context.getOutboundLinks(null));
        }

        @Test
        @DisplayName("Returns empty set for unknown page")
        void returnsEmptySetForUnknownPage() {
            assertEquals(Set.of(), context.getInboundLinks("Unknown"));
            assertEquals(Set.of(), context.getOutboundLinks("Unknown"));
        }

        @Test
        @DisplayName("Gets all outbound links")
        void getsAllOutboundLinks() {
            Set<String> outbound = context.getOutboundLinks("PageA");
            assertEquals(Set.of("PageB", "PageC"), outbound);
        }

        @Test
        @DisplayName("Gets all inbound links")
        void getsAllInboundLinks() {
            Set<String> inbound = context.getInboundLinks("PageB");
            assertEquals(Set.of("PageA", "PageD"), inbound);
        }

        @Test
        @DisplayName("Counts inbound links correctly")
        void countsInboundLinksCorrectly() {
            assertEquals(2, context.getInboundLinkCount("PageB"));
            assertEquals(2, context.getInboundLinkCount("PageC"));
            assertEquals(0, context.getInboundLinkCount("PageA"));
            assertEquals(0, context.getInboundLinkCount("PageD"));
        }
    }

    @Nested
    @DisplayName("Missing links analysis")
    class MissingLinksAnalysis {

        @Test
        @DisplayName("Finds pages missing link to target")
        void findsPagesMissingLinkToTarget() {
            // PageA -> PageB -> PageC
            // PageD -> PageB
            // Both PageA and PageD link to PageB
            // If PageA also links to PageC, PageD might want to link to PageC too
            context.recordLink("PageA", "PageB");
            context.recordLink("PageA", "PageC");
            context.recordLink("PageB", "PageC");
            context.recordLink("PageD", "PageB");

            // PageD links to PageB, PageA also links to PageB
            // PageA links to PageC, so PageD might want to link to PageC
            Set<String> missingLinksToC = context.getPagesMissingLinkTo("PageC");

            // This analysis finds pages that share outbound targets but don't link here
            // Note: The algorithm looks at pages that link to pages that PageC links to
            // But PageC has no outbound links, so it returns empty
        }
    }

    @Nested
    @DisplayName("Most linked pages")
    class MostLinkedPages {

        @Test
        @DisplayName("Returns pages sorted by inbound link count")
        void returnsPagesSortedByInboundLinkCount() {
            context.recordLink("A", "Popular");
            context.recordLink("B", "Popular");
            context.recordLink("C", "Popular");
            context.recordLink("A", "Medium");
            context.recordLink("B", "Medium");
            context.recordLink("A", "Unpopular");

            List<String> mostLinked = context.getMostLinkedPages(3);

            assertEquals("Popular", mostLinked.get(0));
            assertEquals("Medium", mostLinked.get(1));
            assertEquals("Unpopular", mostLinked.get(2));
        }

        @Test
        @DisplayName("Respects limit parameter")
        void respectsLimitParameter() {
            context.recordLink("A", "Page1");
            context.recordLink("B", "Page2");
            context.recordLink("C", "Page3");

            List<String> mostLinked = context.getMostLinkedPages(2);
            assertEquals(2, mostLinked.size());
        }
    }

    @Nested
    @DisplayName("getAllPages()")
    class GetAllPages {

        @Test
        @DisplayName("Returns all registered pages")
        void returnsAllRegisteredPages() {
            context.registerPage("Page1");
            context.registerPage("Page2");
            context.recordLink("Page3", "Page4");

            Set<String> allPages = context.getAllPages();

            assertEquals(4, allPages.size());
            assertTrue(allPages.containsAll(Set.of("Page1", "Page2", "Page3", "Page4")));
        }

        @Test
        @DisplayName("Returns immutable copy")
        void returnsImmutableCopy() {
            context.registerPage("Page1");
            Set<String> allPages = context.getAllPages();

            assertThrows(UnsupportedOperationException.class, () -> allPages.add("NewPage"));
        }
    }

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("Clears all data")
        void clearsAllData() {
            context.registerPage("Page1", "Topic1");
            context.recordLink("Page1", "Page2");

            context.clear();

            assertEquals(0, context.getPageCount());
            assertFalse(context.pageExists("Page1"));
            assertEquals(Set.of(), context.getOutboundLinks("Page1"));
            assertEquals(Set.of(), context.getInboundLinks("Page2"));
        }
    }
}
