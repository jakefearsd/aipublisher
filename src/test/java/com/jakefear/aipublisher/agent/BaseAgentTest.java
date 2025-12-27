package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BaseAgent")
class BaseAgentTest {

    @Mock
    private ChatModel mockModel;

    private TestableAgent agent;
    private PublishingDocument document;

    @BeforeEach
    void setUp() {
        agent = new TestableAgent(mockModel);
        TopicBrief brief = TopicBrief.simple("Test Topic", "testers", 500);
        document = new PublishingDocument(brief);
    }

    @Nested
    @DisplayName("JSON Cleaning")
    class JsonCleaning {

        @Test
        @DisplayName("Removes json markdown code block wrapper")
        void removesJsonCodeBlock() {
            String input = "```json\n{\"key\": \"value\"}\n```";
            String cleaned = agent.testCleanJsonResponse(input);
            assertEquals("{\"key\": \"value\"}", cleaned);
        }

        @Test
        @DisplayName("Removes plain markdown code block wrapper")
        void removesPlainCodeBlock() {
            String input = "```\n{\"key\": \"value\"}\n```";
            String cleaned = agent.testCleanJsonResponse(input);
            assertEquals("{\"key\": \"value\"}", cleaned);
        }

        @Test
        @DisplayName("Handles already clean JSON")
        void handlesCleanJson() {
            String input = "{\"key\": \"value\"}";
            String cleaned = agent.testCleanJsonResponse(input);
            assertEquals("{\"key\": \"value\"}", cleaned);
        }

        @Test
        @DisplayName("Handles null input")
        void handlesNullInput() {
            String cleaned = agent.testCleanJsonResponse(null);
            assertEquals("{}", cleaned);
        }

        @Test
        @DisplayName("Trims whitespace")
        void trimsWhitespace() {
            String input = "  \n{\"key\": \"value\"}\n  ";
            String cleaned = agent.testCleanJsonResponse(input);
            assertEquals("{\"key\": \"value\"}", cleaned);
        }
    }

    @Nested
    @DisplayName("JSON Parsing Helpers")
    class JsonParsingHelpers {

        @Test
        @DisplayName("getStringOrDefault returns value when present")
        void returnsValueWhenPresent() throws Exception {
            JsonNode node = agent.testParseJson("{\"field\": \"value\"}");
            String result = agent.testGetStringOrDefault(node, "field", "default");
            assertEquals("value", result);
        }

        @Test
        @DisplayName("getStringOrDefault returns default when missing")
        void returnsDefaultWhenMissing() throws Exception {
            JsonNode node = agent.testParseJson("{}");
            String result = agent.testGetStringOrDefault(node, "field", "default");
            assertEquals("default", result);
        }

        @Test
        @DisplayName("getStringOrDefault returns default when null")
        void returnsDefaultWhenNull() throws Exception {
            JsonNode node = agent.testParseJson("{\"field\": null}");
            String result = agent.testGetStringOrDefault(node, "field", "default");
            assertEquals("default", result);
        }

        @Test
        @DisplayName("getIntOrDefault returns value when present")
        void returnsIntValueWhenPresent() throws Exception {
            JsonNode node = agent.testParseJson("{\"count\": 42}");
            int result = agent.testGetIntOrDefault(node, "count", 0);
            assertEquals(42, result);
        }

        @Test
        @DisplayName("getIntOrDefault returns default when missing")
        void returnsIntDefaultWhenMissing() throws Exception {
            JsonNode node = agent.testParseJson("{}");
            int result = agent.testGetIntOrDefault(node, "count", 99);
            assertEquals(99, result);
        }

