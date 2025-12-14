package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EditorAgent")
class EditorAgentTest {

    @Mock
    private ChatLanguageModel mockModel;

    private EditorAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        agent = new EditorAgent(mockModel, AgentPrompts.EDITOR);
        TopicBrief brief = TopicBrief.simple("Apache Kafka", "developers", 1000);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Kafka is a distributed streaming platform"),
                        KeyFact.unsourced("Created at LinkedIn in 2011")
                ),
                List.of(new SourceCitation("Apache Kafka Documentation", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Core Concepts"),
                List.of(),
                Map.of(),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);

        // Set up article draft
        ArticleDraft draft = new ArticleDraft(
                "!!! Apache Kafka\n\nApache Kafka is a distributed streaming platform.",
                "An introduction to Apache Kafka",
                List.of(),
                List.of("Technology"),
                Map.of()
        );
        document.setDraft(draft);
        document.transitionTo(DocumentState.FACT_CHECKING);

        // Set up fact-check report (prerequisite for editor)
        FactCheckReport factCheckReport = new FactCheckReport(
                draft.wikiContent(),
                List.of(VerifiedClaim.verified("Kafka is a distributed streaming platform", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        );
        document.setFactCheckReport(factCheckReport);
        document.transitionTo(DocumentState.EDITING);
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("Has EDITOR role")
        void hasEditorRole() {
            assertEquals(AgentRole.EDITOR, agent.getRole());
        }

        @Test
        @DisplayName("Has correct display name")
        void hasCorrectDisplayName() {
            assertEquals("Editor Agent", agent.getName());
        }
    }

    @Nested
    @DisplayName("Existing Pages Management")
    class ExistingPagesManagement {

        @Test
        @DisplayName("Starts with empty existing pages list")
        void startsWithEmptyExistingPages() {
            assertTrue(agent.getExistingPages().isEmpty());
        }

        @Test
        @DisplayName("Can set existing pages")
        void canSetExistingPages() {
            agent.setExistingPages(List.of("EventStreaming", "MessageQueue", "ApacheZookeeper"));

            assertEquals(3, agent.getExistingPages().size());
            assertTrue(agent.getExistingPages().contains("EventStreaming"));
        }

        @Test
        @DisplayName("Returns immutable copy of existing pages")
        void returnsImmutableCopy() {
            agent.setExistingPages(List.of("TestPage"));

            assertThrows(UnsupportedOperationException.class, () ->
                    agent.getExistingPages().add("NewPage"));
        }

        @Test
        @DisplayName("Handles null existing pages")
        void handlesNullExistingPages() {
            agent.setExistingPages(null);

            assertTrue(agent.getExistingPages().isEmpty());
        }
    }

    @Nested
    @DisplayName("JSON Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Parses valid JSON response")
        void parsesValidJsonResponse() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Apache Kafka\\n\\nApache Kafka is a distributed event streaming platform...",
                      "metadata": {
                        "title": "Apache Kafka",
                        "summary": "An introduction to Apache Kafka for developers",
                        "author": "AI Publisher"
                      },
                      "editSummary": "Improved clarity and added section headings",
                      "qualityScore": 0.92,
                      "addedLinks": ["EventStreaming", "MessageQueue"]
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FinalArticle article = result.getFinalArticle();
            assertNotNull(article);
            assertTrue(article.wikiContent().contains("Apache Kafka"));
            assertEquals("Apache Kafka", article.getTitle());
            assertEquals(0.92, article.qualityScore(), 0.001);
            assertEquals(2, article.addedLinks().size());
            assertEquals("Improved clarity and added section headings", article.editSummary());
        }

        @Test
        @DisplayName("Handles JSON wrapped in code blocks")
        void handlesCodeBlockWrappedJson() {
            String jsonResponse = """
                    ```json
                    {
                      "wikiContent": "!!! Test Article\\n\\nPolished content.",
                      "metadata": {"title": "Test", "summary": "Test summary"},
                      "editSummary": "Minor edits",
                      "qualityScore": 0.85,
                      "addedLinks": []
                    }
                    ```
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            assertNotNull(result.getFinalArticle());
            assertTrue(result.getFinalArticle().wikiContent().contains("Test Article"));
        }

        @Test
        @DisplayName("Handles minimal valid response")
        void handlesMinimalResponse() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Minimal\\n\\nContent.",
                      "qualityScore": 0.8
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FinalArticle article = result.getFinalArticle();
            assertNotNull(article);
            assertTrue(article.addedLinks().isEmpty());
            assertNotNull(article.metadata());
        }

        @Test
        @DisplayName("Clamps quality score to valid range")
        void clampsQualityScoreToValidRange() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent.",
                      "qualityScore": 1.5
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();

            assertEquals(1.0, article.qualityScore(), 0.001);
        }

        @Test
        @DisplayName("Throws on missing wiki content")
        void throwsOnMissingWikiContent() {
            String jsonResponse = """
                    {
                      "wikiContent": "",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            assertThrows(AgentException.class, () -> agent.process(document));
        }

        @Test
        @DisplayName("Throws on invalid JSON")
        void throwsOnInvalidJson() {
            when(mockModel.generate(anyString())).thenReturn("Not JSON");

            assertThrows(AgentException.class, () -> agent.process(document));
        }
    }

    @Nested
    @DisplayName("Quality Score Handling")
    class QualityScoreHandling {

        @Test
        @DisplayName("Parses high quality score")
        void parsesHighQualityScore() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nExcellent content.",
                      "qualityScore": 0.95
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();
            assertEquals(0.95, article.qualityScore(), 0.001);
            assertTrue(article.meetsQualityThreshold(0.9));
        }

        @Test
        @DisplayName("Parses low quality score")
        void parsesLowQualityScore() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nBasic content.",
                      "qualityScore": 0.65
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();
            assertEquals(0.65, article.qualityScore(), 0.001);
            assertFalse(article.meetsQualityThreshold(0.7));
        }

        @Test
        @DisplayName("Defaults to 0.8 when score not provided")
        void defaultsTo0point8WhenNotProvided() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent."
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();
            assertEquals(0.8, article.qualityScore(), 0.001);
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Validates document with good quality score")
        void validatesWithGoodQualityScore() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nGood content.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertTrue(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with no final article")
        void failsWithNoFinalArticle() {
            assertFalse(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with low quality score")
        void failsWithLowQualityScore() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nPoor content.",
                      "qualityScore": 0.5
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertFalse(agent.validate(document));
        }

        @Test
        @DisplayName("Passes validation at quality threshold boundary")
        void passesAtQualityThresholdBoundary() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nAcceptable content.",
                      "qualityScore": 0.7
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertTrue(agent.validate(document));
        }
    }

    @Nested
    @DisplayName("Prompt Building")
    class PromptBuilding {

        @Test
        @DisplayName("Includes article draft in prompt")
        void includesArticleDraftInPrompt() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Apache Kafka is a distributed streaming platform"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes fact-check feedback in prompt")
        void includesFactCheckFeedbackInPrompt() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Overall Confidence: HIGH"));
                assertTrue(prompt.contains("Recommendation: APPROVE"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes questionable claims in prompt")
        void includesQuestionableClaimsInPrompt() {
            // Create a fresh document with questionable claims in fact-check report
            TopicBrief brief = TopicBrief.simple("Apache Kafka", "developers", 1000);
            PublishingDocument docWithIssues = new PublishingDocument(brief);
            docWithIssues.transitionTo(DocumentState.RESEARCHING);

            ResearchBrief researchBrief = new ResearchBrief(
                    List.of(KeyFact.unsourced("Kafka is a streaming platform")),
                    List.of(),
                    List.of("Introduction"),
                    List.of(),
                    Map.of(),
                    List.of()
            );
            docWithIssues.setResearchBrief(researchBrief);
            docWithIssues.transitionTo(DocumentState.DRAFTING);

            ArticleDraft draft = new ArticleDraft(
                    "!!! Kafka\n\nKafka is the fastest messaging system.",
                    "Summary",
                    List.of(),
                    List.of(),
                    Map.of()
            );
            docWithIssues.setDraft(draft);
            docWithIssues.transitionTo(DocumentState.FACT_CHECKING);

            FactCheckReport reportWithIssues = new FactCheckReport(
                    draft.wikiContent(),
                    List.of(),
                    List.of(QuestionableClaim.withSuggestion(
                            "Kafka is the fastest",
                            "No evidence",
                            "Remove superlative"
                    )),
                    List.of(),
                    ConfidenceLevel.MEDIUM,
                    RecommendedAction.REVISE
            );
            docWithIssues.setFactCheckReport(reportWithIssues);
            docWithIssues.transitionTo(DocumentState.EDITING);

            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nFixed content.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Kafka is the fastest"));
                assertTrue(prompt.contains("No evidence"));
                assertTrue(prompt.contains("Remove superlative"));
                return jsonResponse;
            });

            agent.process(docWithIssues);
        }

        @Test
        @DisplayName("Includes existing pages in prompt when set")
        void includesExistingPagesInPrompt() {
            agent.setExistingPages(List.of("EventStreaming", "MessageQueue"));

            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent with links.",
                      "qualityScore": 0.85,
                      "addedLinks": ["EventStreaming"]
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("EXISTING PAGES"));
                assertTrue(prompt.contains("EventStreaming"));
                assertTrue(prompt.contains("MessageQueue"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Does not include existing pages section when empty")
        void doesNotIncludeExistingPagesWhenEmpty() {
            // Don't set any existing pages

            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertFalse(prompt.contains("EXISTING PAGES"));
                return jsonResponse;
            });

            agent.process(document);
        }
    }

    @Nested
    @DisplayName("Metadata Parsing")
    class MetadataParsing {

        @Test
        @DisplayName("Parses full metadata")
        void parsesFullMetadata() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Test\\n\\nContent.",
                      "metadata": {
                        "title": "Custom Title",
                        "summary": "Custom summary",
                        "author": "Custom Author"
                      },
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();

            assertEquals("Custom Title", article.getTitle());
            assertEquals("Custom summary", article.getSummary());
        }

        @Test
        @DisplayName("Falls back to topic for missing title")
        void fallsBackToTopicForMissingTitle() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Test\\n\\nContent.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();

            assertEquals("Apache Kafka", article.getTitle());
        }
    }

    @Nested
    @DisplayName("Added Links Parsing")
    class AddedLinksParsing {

        @Test
        @DisplayName("Parses added links correctly")
        void parsesAddedLinks() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nSee [EventStreaming] for more.",
                      "qualityScore": 0.85,
                      "addedLinks": ["EventStreaming", "MessageQueue", "ApacheZookeeper"]
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();

            assertEquals(3, article.addedLinks().size());
            assertTrue(article.addedLinks().contains("EventStreaming"));
            assertTrue(article.addedLinks().contains("MessageQueue"));
            assertTrue(article.addedLinks().contains("ApacheZookeeper"));
        }

        @Test
        @DisplayName("Handles empty added links")
        void handlesEmptyAddedLinks() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nNo new links.",
                      "qualityScore": 0.85,
                      "addedLinks": []
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FinalArticle article = agent.process(document).getFinalArticle();

            assertTrue(article.addedLinks().isEmpty());
        }
    }

    @Nested
    @DisplayName("Contribution Recording")
    class ContributionRecording {

        @Test
        @DisplayName("Records contribution after successful processing")
        void recordsContribution() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent.",
                      "qualityScore": 0.85
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            int initialContributions = document.getContributions().size();

            agent.process(document);

            assertEquals(initialContributions + 1, document.getContributions().size());
            AgentContribution contribution = document.getContributions().get(document.getContributions().size() - 1);
            assertEquals("EDITOR", contribution.agentRole());
            assertNotNull(contribution.processingTime());
        }
    }
}
