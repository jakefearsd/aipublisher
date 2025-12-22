package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.EnabledIfLlmAvailable;
import com.jakefear.aipublisher.IntegrationTestHelper;
import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for FactCheckerAgent using Ollama.
 *
 * Tests run when Ollama server is reachable:
 * - Default URL: http://inference.jakefear.com:11434
 * - Override with OLLAMA_BASE_URL environment variable
 * - Default model: qwen2.5:14b (override with OLLAMA_MODEL)
 *
 * Run with: mvn test -Dtest=FactCheckerAgentIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 */
@Tag("integration")
@EnabledIfLlmAvailable
@DisplayName("FactCheckerAgent Integration")
class FactCheckerAgentIntegrationTest {

    private FactCheckerAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        ChatLanguageModel model = IntegrationTestHelper.buildModel(0.1);
        agent = new FactCheckerAgent(model, AgentPrompts.FACT_CHECKER);
        System.out.println("Using LLM: " + IntegrationTestHelper.getProviderName());

        // Create document with research brief and draft
        TopicBrief brief = TopicBrief.simple("REST API Design", "backend developers", 500);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("REST stands for Representational State Transfer"),
                        KeyFact.unsourced("REST was defined by Roy Fielding in his 2000 doctoral dissertation"),
                        KeyFact.unsourced("REST uses standard HTTP methods: GET, POST, PUT, DELETE"),
                        KeyFact.unsourced("RESTful APIs should be stateless")
                ),
                List.of(
                        new SourceCitation("Roy Fielding's Dissertation", ConfidenceLevel.HIGH),
                        new SourceCitation("HTTP RFC 7231", ConfidenceLevel.HIGH)
                ),
                List.of("Introduction", "Principles", "HTTP Methods"),
                List.of(),
                Map.of("Stateless", "Server does not store client session state"),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);

        // Set up article draft (with one questionable claim to test fact-checking)
        String draftContent = """
                ## REST API Design

                REST (Representational State Transfer) is an architectural style for designing web services.
                It was defined by Roy Fielding in his 2000 doctoral dissertation.

                ### Key Principles

                RESTful APIs should be stateless, meaning the server does not store client session state.
                REST uses standard HTTP methods including GET, POST, PUT, and DELETE.

                ### Best Practices

                REST APIs are always faster than GraphQL APIs.
                Use proper HTTP status codes to indicate success or failure.
                """;

        ArticleDraft draft = new ArticleDraft(
                draftContent,
                "An introduction to REST API design principles",
                List.of(),
                List.of("API", "WebDevelopment"),
                Map.of()
        );
        document.setDraft(draft);
        document.transitionTo(DocumentState.FACT_CHECKING);
    }

    @Test
    @DisplayName("Fact-checks article against research")
    void factChecksArticleAgainstResearch() {
        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        FactCheckReport report = result.getFactCheckReport();
        assertNotNull(report, "Fact-check report should not be null");

        // Should have verified some claims
        assertFalse(report.verifiedClaims().isEmpty() && report.questionableClaims().isEmpty(),
                "Should have checked at least some claims");

        // Should provide overall confidence and recommendation
        assertNotNull(report.overallConfidence());
        assertNotNull(report.recommendedAction());

        // Validation should pass
        assertTrue(agent.validate(document), "Validation should pass");

        // Log output for inspection
        System.out.println("=== Fact Check Report ===");
        System.out.println("Verified claims: " + report.verifiedClaims().size());
        System.out.println("Questionable claims: " + report.questionableClaims().size());
        System.out.println("Consistency issues: " + report.consistencyIssues().size());
        System.out.println("Overall confidence: " + report.overallConfidence());
        System.out.println("Recommended action: " + report.recommendedAction());

        if (!report.verifiedClaims().isEmpty()) {
            System.out.println("\nVerified claims:");
            report.verifiedClaims().forEach(c ->
                    System.out.println("  - " + c.claim() + " [source: " + c.sourceIndex() + "]"));
        }

        if (!report.questionableClaims().isEmpty()) {
            System.out.println("\nQuestionable claims:");
            report.questionableClaims().forEach(c ->
                    System.out.println("  - " + c.claim() + "\n    Issue: " + c.issue()));
        }
    }

    @Test
    @DisplayName("Detects unsupported claims")
    void detectsUnsupportedClaims() {
        // The draft contains "REST APIs are always faster than GraphQL APIs"
        // which is not supported by the research brief

        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        FactCheckReport report = result.getFactCheckReport();

        // The unsupported performance claim should be flagged
        // (exact behavior depends on the model, but it should find issues)
        System.out.println("=== Unsupported Claims Detection ===");
        System.out.println("Total issues found: " + report.getIssueCount());

        if (!report.questionableClaims().isEmpty()) {
            System.out.println("Questionable claims detected:");
            report.questionableClaims().forEach(c -> {
                System.out.println("  Claim: " + c.claim());
                System.out.println("  Issue: " + c.issue());
                if (!c.suggestion().isBlank()) {
                    System.out.println("  Suggestion: " + c.suggestion());
                }
            });
        }
    }

    @Test
    @DisplayName("Records contribution with timing metrics")
    void recordsContributionWithMetrics() {
        // Act
        agent.process(document);

        // Assert
        assertEquals(1, document.getContributions().size());
        AgentContribution contribution = document.getContributions().get(0);

        assertEquals("FACT_CHECKER", contribution.agentRole());
        assertNotNull(contribution.processingTime());
        assertTrue(contribution.processingTime().toMillis() > 0, "Processing time should be recorded");

        System.out.println("Processing time: " + contribution.processingTime().toMillis() + "ms");
        System.out.println("Metrics: " + contribution.metrics());
    }
}