        @Test
        @DisplayName("getDoubleOrDefault returns value when present")
        void returnsDoubleValueWhenPresent() throws Exception {
            JsonNode node = agent.testParseJson("{\"score\": 0.95}");
            double result = agent.testGetDoubleOrDefault(node, "score", 0.0);
            assertEquals(0.95, result, 0.001);
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryLogic {

        @Test
        @DisplayName("Succeeds on first attempt")
        void succeedsOnFirstAttempt() {
            when(mockModel.chat(anyString())).thenReturn("success");

            agent.process(document);

            verify(mockModel, times(1)).chat(anyString());
            assertTrue(agent.wasProcessed());
        }

        @Test
        @DisplayName("Retries on transient failure")
        void retriesOnTransientFailure() {
            when(mockModel.chat(anyString()))
                    .thenThrow(new RuntimeException("timeout"))
                    .thenReturn("success");

            // Use a fast retry agent for testing
            TestableAgent fastAgent = new TestableAgent(mockModel, 3, Duration.ofMillis(10), 1.0);
            fastAgent.process(document);

            verify(mockModel, times(2)).chat(anyString());
        }

        @Test
        @DisplayName("Throws after max retries")
        void throwsAfterMaxRetries() {
            when(mockModel.chat(anyString()))
                    .thenThrow(new RuntimeException("timeout"));

            TestableAgent fastAgent = new TestableAgent(mockModel, 2, Duration.ofMillis(10), 1.0);

            AgentException exception = assertThrows(AgentException.class,
                    () -> fastAgent.process(document));

            assertEquals(AgentRole.RESEARCHER, exception.getAgentRole());
            verify(mockModel, times(2)).chat(anyString());
        }
    }

    @Nested
    @DisplayName("Thinking Tag Extraction")
    class ThinkingTagExtraction {

        @Test
        @DisplayName("Passes through response with no thinking tags unchanged")
        void passesThruResponseWithNoThinkingTags() {
            String input = "{\"key\": \"value\", \"content\": \"Hello world\"}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals(input, result);
        }

        @Test
        @DisplayName("Removes complete thinking block at start of response")
        void removesThinkingBlockAtStart() {
            String input = "<think>Let me analyze this problem...</think>{\"key\": \"value\"}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"key\": \"value\"}", result);
        }

        @Test
        @DisplayName("Removes complete thinking block at end of response")
        void removesThinkingBlockAtEnd() {
            String input = "{\"key\": \"value\"}<think>I should verify this...</think>";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"key\": \"value\"}", result);
        }

        @Test
        @DisplayName("Removes complete thinking block in middle of response")
        void removesThinkingBlockInMiddle() {
            String input = "{\"start\": \"value\",<think>processing...</think>\"end\": \"value\"}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"start\": \"value\",\"end\": \"value\"}", result);
        }

        @Test
        @DisplayName("Removes multiple thinking blocks")
        void removesMultipleThinkingBlocks() {
            String input = "<think>First thought</think>Hello<think>Second thought</think>World";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("HelloWorld", result);
        }

