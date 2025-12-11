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
@DisplayName("FactCheckerAgent")
class FactCheckerAgentTest {

    @Mock
    private ChatLanguageModel mockModel;

    private FactCheckerAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        agent = new FactCheckerAgent(mockModel, AgentPrompts.FACT_CHECKER);
        TopicBrief brief = TopicBrief.simple("Apache Kafka", "developers", 1000);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Kafka is a distributed streaming platform"),
                        KeyFact.unsourced("Created at LinkedIn in 2011"),
                        KeyFact.unsourced("Uses topics and partitions")
                ),
                List.of(new SourceCitation("Apache Kafka Documentation", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Core Concepts"),
                List.of(),
                Map.of("Topic", "A category for records"),
                List.of("Performance numbers vary by configuration")
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);

        // Set up article draft (prerequisite for fact checker)
        ArticleDraft draft = new ArticleDraft(
                "## Apache Kafka\n\nApache Kafka is a distributed streaming platform created at LinkedIn in 2011.",
                "An introduction to Apache Kafka",
                List.of(),
                List.of(),
                Map.of()
        );
        document.setDraft(draft);
        document.transitionTo(DocumentState.FACT_CHECKING);
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("Has FACT_CHECKER role")
        void hasFactCheckerRole() {
            assertEquals(AgentRole.FACT_CHECKER, agent.getRole());
        }

        @Test
        @DisplayName("Has correct display name")
        void hasCorrectDisplayName() {
            assertEquals("Fact Checker Agent", agent.getName());
        }
    }

    @Nested
    @DisplayName("JSON Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Parses valid JSON response with APPROVE recommendation")
        void parsesValidJsonResponseApprove() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [
                        {"claim": "Kafka is a distributed streaming platform", "status": "VERIFIED", "sourceIndex": 0},
                        {"claim": "Created at LinkedIn in 2011", "status": "VERIFIED", "sourceIndex": 0}
                      ],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FactCheckReport report = result.getFactCheckReport();
            assertNotNull(report);
            assertEquals(2, report.verifiedClaims().size());
            assertTrue(report.questionableClaims().isEmpty());
            assertTrue(report.consistencyIssues().isEmpty());
            assertEquals(ConfidenceLevel.HIGH, report.overallConfidence());
            assertEquals(RecommendedAction.APPROVE, report.recommendedAction());
            assertTrue(report.isPassed());
        }

        @Test
        @DisplayName("Parses response with REVISE recommendation")
        void parsesResponseWithRevise() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [
                        {"claim": "Kafka is a streaming platform", "status": "VERIFIED", "sourceIndex": 0}
                      ],
                      "questionableClaims": [
                        {"claim": "Kafka processes millions of messages per second", "issue": "No source for this specific number", "suggestion": "Remove or cite source"}
                      ],
                      "consistencyIssues": [],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FactCheckReport report = result.getFactCheckReport();
            assertNotNull(report);
            assertEquals(1, report.verifiedClaims().size());
            assertEquals(1, report.questionableClaims().size());
            assertEquals(ConfidenceLevel.MEDIUM, report.overallConfidence());
            assertEquals(RecommendedAction.REVISE, report.recommendedAction());
            assertTrue(report.needsRevision());
        }

        @Test
        @DisplayName("Parses response with REJECT recommendation")
        void parsesResponseWithReject() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [],
                      "questionableClaims": [
                        {"claim": "Kafka was created in 2005", "issue": "Wrong date - actually 2011", "suggestion": "Correct to 2011"}
                      ],
                      "consistencyIssues": ["Article contradicts itself about creation date"],
                      "overallConfidence": "LOW",
                      "recommendedAction": "REJECT"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FactCheckReport report = result.getFactCheckReport();
            assertNotNull(report);
            assertEquals(0, report.verifiedClaims().size());
            assertEquals(1, report.questionableClaims().size());
            assertEquals(1, report.consistencyIssues().size());
            assertEquals(ConfidenceLevel.LOW, report.overallConfidence());
            assertEquals(RecommendedAction.REJECT, report.recommendedAction());
            assertTrue(report.isRejected());
        }

        @Test
        @DisplayName("Handles JSON wrapped in markdown code blocks")
        void handlesMarkdownWrappedJson() {
            String jsonResponse = """
                    ```json
                    {
                      "verifiedClaims": [{"claim": "Test claim", "status": "VERIFIED", "sourceIndex": 0}],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    ```
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            assertNotNull(result.getFactCheckReport());
            assertEquals(1, result.getFactCheckReport().verifiedClaims().size());
        }

        @Test
        @DisplayName("Handles minimal valid response")
        void handlesMinimalResponse() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Basic fact", "status": "VERIFIED", "sourceIndex": -1}],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            FactCheckReport report = result.getFactCheckReport();
            assertNotNull(report);
            assertTrue(report.questionableClaims().isEmpty());
            assertTrue(report.consistencyIssues().isEmpty());
        }

        @Test
        @DisplayName("Defaults to REVISE for unknown action (safe default)")
        void defaultsToReviseForUnknownAction() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Fact", "status": "VERIFIED", "sourceIndex": 0}],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "UNKNOWN_ACTION"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            // Unknown actions default to REVISE for safety - better to require manual review
            assertEquals(RecommendedAction.REVISE, result.getFactCheckReport().recommendedAction());
        }

        @Test
        @DisplayName("Throws on invalid JSON")
        void throwsOnInvalidJson() {
            when(mockModel.generate(anyString())).thenReturn("This is not JSON");

            assertThrows(AgentException.class, () -> agent.process(document));
        }
    }

    @Nested
    @DisplayName("Confidence Level Parsing")
    class ConfidenceLevelParsing {

        @Test
        @DisplayName("Parses HIGH confidence")
        void parsesHighConfidence() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Fact", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FactCheckReport report = agent.process(document).getFactCheckReport();
            assertEquals(ConfidenceLevel.HIGH, report.overallConfidence());
        }

        @Test
        @DisplayName("Parses MEDIUM confidence")
        void parsesMediumConfidence() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Fact", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FactCheckReport report = agent.process(document).getFactCheckReport();
            assertEquals(ConfidenceLevel.MEDIUM, report.overallConfidence());
        }

        @Test
        @DisplayName("Parses LOW confidence")
        void parsesLowConfidence() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Fact", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "LOW",
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FactCheckReport report = agent.process(document).getFactCheckReport();
            assertEquals(ConfidenceLevel.LOW, report.overallConfidence());
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Validates document with fact-check report")
        void validatesWithFactCheckReport() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Test", "status": "VERIFIED", "sourceIndex": 0}],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertTrue(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with no report")
        void failsWithNoReport() {
            assertFalse(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with no claims checked")
        void failsWithNoClaimsChecked() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertFalse(agent.validate(document));
        }
    }

    @Nested
    @DisplayName("Prompt Building")
    class PromptBuilding {

        @Test
        @DisplayName("Includes article content in prompt")
        void includesArticleContentInPrompt() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Test", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
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
        @DisplayName("Includes research key facts in prompt")
        void includesKeyFactsInPrompt() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Test", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Created at LinkedIn in 2011"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes uncertain areas in prompt")
        void includesUncertainAreasInPrompt() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [{"claim": "Test", "status": "VERIFIED", "sourceIndex": 0}],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Performance numbers vary by configuration"));
                return jsonResponse;
            });

            agent.process(document);
        }
    }

    @Nested
    @DisplayName("Questionable Claims Parsing")
    class QuestionableClaimsParsing {

        @Test
        @DisplayName("Parses questionable claims with all fields")
        void parsesQuestionableClaimsWithAllFields() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [],
                      "questionableClaims": [
                        {
                          "claim": "Kafka is the fastest messaging system",
                          "issue": "Superlative claim without evidence",
                          "suggestion": "Rephrase as 'one of the fastest' or provide benchmark data"
                        }
                      ],
                      "consistencyIssues": [],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FactCheckReport report = agent.process(document).getFactCheckReport();

            assertEquals(1, report.questionableClaims().size());
            QuestionableClaim claim = report.questionableClaims().get(0);
            assertEquals("Kafka is the fastest messaging system", claim.claim());
            assertEquals("Superlative claim without evidence", claim.issue());
            assertTrue(claim.suggestion().contains("one of the fastest"));
        }

        @Test
        @DisplayName("Handles questionable claims without suggestion")
        void handlesQuestionableClaimsWithoutSuggestion() {
            String jsonResponse = """
                    {
                      "verifiedClaims": [],
                      "questionableClaims": [
                        {"claim": "Some claim", "issue": "Some issue"}
                      ],
                      "overallConfidence": "MEDIUM",
                      "recommendedAction": "REVISE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            FactCheckReport report = agent.process(document).getFactCheckReport();

            assertEquals(1, report.questionableClaims().size());
            assertEquals("", report.questionableClaims().get(0).suggestion());
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
                      "verifiedClaims": [{"claim": "Test", "status": "VERIFIED", "sourceIndex": 0}],
                      "questionableClaims": [],
                      "consistencyIssues": [],
                      "overallConfidence": "HIGH",
                      "recommendedAction": "APPROVE"
                    }
                    """;

            when(mockModel.generate(anyString())).thenReturn(jsonResponse);

            // Document already has contributions from previous phases (simulated)
            int initialContributions = document.getContributions().size();

            agent.process(document);

            assertEquals(initialContributions + 1, document.getContributions().size());
            AgentContribution contribution = document.getContributions().get(document.getContributions().size() - 1);
            assertEquals("FACT_CHECKER", contribution.agentRole());
            assertNotNull(contribution.processingTime());
        }
    }
}
