package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import dev.langchain4j.model.chat.ChatLanguageModel;
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
    private ChatLanguageModel mockModel;

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
            when(mockModel.generate(anyString())).thenReturn("success");

            agent.process(document);

            verify(mockModel, times(1)).generate(anyString());
            assertTrue(agent.wasProcessed());
        }

        @Test
        @DisplayName("Retries on transient failure")
        void retriesOnTransientFailure() {
            when(mockModel.generate(anyString()))
                    .thenThrow(new RuntimeException("timeout"))
                    .thenReturn("success");

            // Use a fast retry agent for testing
            TestableAgent fastAgent = new TestableAgent(mockModel, 3, Duration.ofMillis(10), 1.0);
            fastAgent.process(document);

            verify(mockModel, times(2)).generate(anyString());
        }

        @Test
        @DisplayName("Throws after max retries")
        void throwsAfterMaxRetries() {
            when(mockModel.generate(anyString()))
                    .thenThrow(new RuntimeException("timeout"));

            TestableAgent fastAgent = new TestableAgent(mockModel, 2, Duration.ofMillis(10), 1.0);

            AgentException exception = assertThrows(AgentException.class,
                    () -> fastAgent.process(document));

            assertEquals(AgentRole.RESEARCHER, exception.getAgentRole());
            verify(mockModel, times(2)).generate(anyString());
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

        TestableAgent(ChatLanguageModel model) {
            super(model, "Test system prompt");
        }

        TestableAgent(ChatLanguageModel model, int maxRetries, Duration delay, double backoff) {
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
    }
}
