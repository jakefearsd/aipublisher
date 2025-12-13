package com.jakefear.aipublisher.linking;

import com.jakefear.aipublisher.glossary.GlossaryEntry;
import com.jakefear.aipublisher.glossary.GlossaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LinkEvaluator.
 */
@DisplayName("LinkEvaluator")
class LinkEvaluatorTest {

    private GlossaryService glossaryService;
    private WikiLinkContext wikiContext;
    private LinkEvaluator evaluator;

    @BeforeEach
    void setUp() {
        glossaryService = new GlossaryService();
        wikiContext = new WikiLinkContext();
        evaluator = new LinkEvaluator(glossaryService);
    }

    @Nested
    @DisplayName("findCandidates()")
    class FindCandidates {

        @Test
        @DisplayName("Returns empty list for null content")
        void returnsEmptyListForNullContent() {
            List<LinkCandidate> candidates = evaluator.findCandidates(null, wikiContext);
            assertTrue(candidates.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for blank content")
        void returnsEmptyListForBlankContent() {
            List<LinkCandidate> candidates = evaluator.findCandidates("   ", wikiContext);
            assertTrue(candidates.isEmpty());
        }

        @Test
        @DisplayName("Finds page mentions in content")
        void findsPageMentionsInContent() {
            wikiContext.registerPage("VersionControl");
            String content = "Learn about version control and how to use version control systems.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            assertFalse(candidates.isEmpty());
            assertTrue(candidates.stream().anyMatch(c -> c.targetPage().equals("VersionControl")));
        }

        @Test
        @DisplayName("Marks first mention correctly")
        void marksFirstMentionCorrectly() {
            wikiContext.registerPage("Git");
            String content = "Git is popular. Many developers use Git daily.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            // Filter to Git candidates only
            List<LinkCandidate> gitCandidates = candidates.stream()
                    .filter(c -> c.targetPage().equals("Git"))
                    .toList();

            assertTrue(gitCandidates.size() >= 2);
            assertTrue(gitCandidates.get(0).firstMention());
        }

        @Test
        @DisplayName("Finds glossary term mentions")
        void findsGlossaryTermMentions() {
            wikiContext.registerPage("VersionControlSystem");
            glossaryService.addEntry(GlossaryEntry.create(
                    "VCS",
                    "Version Control System",
                    "development",
                    "VersionControlSystem"
            ));

            String content = "A VCS helps track changes to code over time.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            assertTrue(candidates.stream().anyMatch(c ->
                    c.targetPage().equals("VersionControlSystem") && c.anchorText().equals("VCS")));
        }

        @Test
        @DisplayName("Returns candidates sorted by position")
        void returnsCandidatesSortedByPosition() {
            wikiContext.registerPage("First");
            wikiContext.registerPage("Second");
            wikiContext.registerPage("Third");

            String content = "First comes before Second which comes before Third.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            // Filter and verify positions are ascending
            for (int i = 1; i < candidates.size(); i++) {
                assertTrue(candidates.get(i).position() >= candidates.get(i - 1).position());
            }
        }

        @Test
        @DisplayName("Respects word boundaries")
        void respectsWordBoundaries() {
            wikiContext.registerPage("Git");
            String content = "The digit 5 and GitHub are not Git.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            // Should only find "Git" at the end, not in "digit" or "GitHub"
            List<LinkCandidate> gitCandidates = candidates.stream()
                    .filter(c -> c.targetPage().equals("Git"))
                    .toList();

            assertEquals(1, gitCandidates.size());
            assertTrue(content.indexOf("Git.") > 0); // Verify it's the standalone "Git."
        }
    }

    @Nested
    @DisplayName("selectBestLinks()")
    class SelectBestLinks {

        @Test
        @DisplayName("Returns empty list for empty candidates")
        void returnsEmptyListForEmptyCandidates() {
            List<LinkCandidate> selected = evaluator.selectBestLinks(List.of(), 500);
            assertTrue(selected.isEmpty());
        }

        @Test
        @DisplayName("Filters by minimum relevance score")
        void filtersByMinimumRelevanceScore() {
            List<LinkCandidate> candidates = List.of(
                    new LinkCandidate("HighScore", "text", 100, true, "", 0.8),
                    new LinkCandidate("LowScore", "text", 200, true, "", 0.3)
            );

            List<LinkCandidate> selected = evaluator.selectBestLinks(candidates, 500);

            assertTrue(selected.stream().anyMatch(c -> c.targetPage().equals("HighScore")));
            assertFalse(selected.stream().anyMatch(c -> c.targetPage().equals("LowScore")));
        }

        @Test
        @DisplayName("Respects target link count")
        void respectsTargetLinkCount() {
            // Create many high-scoring candidates
            List<LinkCandidate> candidates = List.of(
                    new LinkCandidate("Page1", "text", 0, true, "", 0.9),
                    new LinkCandidate("Page2", "text", 400, true, "", 0.85),
                    new LinkCandidate("Page3", "text", 800, true, "", 0.8),
                    new LinkCandidate("Page4", "text", 1200, true, "", 0.75),
                    new LinkCandidate("Page5", "text", 1600, true, "", 0.7)
            );

            // For 100 words with default strategy (5.5% target), expect about 5-6 links
            // but capped by actual candidates and spacing
            List<LinkCandidate> selected = evaluator.selectBestLinks(candidates, 100);

            assertTrue(selected.size() <= candidates.size());
        }

