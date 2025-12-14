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
 * Integration tests for WriterAgent using the real Claude API.
 *
 * These tests are only run when the ANTHROPIC_API_KEY environment variable is set.
 * They make real API calls and consume tokens/credits.
 *
 * Run with: mvn test -Dtest=WriterAgentIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
@DisplayName("WriterAgent Integration")
class WriterAgentIntegrationTest {

    private WriterAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        ChatLanguageModel model = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-sonnet-4-20250514")
                .maxTokens(4096)
                .temperature(0.7)
                .build();

        agent = new WriterAgent(model, AgentPrompts.WRITER);

        // Create document with research brief
        TopicBrief brief = TopicBrief.simple("Git branching strategies", "software developers", 600);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief (from research phase)
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Git branching allows parallel development workflows"),
                        KeyFact.unsourced("Common strategies include GitFlow, GitHub Flow, and trunk-based development"),
                        KeyFact.unsourced("Feature branches isolate changes until ready to merge"),
                        KeyFact.unsourced("Pull requests enable code review before merging")
                ),
                List.of(new SourceCitation("Git Documentation", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Common Strategies", "Best Practices"),
                List.of("GitFlow", "PullRequest", "CodeReview"),
                Map.of("Feature Branch", "A branch created for developing a specific feature"),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);
    }

    @Test
    @DisplayName("Writes article from research brief")
    void writesArticleFromResearchBrief() {
        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        ArticleDraft draft = result.getDraft();
        assertNotNull(draft, "Draft should not be null");

        // Verify content was generated
        assertFalse(draft.wikiContent().isBlank(), "Should have wiki content");
        // JSPWiki uses ! for headings (!, !!, !!!)
        assertTrue(draft.wikiContent().contains("!"), "Should have JSPWiki headings");

        // Verify summary was generated
        assertFalse(draft.summary().isBlank(), "Should have summary");

        // Verify draft is valid
        assertTrue(draft.isValid(), "Draft should be valid");

        // Verify validation passes
        assertTrue(agent.validate(document), "Validation should pass");

        // Log output for inspection
        System.out.println("=== Article Draft ===");
        System.out.println("Word count: " + draft.estimateWordCount());
        System.out.println("Internal links: " + draft.internalLinks());
        System.out.println("Categories: " + draft.categories());
        System.out.println("\nSummary: " + draft.summary());
        System.out.println("\nContent preview (first 500 chars):");
        System.out.println(draft.wikiContent().substring(0, Math.min(500, draft.wikiContent().length())));
    }

    @Test
    @DisplayName("Records contribution with timing metrics")
    void recordsContributionWithMetrics() {
        // Act
        agent.process(document);

        // Assert
        assertEquals(1, document.getContributions().size());
        AgentContribution contribution = document.getContributions().get(0);

        assertEquals("WRITER", contribution.agentRole());
        assertNotNull(contribution.processingTime());
        assertTrue(contribution.processingTime().toMillis() > 0, "Processing time should be recorded");

        // Check metrics
        assertTrue(contribution.metrics().containsKey("responseLength"));
        assertTrue(contribution.metrics().containsKey("attempts"));

        System.out.println("Processing time: " + contribution.processingTime().toMillis() + "ms");
        System.out.println("Metrics: " + contribution.metrics());
    }
}
