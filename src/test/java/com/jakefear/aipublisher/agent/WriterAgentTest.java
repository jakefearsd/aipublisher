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
@DisplayName("WriterAgent")
class WriterAgentTest {

    @Mock
    private ChatModel mockModel;

    private WriterAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        agent = new WriterAgent(mockModel, AgentPrompts.WRITER);
        TopicBrief brief = TopicBrief.simple("Apache Kafka", "developers new to streaming", 1500);
        document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        // Set up research brief (prerequisite for writer)
        ResearchBrief researchBrief = new ResearchBrief(
                List.of(
                        KeyFact.unsourced("Kafka is a distributed streaming platform"),
                        KeyFact.unsourced("Created at LinkedIn in 2011"),
                        KeyFact.unsourced("Uses topics and partitions")
                ),
                List.of(new SourceCitation("Apache Kafka Documentation", ConfidenceLevel.HIGH)),
                List.of("Introduction", "Core Concepts", "Use Cases"),
                List.of("EventStreaming", "MessageQueue"),
                Map.of("Topic", "A category for records"),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("Has WRITER role")
        void hasWriterRole() {
            assertEquals(AgentRole.WRITER, agent.getRole());
        }

        @Test
        @DisplayName("Has correct display name")
        void hasCorrectDisplayName() {
            assertEquals("Writer Agent", agent.getName());
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
                      "wikiContent": "!!! Apache Kafka\\n\\nApache Kafka is a distributed streaming platform...\\n\\n!! Core Concepts\\n\\nTopics organize messages...",
                      "summary": "An introduction to Apache Kafka for developers new to streaming.",
                      "internalLinks": ["EventStreaming", "MessageQueue"],
                      "categories": ["Technology", "Streaming"]
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            ArticleDraft draft = result.getDraft();
            assertNotNull(draft);
            assertTrue(draft.wikiContent().contains("Apache Kafka"));
            assertEquals("An introduction to Apache Kafka for developers new to streaming.", draft.summary());
            assertEquals(2, draft.internalLinks().size());
            assertEquals(2, draft.categories().size());
        }

        @Test
        @DisplayName("Handles JSON wrapped in code blocks")
        void handlesCodeBlockWrappedJson() {
            String jsonResponse = """
                    ```json
                    {
                      "wikiContent": "!!! Test Article\\n\\nContent here.",
                      "summary": "Test summary",
                      "internalLinks": [],
                      "categories": []
                    }
                    ```
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            assertNotNull(result.getDraft());
            assertTrue(result.getDraft().wikiContent().contains("Test Article"));
        }

        @Test
        @DisplayName("Handles minimal valid response")
        void handlesMinimalResponse() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Minimal Article\\n\\nThis is minimal content for testing.",
                      "summary": "A minimal article."
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            PublishingDocument result = agent.process(document);

            ArticleDraft draft = result.getDraft();
            assertNotNull(draft);
            assertTrue(draft.internalLinks().isEmpty());
            assertTrue(draft.categories().isEmpty());
        }

        @Test
        @DisplayName("Throws on missing wiki content")
        void throwsOnMissingWikiContent() {
            String jsonResponse = """
                    {
                      "wikiContent": "",
                      "summary": "Summary without content"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            assertThrows(AgentException.class, () -> agent.process(document));
        }

        @Test
        @DisplayName("Throws on missing summary")
        void throwsOnMissingSummary() {
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Article\\n\\nContent...",
                      "summary": ""
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            assertThrows(AgentException.class, () -> agent.process(document));
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
        @DisplayName("Validates document with sufficient content")
        void validatesWithSufficientContent() {
            // Create a draft with enough words (at least 50% of target 1500 = 750)
            String longContent = "!!! Apache Kafka\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test summary"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            agent.process(document);

            assertTrue(agent.validate(document));
        }

        @Test
        @DisplayName("Fails validation with no draft")
        void failsWithNoDraft() {
            // Don't process, so no draft is set
            assertFalse(agent.validate(document));
        }

        @Test
        @DisplayName("Passes validation with insufficient word count (warns only)")
        void passesValidationWithInsufficientWordCountButWarns() {
            // Create a very short draft (less than 50% of 1500 = 750)
            // Note: Word count validation only warns, doesn't fail - the pipeline decides
            String jsonResponse = """
                    {
                      "wikiContent": "!!! Short\\n\\nToo short.",
                      "summary": "Test summary"
                    }
                    """;

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);
            agent.process(document);

            // Validation passes even with insufficient words - it only warns
            assertTrue(agent.validate(document),
                    "Validation should pass (word count only logs warning)");
        }
    }

    @Nested
    @DisplayName("Prompt Building")
    class PromptBuilding {

        @Test
        @DisplayName("Includes topic in prompt")
        void includesTopicInPrompt() {
            String longContent = "!!! Test\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Apache Kafka"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes key facts in prompt")
        void includesKeyFactsInPrompt() {
            String longContent = "!!! Test\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("distributed streaming platform"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes suggested outline in prompt")
        void includesSuggestedOutlineInPrompt() {
            String longContent = "!!! Test\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("Core Concepts"));
                assertTrue(prompt.contains("Use Cases"));
                return jsonResponse;
            });

            agent.process(document);
        }

        @Test
        @DisplayName("Includes related pages for linking")
        void includesRelatedPagesInPrompt() {
            String longContent = "!!! Test\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenAnswer(invocation -> {
                String prompt = invocation.getArgument(0);
                assertTrue(prompt.contains("EventStreaming"));
                assertTrue(prompt.contains("MessageQueue"));
                return jsonResponse;
            });

            agent.process(document);
        }
    }

    @Nested
    @DisplayName("Contribution Recording")
    class ContributionRecording {

        @Test
        @DisplayName("Records contribution after successful processing")
        void recordsContribution() {
            String longContent = "!!! Test\n\n" + "word ".repeat(800);
            String jsonResponse = """
                    {
                      "wikiContent": "%s",
                      "summary": "Test"
                    }
                    """.formatted(longContent.replace("\n", "\\n"));

            when(mockModel.chat(anyString())).thenReturn(jsonResponse);

            assertEquals(0, document.getContributions().size());

            agent.process(document);

            assertEquals(1, document.getContributions().size());
            AgentContribution contribution = document.getContributions().get(0);
            assertEquals("WRITER", contribution.agentRole());
            assertNotNull(contribution.processingTime());
        }
    }
}