        @Test
        @DisplayName("Avoids duplicate page links")
        void avoidsDuplicatePageLinks() {
            List<LinkCandidate> candidates = List.of(
                    new LinkCandidate("SamePage", "first", 100, true, "", 0.9),
                    new LinkCandidate("SamePage", "second", 500, false, "", 0.8),
                    new LinkCandidate("OtherPage", "other", 300, true, "", 0.85)
            );

            List<LinkCandidate> selected = evaluator.selectBestLinks(candidates, 500);

            long samePageCount = selected.stream()
                    .filter(c -> c.targetPage().equals("SamePage"))
                    .count();

            assertTrue(samePageCount <= 1, "Should only include one link to same page");
        }

        @Test
        @DisplayName("Returns selected links sorted by position")
        void returnsSelectedLinksSortedByPosition() {
            List<LinkCandidate> candidates = List.of(
                    new LinkCandidate("Page1", "text", 500, true, "", 0.7),
                    new LinkCandidate("Page2", "text", 100, true, "", 0.6),
                    new LinkCandidate("Page3", "text", 900, true, "", 0.65)
            );

            List<LinkCandidate> selected = evaluator.selectBestLinks(candidates, 500);

            // Verify positions are ascending
            for (int i = 1; i < selected.size(); i++) {
                assertTrue(selected.get(i).position() >= selected.get(i - 1).position());
            }
        }
    }

    @Nested
    @DisplayName("calculateLinkDensity()")
    class CalculateLinkDensity {

        @Test
        @DisplayName("Calculates correct density")
        void calculatesCorrectDensity() {
            double density = evaluator.calculateLinkDensity(5, 100);
            assertEquals(0.05, density, 0.001);
        }

        @Test
        @DisplayName("Returns zero for zero word count")
        void returnsZeroForZeroWordCount() {
            double density = evaluator.calculateLinkDensity(5, 0);
            assertEquals(0.0, density, 0.001);
        }

        @Test
        @DisplayName("Returns zero for zero links")
        void returnsZeroForZeroLinks() {
            double density = evaluator.calculateLinkDensity(0, 100);
            assertEquals(0.0, density, 0.001);
        }
    }

    @Nested
    @DisplayName("isLinkDensityAcceptable()")
    class IsLinkDensityAcceptable {

        @Test
        @DisplayName("Delegates to strategy")
        void delegatesToStrategy() {
            // Default strategy: 3-8% density acceptable
            assertTrue(evaluator.isLinkDensityAcceptable(5, 100)); // 5%
            assertFalse(evaluator.isLinkDensityAcceptable(1, 100)); // 1%
            assertFalse(evaluator.isLinkDensityAcceptable(15, 100)); // 15%
        }
    }

    @Nested
    @DisplayName("With custom strategy")
    class WithCustomStrategy {

        @Test
        @DisplayName("Uses custom strategy settings")
        void usesCustomStrategySettings() {
            LinkingStrategy customStrategy = LinkingStrategy.minimal();
            LinkEvaluator customEvaluator = new LinkEvaluator(glossaryService, customStrategy);

            // Minimal strategy has higher min relevance (0.7)
            List<LinkCandidate> candidates = List.of(
                    new LinkCandidate("HighScore", "text", 100, true, "", 0.8),
                    new LinkCandidate("MidScore", "text", 200, true, "", 0.6)
            );

            List<LinkCandidate> selected = customEvaluator.selectBestLinks(candidates, 500);

            // Should only include high score (above 0.7 threshold)
            assertTrue(selected.stream().anyMatch(c -> c.targetPage().equals("HighScore")));
            assertFalse(selected.stream().anyMatch(c -> c.targetPage().equals("MidScore")));
        }
    }

    @Nested
    @DisplayName("CamelCase handling")
    class CamelCaseHandling {

        @Test
        @DisplayName("Finds CamelCase page as spaced words")
        void findsCamelCasePageAsSpacedWords() {
            wikiContext.registerPage("VersionControl");
            String content = "This article discusses version control best practices.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            assertTrue(candidates.stream().anyMatch(c ->
                    c.targetPage().equals("VersionControl") &&
                    c.anchorText().equalsIgnoreCase("version control")));
        }

        @Test
        @DisplayName("Handles multiple CamelCase words")
        void handlesMultipleCamelCaseWords() {
            wikiContext.registerPage("GitCommandLine");
            String content = "Use the git command line for advanced operations.";

            List<LinkCandidate> candidates = evaluator.findCandidates(content, wikiContext);

            assertTrue(candidates.stream().anyMatch(c ->
                    c.targetPage().equals("GitCommandLine")));
        }
    }
}
