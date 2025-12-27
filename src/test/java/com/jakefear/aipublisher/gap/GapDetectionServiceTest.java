package com.jakefear.aipublisher.gap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.config.OutputProperties;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GapDetectionService.
 */
class GapDetectionServiceTest {

    @TempDir
    Path tempDir;

    private OutputProperties outputProperties;
    private ChatModel mockModel;
    private ObjectMapper objectMapper;
    private GapDetectionService service;

    @BeforeEach
    void setUp() {
        outputProperties = mock(OutputProperties.class);
        when(outputProperties.getDirectoryPath()).thenReturn(tempDir);
        when(outputProperties.getFileExtension()).thenReturn(".txt");

        mockModel = mock(ChatModel.class);
        objectMapper = new ObjectMapper();

        service = new GapDetectionService(outputProperties, mockModel, objectMapper);
    }

    @Test
    void extractLinksFromContent_simpleLinks() {
        String content = """
                This article mentions [CompoundInterest] and [PresentValue].
                See also [Time Value of Money|TimeValueOfMoney] for more info.
                """;

        Set<String> links = service.extractLinksFromContent(content);

        assertEquals(3, links.size());
        assertTrue(links.contains("CompoundInterest"));
        assertTrue(links.contains("PresentValue"));
        assertTrue(links.contains("TimeValueOfMoney"));
    }

    @Test
    void extractLinksFromContent_ignoresExternalUrls() {
        String content = """
                Read more at [https://example.com/docs].
                Also see [http://test.com] and [mailto:test@example.com].
                But include [InternalPage].
                """;

        Set<String> links = service.extractLinksFromContent(content);

        assertEquals(1, links.size());
        assertTrue(links.contains("InternalPage"));
    }

    @Test
    void extractLinksFromContent_ignoresDirectives() {
        String content = """
                [{SET categories='Finance,Investing'}]
                [{INSERT SomePlugin}]
                [{TableOfContents}]
                But include [ValidLink].
                """;

        Set<String> links = service.extractLinksFromContent(content);

        assertEquals(1, links.size());
        assertTrue(links.contains("ValidLink"));
    }

    @Test
    void extractLinksFromContent_ignoresCategoryLinks() {
        String content = """
                [Category:Finance]
                [Wikipedia:Compound Interest]
                But include [FinancialConcepts].
                """;

        Set<String> links = service.extractLinksFromContent(content);

        assertEquals(1, links.size());
        assertTrue(links.contains("FinancialConcepts"));
    }

    @Test
    void getExistingPages_returnsPageNamesWithoutExtension() throws IOException {
        // Create test files
        Files.writeString(tempDir.resolve("CompoundInterest.txt"), "content");
        Files.writeString(tempDir.resolve("PresentValue.txt"), "content");
        Files.writeString(tempDir.resolve("other.md"), "should be ignored");

        Set<String> pages = service.getExistingPages(tempDir, ".txt");

        assertEquals(2, pages.size());
        assertTrue(pages.contains("CompoundInterest"));
        assertTrue(pages.contains("PresentValue"));
        assertFalse(pages.contains("other"));
    }

    @Test
    void extractAllLinks_mapsLinksToSources() throws IOException {
        // Create test wiki files
        String article1 = """
                !!! Investing Basics
                Learn about [CompoundInterest] and [Risk].
                """;
        String article2 = """
                !!! Advanced Topics
                Deep dive into [CompoundInterest] and [Diversification].
                """;

        Files.writeString(tempDir.resolve("InvestingBasics.txt"), article1);
        Files.writeString(tempDir.resolve("AdvancedTopics.txt"), article2);

        Map<String, Set<String>> linkToSources = service.extractAllLinks(tempDir, ".txt");

        // CompoundInterest is referenced by both
        assertTrue(linkToSources.containsKey("CompoundInterest"));
        assertEquals(2, linkToSources.get("CompoundInterest").size());

        // Risk is only in article 1
        assertTrue(linkToSources.containsKey("Risk"));
        assertEquals(1, linkToSources.get("Risk").size());
        assertTrue(linkToSources.get("Risk").contains("InvestingBasics"));

        // Diversification is only in article 2
        assertTrue(linkToSources.containsKey("Diversification"));
        assertEquals(1, linkToSources.get("Diversification").size());
        assertTrue(linkToSources.get("Diversification").contains("AdvancedTopics"));
    }