        @Test
        @DisplayName("Removes orphaned closing think tag")
        void removesOrphanedClosingTag() {
            String input = "{\"content\": \"Some text\"}</think>More text";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"content\": \"Some text\"}More text", result);
        }

        @Test
        @DisplayName("Removes orphaned opening think tag")
        void removesOrphanedOpeningTag() {
            String input = "Some text<think>{\"key\": \"value\"}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("Some text{\"key\": \"value\"}", result);
        }

        @Test
        @DisplayName("Handles multiline thinking content")
        void handlesMultilineThinkingContent() {
            String input = "<think>\nLine 1\nLine 2\nLine 3\n</think>{\"result\": \"success\"}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"result\": \"success\"}", result);
        }

        @Test
        @DisplayName("Handles thinking block with special characters")
        void handlesThinkingWithSpecialCharacters() {
            String input = "<think>Let's analyze: {\"temp\": 123} and [1,2,3]</think>{\"final\": true}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"final\": true}", result);
        }

        @Test
        @DisplayName("Returns null for null input")
        void returnsNullForNullInput() {
            String result = agent.testExtractAndLogThinking(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Returns empty string for empty input")
        void returnsEmptyForEmptyInput() {
            String result = agent.testExtractAndLogThinking("");
            assertEquals("", result);
        }

        @Test
        @DisplayName("Handles response that is only a thinking block")
        void handlesResponseThatIsOnlyThinkingBlock() {
            String input = "<think>All thinking, no output</think>";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("", result);
        }

        @Test
        @DisplayName("Preserves whitespace outside thinking blocks")
        void preservesWhitespaceOutsideThinkingBlocks() {
            String input = "  {\"key\": \"value\"}  ";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("  {\"key\": \"value\"}  ", result);
        }

        @Test
        @DisplayName("Handles nested angle brackets in thinking content")
        void handlesNestedAngleBracketsInThinking() {
            String input = "<think>Compare: a < b and c > d</think>{\"done\": true}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"done\": true}", result);
        }

        @Test
        @DisplayName("Handles real-world qwen3 failure scenario")
        void handlesRealWorldQwen3FailureScenario() {
            // This simulates the actual failure from session.log
            String input = "{\"wikiContent\": \"!!! Docker Guide\\n\\n" +
                    "Docker is a containerization platform...\\n\\n" +
                    "you can ensure that your Docker applications run smoothly and efficiently.\\n\\n\\n" +
                    "</think>\\n\\nTo effectively work with Docker, it's essential...\"," +
                    "\"summary\": \"A guide to Docker\"}";
            String result = agent.testExtractAndLogThinking(input);
            // Should remove the </think> but keep the rest
            assertFalse(result.contains("</think>"));
            assertTrue(result.contains("wikiContent"));
            assertTrue(result.contains("Docker Guide"));
        }

        @Test
        @DisplayName("Handles thinking block with escaped characters")
        void handlesThinkingWithEscapedCharacters() {
            String input = "<think>Processing \\\"quoted\\\" text and \\n newlines</think>{\"ok\": true}";
            String result = agent.testExtractAndLogThinking(input);
            assertEquals("{\"ok\": true}", result);
        }

        @Test
        @DisplayName("Thinking content is logged at INFO level")
        void thinkingContentIsLogged() {
            // This test verifies that logging happens - we check via the TestableAgent's capture
            String input = "<think>Important reasoning here</think>{\"result\": 42}";
            String result = agent.testExtractAndLogThinking(input);

            assertEquals("{\"result\": 42}", result);
            // Verify thinking was captured (TestableAgent stores it for verification)
            assertEquals("Important reasoning here", agent.getLastCapturedThinking());
        }

        @Test
        @DisplayName("Multiple thinking blocks are all logged")
        void multipleThinkingBlocksAreAllLogged() {
            String input = "<think>First</think>middle<think>Second</think>end";
            String result = agent.testExtractAndLogThinking(input);

            assertEquals("middleend", result);
            // All thinking content should be captured
            assertTrue(agent.getLastCapturedThinking().contains("First"));
            assertTrue(agent.getLastCapturedThinking().contains("Second"));
        }

        @Test
        @DisplayName("No logging occurs when no thinking blocks present")
        void noLoggingWhenNoThinkingBlocks() {
            agent.clearCapturedThinking();
            String input = "{\"plain\": \"json\"}";
            agent.testExtractAndLogThinking(input);

            assertNull(agent.getLastCapturedThinking());
        }
    }

    @Nested
    @DisplayName("Retryable Error Detection")
    class RetryableErrorDetection {

        @Test
        @DisplayName("Timeout errors are retryable")
        void timeoutErrorsAreRetryable() {
            assertTrue(agent.testIsRetryableError(new Exception("Connection timeout")));
        }

        @Test
        @DisplayName("Rate limit errors are retryable")
        void rateLimitErrorsAreRetryable() {
            assertTrue(agent.testIsRetryableError(new Exception("Rate limit exceeded")));
        }

        @Test
        @DisplayName("503 errors are retryable")
        void serviceUnavailableErrorsAreRetryable() {
            assertTrue(agent.testIsRetryableError(new Exception("HTTP 503 Service Unavailable")));
        }

        @Test
        @DisplayName("529 errors are retryable")
        void overloadedErrorsAreRetryable() {
            assertTrue(agent.testIsRetryableError(new Exception("529 Overloaded")));
        }

        @Test
        @DisplayName("Null message errors default to retryable")
        void nullMessageErrorsAreRetryable() {
            assertTrue(agent.testIsRetryableError(new Exception((String) null)));
        }
    }

    /**
     * Testable subclass of BaseAgent that exposes protected methods for testing.
     */
    private static class TestableAgent extends BaseAgent {
        private boolean processed = false;
        private String lastCapturedThinking = null;

        TestableAgent(ChatModel model) {
            super(model, "Test system prompt");
        }

        TestableAgent(ChatModel model, int maxRetries, Duration delay, double backoff) {
            super(model, "Test system prompt", maxRetries, delay, backoff);
        }

        @Override
        public AgentRole getRole() {
            return AgentRole.RESEARCHER;
        }

        @Override
        protected String buildUserPrompt(PublishingDocument document) {
            return "Test prompt for " + document.getPageName();
        }

        @Override
        protected void parseAndApplyResponse(String response, PublishingDocument document) {
            processed = true;
            // No-op for testing
        }

        @Override
        public boolean validate(PublishingDocument document) {
            return processed;
        }

        boolean wasProcessed() {
            return processed;
        }

        // Override to capture thinking content for test verification
        @Override
        protected void logThinkingContent(String thinkingContent) {
            this.lastCapturedThinking = thinkingContent;
            super.logThinkingContent(thinkingContent);
        }

        String getLastCapturedThinking() {
            return lastCapturedThinking;
        }

        void clearCapturedThinking() {
            lastCapturedThinking = null;
        }

        // Expose protected methods for testing
        String testCleanJsonResponse(String response) {
            return cleanJsonResponse(response);
        }

        JsonNode testParseJson(String json) throws JsonProcessingException {
            return parseJson(json);
        }

        String testGetStringOrDefault(JsonNode node, String field, String defaultValue) {
            return getStringOrDefault(node, field, defaultValue);
        }

        int testGetIntOrDefault(JsonNode node, String field, int defaultValue) {
            return getIntOrDefault(node, field, defaultValue);
        }

        double testGetDoubleOrDefault(JsonNode node, String field, double defaultValue) {
            return getDoubleOrDefault(node, field, defaultValue);
        }

        boolean testIsRetryableError(Exception e) {
            return isRetryableError(e);
        }

        String testExtractAndLogThinking(String response) {
            return extractAndLogThinking(response);
        }
    }
}
