package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatModel;
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
@DisplayName("CriticAgent")
class CriticAgentTest {

    @Mock
    private ChatModel mockModel;

    private CriticAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        agent = new CriticAgent(mockModel, AgentPrompts.CRITIC);

        // Create a document that has gone through the full pipeline
        TopicBrief topicBrief = TopicBrief.simple("Apache Kafka", "developers", 1000);
        document = new PublishingDocument(topicBrief);

        // Simulate pipeline progression
        document.transitionTo(DocumentState.RESEARCHING);
        document.setResearchBrief(createResearchBrief());

        document.transitionTo(DocumentState.DRAFTING);
        document.setDraft(createDraft());

        document.transitionTo(DocumentState.FACT_CHECKING);
        document.setFactCheckReport(createFactCheckReport());

        document.transitionTo(DocumentState.EDITING);
        document.setFinalArticle(createFinalArticle());

        document.transitionTo(DocumentState.CRITIQUING);
    }

    private ResearchBrief createResearchBrief() {
        return new ResearchBrief(
                List.of(KeyFact.unsourced("Kafka is a distributed streaming platform"),
                        KeyFact.unsourced("Created at LinkedIn"),
                        KeyFact.unsourced("Uses topics and partitions")),
                List.of(new SourceCitation("Apache Kafka Docs", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Core Concepts", "Use Cases"),
                List.of("EventStreaming"),
                Map.of("Topic", "A category for records"),
                List.of()
        );
    }

    private ArticleDraft createDraft() {
        return new ArticleDraft(
                "!!! Apache Kafka\n\nKafka is a distributed streaming platform...",
                "Summary of Apache Kafka",
                List.of("EventStreaming"),
                List.of("Technology"),
                Map.of()
        );
    }

    private FactCheckReport createFactCheckReport() {
        return new FactCheckReport(
                "!!! Apache Kafka\n\nKafka is a distributed streaming platform...",
                List.of(new VerifiedClaim("Kafka is distributed", "VERIFIED", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        );
    }

    private FinalArticle createFinalArticle() {
        return new FinalArticle(
                "!!! Apache Kafka\n\n[{TableOfContents}]\n\nKafka is a distributed streaming platform...\n\n!! See Also\n\n[EventStreaming]",
                DocumentMetadata.create("Apache Kafka", "Overview of Apache Kafka"),
                "Polished article with proper JSPWiki syntax",
                0.9,
                List.of("EventStreaming")
        );
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("Has CRITIC role")
        void hasCriticRole() {
            assertEquals(AgentRole.CRITIC, agent.getRole());
        }

        @Test
        @DisplayName("Has correct display name")
        void hasCorrectDisplayName() {
            assertEquals("Critic Agent", agent.getName());
        }
    }

    @Nested
    @DisplayName("JSON Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Parses valid JSON response with approval")
        void parsesValidApprovalResponse() {
            String jsonResponse = """
                    {
                      "overallScore": 0.92,
                      "structureScore": 0.95,
                      "syntaxScore": 1.0,
                      "readabilityScore": 0.88,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": ["Minor word choice issue in paragraph 3"],
                      "suggestions": ["Consider adding more examples"],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            CriticReport report = result.getCriticReport();
            assertNotNull(report);
            assertEquals(0.92, report.overallScore(), 0.001);
            assertEquals(0.95, report.structureScore(), 0.001);
            assertEquals(1.0, report.syntaxScore(), 0.001);
            assertEquals(0.88, report.readabilityScore(), 0.001);
            assertTrue(report.structureIssues().isEmpty());
            assertTrue(report.syntaxIssues().isEmpty());
            assertEquals(1, report.styleIssues().size());
            assertEquals(1, report.suggestions().size());
            assertEquals(RecommendedAction.APPROVE, report.recommendedAction());
            assertTrue(report.isApproved());
        }

        @Test
        @DisplayName("Parses response with syntax issues")
        void parsesResponseWithSyntaxIssues() {
            String jsonResponse = """
                    {
                      "overallScore": 0.65,
                      "structureScore": 0.8,
                      "syntaxScore": 0.5,
                      "readabilityScore": 0.7,
                      "structureIssues": [],
                      "syntaxIssues": [
                        "Line 5: Uses **bold** instead of __bold__",
                        "Line 10: Uses [text](url) instead of [text|url]",
                        "Line 15: Uses ## heading instead of !! heading"
                      ],
                      "styleIssues": [],
                      "suggestions": ["Convert all Markdown syntax to JSPWiki syntax"],
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            CriticReport report = result.getCriticReport();
            assertNotNull(report);
            assertEquals(0.5, report.syntaxScore(), 0.001);
            assertEquals(3, report.syntaxIssues().size());
            assertTrue(report.hasSyntaxIssues());
            assertTrue(report.needsRevision());
            assertFalse(report.isApproved());
        }

        @Test
        @DisplayName("Parses response with structure issues")
        void parsesResponseWithStructureIssues() {
            String jsonResponse = """
                    {
                      "overallScore": 0.72,
                      "structureScore": 0.6,
                      "syntaxScore": 0.9,
                      "readabilityScore": 0.75,
                      "structureIssues": [
                        "Missing table of contents for long article",
                        "See Also section not at end"
                      ],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": ["Add [{TableOfContents}] after intro"],
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            CriticReport report = result.getCriticReport();
            assertEquals(2, report.structureIssues().size());
            assertTrue(report.needsRevision());
        }

        @Test
        @DisplayName("Parses rejection response")
        void parsesRejectionResponse() {
            String jsonResponse = """
                    {
                      "overallScore": 0.45,
                      "structureScore": 0.5,
                      "syntaxScore": 0.3,
                      "readabilityScore": 0.5,
                      "structureIssues": ["No clear heading hierarchy"],
                      "syntaxIssues": ["Entire article uses Markdown syntax"],
                      "styleIssues": ["Tone inconsistent", "Multiple grammar errors"],
                      "suggestions": ["Rewrite with proper JSPWiki syntax"],
                      "recommendedAction": "REJECT"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            CriticReport report = result.getCriticReport();
            assertTrue(report.needsRework());
            assertFalse(report.isApproved());
            assertFalse(report.needsRevision());
        }

        @Test
        @DisplayName("Handles JSON wrapped in markdown code blocks")
        void handlesMarkdownWrappedJson() {
            String jsonResponse = """
                    ```json
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    ```
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            assertNotNull(result.getCriticReport());
            assertTrue(result.getCriticReport().isApproved());
        }

        @Test
        @DisplayName("Throws on invalid JSON")
        void throwsOnInvalidJson() {
            when(mockModel.chat(anyString())).thenReturn("This is not JSON");

            assertThrows(AgentException.class, () -> agent.process(document));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Validates document with critic report")
        void validatesWithCriticReport() {
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertTrue(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with no critic report")
        void failsWithNoCriticReport() {
            // Don't process, so no critic report is set
            assertFalse(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with zero overall score")
        void failsWithZeroOverallScore() {
            String jsonResponse = """
                    {
                      "overallScore": 0,
                      "structureScore": 0,
                      "syntaxScore": 0,
                      "readabilityScore": 0,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "REJECT"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertFalse(agent.validate(document));
        }
    }

    @Nested
    @DisplayName("CriticReport Behavior")
    class CriticReportBehavior {

        @Test
        @DisplayName("meetsQualityThreshold works correctly")
        void meetsQualityThresholdWorks() {
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            CriticReport report = document.getCriticReport();
            assertTrue(report.meetsQualityThreshold(0.8));
            assertTrue(report.meetsQualityThreshold(0.85));
            assertFalse(report.meetsQualityThreshold(0.9));
        }

        @Test
        @DisplayName("getIssueSummary formats correctly")
        void getIssueSummaryFormatsCorrectly() {
            String jsonResponse = """
                    {
                      "overallScore": 0.7,
                      "structureScore": 0.7,
                      "syntaxScore": 0.6,
                      "readabilityScore": 0.7,
                      "structureIssues": ["Issue 1", "Issue 2"],
                      "syntaxIssues": ["Syntax issue"],
                      "styleIssues": ["Style 1", "Style 2", "Style 3"],
                      "suggestions": [],
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            CriticReport report = document.getCriticReport();
            String summary = report.getIssueSummary();

            assertTrue(summary.contains("6 issues found"));
            assertTrue(summary.contains("structure: 2"));
            assertTrue(summary.contains("syntax: 1"));
            assertTrue(summary.contains("style: 3"));
        }
    }

    @Nested
    @DisplayName("Prompt Building")
    class PromptBuilding {

        @Test
        @DisplayName("Includes article content in prompt")
        void includesArticleContent() {
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Apache Kafka"));
                assertTrue(prompt.contains("[{TableOfContents}]"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes metadata in prompt")
        void includesMetadataInPrompt() {
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Title:"));
                assertTrue(prompt.contains("Editor quality score:"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Mentions JSPWiki syntax in prompt")
        void mentionsJSPWikiSyntax() {
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.toLowerCase().contains("jspwiki") || prompt.contains("Markdown"));
                return jsonResponse;
            });

            agent.process(document);
        }
    }

    @Nested
    @DisplayName("Defensive Null Handling")
    class DefensiveNullHandling {

        @Test
        @DisplayName("Throws AgentException when FinalArticle is null")
        void throwsAgentExceptionWhenFinalArticleIsNull() {
            // This test documents the current behavior: CriticAgent requires FinalArticle to be set
            // If called without a FinalArticle (which shouldn't happen in normal pipeline flow),
            // it will throw AgentException (wrapping the underlying NPE). This is acceptable
            // because CriticAgent is only called after EditorAgent.

            // Create a document without FinalArticle (simulating incorrect usage)
            TopicBrief brief = TopicBrief.simple("Test Topic", "developers", 500);
            PublishingDocument docWithoutFinalArticle = new PublishingDocument(brief);
            docWithoutFinalArticle.transitionTo(DocumentState.RESEARCHING);
            docWithoutFinalArticle.setResearchBrief(createResearchBrief());
            docWithoutFinalArticle.transitionTo(DocumentState.DRAFTING);
            docWithoutFinalArticle.setDraft(createDraft());
            docWithoutFinalArticle.transitionTo(DocumentState.FACT_CHECKING);
            docWithoutFinalArticle.setFactCheckReport(createFactCheckReport());
            docWithoutFinalArticle.transitionTo(DocumentState.EDITING);
            // Note: NOT setting FinalArticle
            docWithoutFinalArticle.transitionTo(DocumentState.CRITIQUING);

            // Assert that FinalArticle is actually null
            assertNull(docWithoutFinalArticle.getFinalArticle(),
                    "FinalArticle should be null for this test");

            // This is expected to throw AgentException because FinalArticle is null
            // The CriticAgent.buildUserPrompt() accesses article.wikiContent() without null check
            // BaseAgent wraps the NPE in an AgentException
            AgentException exception = assertThrows(AgentException.class,
                    () -> agent.process(docWithoutFinalArticle),
                    "CriticAgent should throw AgentException when FinalArticle is null");

            // Verify the underlying cause is NPE
            assertNotNull(exception.getCause());
            assertTrue(exception.getCause() instanceof NullPointerException,
                    "Underlying cause should be NullPointerException");
        }

        @Test
        @DisplayName("Requires FinalArticle to be present - documents contract")
        void requiresFinalArticleToBePresent() {
            // This test documents the contract that CriticAgent expects:
            // - FinalArticle must be set before calling process()
            // - This is enforced by the pipeline which runs EditorAgent before CriticAgent

            // Verify our normal test setup has FinalArticle set
            assertNotNull(document.getFinalArticle(),
                    "Normal test setup should have FinalArticle set");
            assertFalse(document.getFinalArticle().wikiContent().isBlank(),
                    "FinalArticle should have content");

            // Verify we can process normally
            String jsonResponse = """
                    {
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;
            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            assertDoesNotThrow(() -> agent.process(document),
                    "CriticAgent should process successfully when FinalArticle is present");
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
                      "overallScore": 0.85,
                      "structureScore": 0.9,
                      "syntaxScore": 0.8,
                      "readabilityScore": 0.85,
                      "structureIssues": [],
                      "syntaxIssues": [],
                      "styleIssues": [],
                      "suggestions": [],
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            int initialContributions = document.getContributions().size();
            agent.process(document);

            assertEquals(initialContributions + 1, document.getContributions().size());
            AgentContribution contribution = document.getContributions().get(document.getContributions().size() - 1);
            assertEquals("CRITIC", contribution.agentRole());
            assertNotNull(contribution.processingTime());
        }
    }
}