    @Test
    void findGaps_detectsMissingPages() {
        Map<String, Set<String>> linkToSources = Map.of(
                "ExistingPage", Set.of("Source1"),
                "MissingPage", Set.of("Source1", "Source2"),
                "AnotherMissing", Set.of("Source2")
        );
        Set<String> existingPages = Set.of("ExistingPage");

        List<GapConcept> gaps = service.findGaps(linkToSources, existingPages);

        assertEquals(2, gaps.size());

        List<String> gapNames = gaps.stream().map(GapConcept::name).toList();
        assertTrue(gapNames.contains("MissingPage"));
        assertTrue(gapNames.contains("AnotherMissing"));

        // Verify MissingPage has correct references
        GapConcept missingPage = gaps.stream()
                .filter(g -> g.name().equals("MissingPage"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, missingPage.referencedBy().size());
    }

    @Test
    void findGaps_normalizedComparison() {
        // Link uses spaces, page uses CamelCase
        Map<String, Set<String>> linkToSources = Map.of(
                "compound interest", Set.of("Source1")
        );
        // Existing page without spaces
        Set<String> existingPages = Set.of("CompoundInterest");

        List<GapConcept> gaps = service.findGaps(linkToSources, existingPages);

        // Should recognize these as the same (after normalization)
        // and possibly create a redirect
        assertTrue(gaps.isEmpty() || gaps.stream().allMatch(g -> g.type() == GapType.REDIRECT));
    }

    @Test
    void categorizeGaps_parsesLlmResponse() {
        // Mock LLM response
        String llmResponse = """
                [
                  {"name": "Present Value", "type": "DEFINITION", "category": "Finance"},
                  {"name": "compound interest", "type": "REDIRECT", "redirectTarget": "CompoundInterest", "category": ""},
                  {"name": "money", "type": "IGNORE", "category": ""}
                ]
                """;

        when(mockModel.chat(anyString())).thenReturn(llmResponse);

        List<GapConcept> originalGaps = List.of(
                GapConcept.of("Present Value", GapType.DEFINITION),
                GapConcept.of("compound interest", GapType.DEFINITION),
                GapConcept.of("money", GapType.DEFINITION)
        );

        List<GapConcept> categorized = service.categorizeGaps(originalGaps, "Finance");

        assertEquals(3, categorized.size());

        // Verify Present Value is categorized as DEFINITION
        GapConcept presentValue = categorized.stream()
                .filter(g -> g.name().equals("Present Value"))
                .findFirst()
                .orElseThrow();
        assertEquals(GapType.DEFINITION, presentValue.type());
        assertEquals("Finance", presentValue.category());

        // Verify compound interest is categorized as REDIRECT
        GapConcept compoundInterest = categorized.stream()
                .filter(g -> g.name().equals("compound interest"))
                .findFirst()
                .orElseThrow();
        assertEquals(GapType.REDIRECT, compoundInterest.type());
        assertEquals("CompoundInterest", compoundInterest.redirectTarget());

        // Verify money is categorized as IGNORE
        GapConcept money = categorized.stream()
                .filter(g -> g.name().equals("money"))
                .findFirst()
                .orElseThrow();
        assertEquals(GapType.IGNORE, money.type());
    }

    @Test
    void extractJsonArray_handlesMarkdownCodeBlocks() {
        String response = """
                Here's the categorization:
                ```json
                [{"name": "Test", "type": "DEFINITION"}]
                ```
                Done!
                """;

        String json = service.extractJsonArray(response);

        assertNotNull(json);
        assertTrue(json.startsWith("["));
        assertTrue(json.endsWith("]"));
    }

    @Test
    void detectGaps_endToEnd() throws IOException {
        // Create an existing page that references missing pages
        String content = """
                !!! Investing Guide
                Learn about [CompoundInterest], [PresentValue], and [RiskManagement].
                See also [TimeValueOfMoney|Time Value of Money].
                """;

        Files.writeString(tempDir.resolve("InvestingGuide.txt"), content);
        Files.writeString(tempDir.resolve("CompoundInterest.txt"), "Existing content");

        List<GapConcept> gaps = service.detectGaps();

        // CompoundInterest exists, so only 3 gaps
        assertEquals(3, gaps.size());

        List<String> gapNames = gaps.stream().map(GapConcept::name).toList();
        assertTrue(gapNames.contains("PresentValue"));
        assertTrue(gapNames.contains("RiskManagement"));
        assertTrue(gapNames.contains("Time Value of Money") || gapNames.contains("TimeValueOfMoney"));
    }

    // Tests for invalid page name filtering
    @Test
    void shouldSkipPageName_skipsNumericOnly() {
        assertTrue(service.shouldSkipPageName("1"));
        assertTrue(service.shouldSkipPageName("123"));
        assertTrue(service.shouldSkipPageName("42"));
    }

    @Test
    void shouldSkipPageName_skipsTooShort() {
        assertTrue(service.shouldSkipPageName("a"));
        assertTrue(service.shouldSkipPageName("ab"));
    }

    @Test
    void shouldSkipPageName_skipsCommonWords() {
        assertTrue(service.shouldSkipPageName("the"));
        assertTrue(service.shouldSkipPageName("and"));
        assertTrue(service.shouldSkipPageName("is"));
    }

    @Test
    void shouldSkipPageName_allowsValidNames() {
        assertFalse(service.shouldSkipPageName("CompoundInterest"));
        assertFalse(service.shouldSkipPageName("401k"));
        assertFalse(service.shouldSkipPageName("RothIRA"));
    }

    @Test
    void shouldSkipPageName_handlesNullAndBlank() {
        assertTrue(service.shouldSkipPageName(null));
        assertTrue(service.shouldSkipPageName(""));
        assertTrue(service.shouldSkipPageName("  "));
    }

    @Test
    void extractLinksFromContent_filtersInvalidNames() {
        String content = """
                See [1] and [2] for numbering.
                Also [the] and [CompoundInterest].
                """;

        Set<String> links = service.extractLinksFromContent(content);

        assertFalse(links.contains("1"));
        assertFalse(links.contains("2"));
        assertFalse(links.contains("the"));
        assertTrue(links.contains("CompoundInterest"));
    }

    // Tests for duplicate/fuzzy matching
    @Test
    void findCanonicalPage_findsExactNormalizedMatch() {
        Set<String> existingPages = Set.of("CompoundInterest", "PresentValue");

        // Different case, spaces
        assertEquals("CompoundInterest", service.findCanonicalPage("compound interest", existingPages));
        assertEquals("CompoundInterest", service.findCanonicalPage("compoundinterest", existingPages));
    }

    @Test
    void findCanonicalPage_findsAccentVariations() {
        Set<String> existingPages = Set.of("EstadoDaIndia");

        // Accented variations should match
        assertEquals("EstadoDaIndia", service.findCanonicalPage("EstadoDaÍndia", existingPages));
        assertEquals("EstadoDaIndia", service.findCanonicalPage("EstadoDaIndía", existingPages));
    }

    @Test
    void findCanonicalPage_findsNumberWordVariations() {
        Set<String> existingPages = Set.of("401kPlan");

        // Common variations of 401k
        String canonical = service.findCanonicalPage("401KPlan", existingPages);
        assertEquals("401kPlan", canonical);
    }

    @Test
    void findCanonicalPage_returnsNullForNoMatch() {
        Set<String> existingPages = Set.of("CompoundInterest", "PresentValue");

        assertNull(service.findCanonicalPage("TotallyDifferent", existingPages));
    }

    @Test
    void findGaps_createRedirectForNearDuplicates() {
        Map<String, Set<String>> linkToSources = Map.of(
                "compound-interest", Set.of("Source1"),
                "COMPOUND_INTEREST", Set.of("Source2")
        );
        Set<String> existingPages = Set.of("CompoundInterest");

        List<GapConcept> gaps = service.findGaps(linkToSources, existingPages);

        // Should create redirects, not new pages
        assertTrue(gaps.stream().allMatch(g -> g.type() == GapType.REDIRECT));
        for (GapConcept gap : gaps) {
            assertEquals("CompoundInterest", gap.redirectTarget());
        }
    }
}
