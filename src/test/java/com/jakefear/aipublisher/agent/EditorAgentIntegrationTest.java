package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for EditorAgent using the real Claude API.
 *
 * These tests are only run when the ANTHROPIC_API_KEY environment variable is set.
 * They make real API calls and consume tokens/credits.
 *
 * Run with: mvn test -Dtest=EditorAgentIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@DisplayName("EditorAgent Integration")
class EditorAgentIntegrationTest {

    private EditorAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        ChatLanguageModel model = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-sonnet-4-20250514")
                .maxTokens(4096)
                .temperature(0.5)
                .build();

        agent = new EditorAgent(model, AgentPrompts.EDITOR);

        // Set up existing pages for link integration
        agent.setExistingPages(List.of(
                "VersionControl",
                "CodeReview",
                "ContinuousIntegration",
                "SoftwareDevelopment"
        ));

        // Create document with full pipeline state
        TopicBrief brief = TopicBrief.simple("Git basics", "developers new to version control", 400);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Git is a distributed version control system"),
                        KeyFact.unsourced("Created by Linus Torvalds in 2005"),
                        KeyFact.unsourced("Git tracks changes to files over time")
                ),
                List.of(new SourceCitation("Git Documentation", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Basic Commands"),
                List.of(),
                Map.of(),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);

        // Set up article draft (with some rough edges for editor to polish)
        String draftContent = """
                !!! Git Basics

                Git is a distributed version control system. It was created by Linus Torvalds in 2005.
                Git tracks changes to files over time.

                !! Basic Commands

                Use {{git init}} to create a new repository.
                Use {{git add}} to stage changes.
                Use {{git commit}} to save changes.

                Git is really important for software development and helps teams collaborate.
                """;

        ArticleDraft draft = new ArticleDraft(
                draftContent,
                "An introduction to Git version control basics",
                List.of(),
                List.of("VersionControl", "Tools"),
                Map.of()
        );
        document.setDraft(draft);
        document.transitionTo(DocumentState.FACT_CHECKING);

        // Set up fact-check report (approved with minor notes)
        FactCheckReport factCheckReport = new FactCheckReport(
                draftContent,
                List.of(
                        VerifiedClaim.verified("Git is a distributed version control system", 0),
                        VerifiedClaim.verified("Created by Linus Torvalds in 2005", 0)
                ),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        );
        document.setFactCheckReport(factCheckReport);
        document.transitionTo(DocumentState.EDITING);
    }

    @Test
    @DisplayName("Edits and polishes article for publication")
    void editsAndPolishesArticle() {
        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        FinalArticle article = result.getFinalArticle();
        assertNotNull(article, "Final article should not be null");

        // Verify content exists
        assertFalse(article.wikiContent().isBlank(), "Should have wiki content");

        // Verify metadata
        assertNotNull(article.metadata());
        assertNotNull(article.getTitle());

        // Verify quality score is reasonable
        assertTrue(article.qualityScore() >= 0.0 && article.qualityScore() <= 1.0,
                "Quality score should be between 0 and 1");

        // Verify edit summary exists
        assertNotNull(article.editSummary());
        assertFalse(article.editSummary().isBlank(), "Should have edit summary");

        // Validation should pass
        assertTrue(agent.validate(document), "Validation should pass");

        // Log output for inspection
        System.out.println("=== Final Article ===");
        System.out.println("Title: " + article.getTitle());
        System.out.println("Word count: " + article.estimateWordCount());
        System.out.println("Quality score: " + article.qualityScore());
        System.out.println("Edit summary: " + article.editSummary());
        System.out.println("Added links: " + article.addedLinks());
        System.out.println("\nContent:\n" + article.wikiContent());
    }

    @Test
    @DisplayName("Integrates links to existing pages")
    void integratesLinksToExistingPages() {
        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        FinalArticle article = result.getFinalArticle();

        // Should have tried to add relevant links
        System.out.println("=== Link Integration ===");
        System.out.println("Existing pages available: " + agent.getExistingPages());
        System.out.println("Added links: " + article.addedLinks());

        // Check if content contains any JSPWiki-style links
        String content = article.wikiContent();
        boolean hasWikiLinks = content.contains("[VersionControl]")
                || content.contains("[SoftwareDevelopment]")
                || content.contains("VersionControl")
                || content.contains("SoftwareDevelopment");

        System.out.println("Content contains JSPWiki-style links or page references: " + hasWikiLinks);
        System.out.println("\nContent preview:\n" + content);
    }

    @Test
    @DisplayName("Produces publication-ready quality")
    void producesPublicationReadyQuality() {
        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        FinalArticle article = result.getFinalArticle();

        // Should meet minimum quality threshold
        assertTrue(article.meetsQualityThreshold(0.7),
                "Should meet minimum quality threshold of 0.7");

        System.out.println("=== Quality Assessment ===");
        System.out.println("Quality score: " + article.qualityScore());
        System.out.println("Meets 0.7 threshold: " + article.meetsQualityThreshold(0.7));
        System.out.println("Meets 0.8 threshold: " + article.meetsQualityThreshold(0.8));
        System.out.println("Meets 0.9 threshold: " + article.meetsQualityThreshold(0.9));
    }

    @Test
    @DisplayName("Records contribution with timing metrics")
    void recordsContributionWithMetrics() {
        // Act
        agent.process(document);

        // Assert
        assertEquals(1, document.getContributions().size());
        AgentContribution contribution = document.getContributions().get(0);

        assertEquals("EDITOR", contribution.agentRole());
        assertNotNull(contribution.processingTime());
        assertTrue(contribution.processingTime().toMillis() > 0, "Processing time should be recorded");

        System.out.println("Processing time: " + contribution.processingTime().toMillis() + "ms");
        System.out.println("Metrics: " + contribution.metrics());
    }
}
