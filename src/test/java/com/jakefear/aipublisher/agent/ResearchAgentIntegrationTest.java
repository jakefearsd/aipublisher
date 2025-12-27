package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.EnabledIfLlmAvailable;
import com.jakefear.aipublisher.IntegrationTestHelper;
import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ResearchAgent using Ollama.
 *
 * Tests run when Ollama server is reachable:
 * - Default URL: http://inference.jakefear.com:11434
 * - Override with OLLAMA_BASE_URL environment variable
 * - Default model: qwen3:14b (override with OLLAMA_MODEL)
 *
 * Run with: mvn test -Dtest=ResearchAgentIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 */
@Tag("integration")
@EnabledIfLlmAvailable
@DisplayName("ResearchAgent Integration")
class ResearchAgentIntegrationTest {

    private ResearchAgent agent;

    @BeforeEach
    void setUp() {
        ChatModel model = IntegrationTestHelper.buildModel(0.3);
        agent = new ResearchAgent(model, AgentPrompts.RESEARCH);
        System.out.println("Using LLM: " + IntegrationTestHelper.getProviderName());
    }

    @Test
    @DisplayName("Researches a simple technical topic")
    void researchesSimpleTechnicalTopic() {
        // Arrange
        TopicBrief brief = TopicBrief.simple(
                "Git branching strategies",
                "software developers",
                800
        );
        PublishingDocument document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        ResearchBrief researchBrief = result.getResearchBrief();
        assertNotNull(researchBrief, "Research brief should not be null");

        // Verify key facts were gathered
        assertFalse(researchBrief.keyFacts().isEmpty(), "Should have key facts");
        assertTrue(researchBrief.keyFacts().size() >= 3, "Should have at least 3 facts");

        // Verify outline was suggested
        assertFalse(researchBrief.suggestedOutline().isEmpty(), "Should have suggested outline");
        assertTrue(researchBrief.suggestedOutline().size() >= 2, "Should have at least 2 sections");

        // Verify validation passes
        assertTrue(agent.validate(document), "Validation should pass");

        // Log some output for inspection
        System.out.println("=== Research Brief ===");
        System.out.println("Key Facts: " + researchBrief.keyFacts().size());
        System.out.println("Sources: " + researchBrief.sources().size());
        System.out.println("Outline: " + researchBrief.suggestedOutline());
        System.out.println("Related Pages: " + researchBrief.relatedPageSuggestions());
    }

    @Test
    @DisplayName("Handles topic with specific requirements")
    void handlesTopicWithRequirements() {
        // Arrange
        TopicBrief brief = TopicBrief.builder("Apache Kafka")
                .targetAudience("developers new to event streaming")
                .targetWordCount(1200)
                .requiredSections(java.util.List.of("Introduction", "Core Concepts", "Use Cases"))
                .relatedPages(java.util.List.of("EventStreaming", "MessageQueue"))
                .build();
        PublishingDocument document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Act
        PublishingDocument result = agent.process(document);

        // Assert
        ResearchBrief researchBrief = result.getResearchBrief();
        assertNotNull(researchBrief);

        // Should have comprehensive research for a longer article
        assertTrue(researchBrief.keyFacts().size() >= 5,
                "Longer article should have more facts");

        // Should suggest related pages in CamelCase format
        for (String page : researchBrief.relatedPageSuggestions()) {
            // CamelCase pages shouldn't have spaces
            assertFalse(page.contains(" "),
                    "Page name should be CamelCase: " + page);
        }

        // Log for inspection
        System.out.println("=== Kafka Research Brief ===");
        System.out.println("Facts: " + researchBrief.keyFacts().size());
        researchBrief.keyFacts().forEach(f -> System.out.println("  - " + f.fact()));
        System.out.println("Outline: " + researchBrief.suggestedOutline());
    }

    @Test
    @DisplayName("Records contribution with timing metrics")
    void recordsContributionWithMetrics() {
        // Arrange
        TopicBrief brief = TopicBrief.simple("REST API design", "backend developers", 600);
        PublishingDocument document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Act
        agent.process(document);

        // Assert
        assertEquals(1, document.getContributions().size());
        AgentContribution contribution = document.getContributions().get(0);

        assertEquals("RESEARCHER", contribution.agentRole());
        assertNotNull(contribution.processingTime());
        assertTrue(contribution.processingTime().toMillis() > 0,
                "Processing time should be recorded");

        // Check metrics
        assertTrue(contribution.metrics().containsKey("responseLength"));
        assertTrue(contribution.metrics().containsKey("attempts"));

        System.out.println("Processing time: " + contribution.processingTime().toMillis() + "ms");
        System.out.println("Metrics: " + contribution.metrics());
    }
}
