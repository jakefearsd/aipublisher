package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.document.AgentContribution;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.util.JsonParsingUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Base implementation for all agents, providing common functionality:
 * - LangChain4j integration
 * - JSON response parsing
 * - Retry logic with exponential backoff
 * - Contribution recording
 */
public abstract class BaseAgent implements Agent {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected ChatLanguageModel model;
    protected final String systemPrompt;
    protected final ObjectMapper objectMapper;

    // Retry configuration
    private final int maxRetries;
    private final Duration initialRetryDelay;
    private final double backoffMultiplier;

    /**
     * Constructor for Spring setter injection - model will be set later.
     */
    protected BaseAgent(String systemPrompt) {
        this(null, systemPrompt, 3, Duration.ofSeconds(1), 2.0);
    }

    protected BaseAgent(ChatLanguageModel model, String systemPrompt) {
        this(model, systemPrompt, 3, Duration.ofSeconds(1), 2.0);
    }

    protected BaseAgent(
            ChatLanguageModel model,
            String systemPrompt,
            int maxRetries,
            Duration initialRetryDelay,
            double backoffMultiplier
    ) {
        this.model = model;
        this.systemPrompt = systemPrompt;
        this.objectMapper = new ObjectMapper();
        this.maxRetries = maxRetries;
        this.initialRetryDelay = initialRetryDelay;
        this.backoffMultiplier = backoffMultiplier;
    }

    @Override
    public PublishingDocument process(PublishingDocument document) throws AgentException {
        log.info("Starting {} processing for document: {}", getName(), document.getPageName());

        Instant startTime = Instant.now();
        String response = null;
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String userPrompt = buildUserPrompt(document);
                log.debug("Sending prompt to Claude (attempt {}/{})", attempt, maxRetries);

                response = callModel(userPrompt);
                log.debug("Received response of {} characters", response.length());

                // Parse and apply the response
                parseAndApplyResponse(response, document);

                // Record the contribution
                Duration processingTime = Duration.between(startTime, Instant.now());
                document.addContribution(AgentContribution.withMetrics(
                        getRole().name(),
                        processingTime,
                        Map.of(
                                "responseLength", response.length(),
                                "attempts", attempt
                        )
                ));

                log.info("{} completed successfully in {} ms",
                        getName(), processingTime.toMillis());
                return document;

            } catch (JsonProcessingException e) {
                lastException = e;
                log.warn("JSON parsing failed on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries) {
                    sleepWithBackoff(attempt);
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Processing failed on attempt {}/{}: {}", attempt, maxRetries, e.getMessage());

                if (attempt < maxRetries && isRetryableError(e)) {
                    sleepWithBackoff(attempt);
                } else if (!isRetryableError(e)) {
                    break; // Non-retryable error, stop immediately
                }
            }
        }

        // All retries exhausted
        String errorMsg = String.format("%s failed after %d attempts", getName(), maxRetries);
        log.error(errorMsg, lastException);
        throw new AgentException(getRole(), errorMsg, lastException);
    }

    /**
     * Build the user prompt for this agent based on the document state.
     */
    protected abstract String buildUserPrompt(PublishingDocument document);

    /**
     * Parse the model response and apply it to the document.
     */
    protected abstract void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException;

    /**
     * Call the language model with the given user prompt.
     */
    protected String callModel(String userPrompt) {
        // LangChain4j handles the system prompt + user prompt combination
        String fullPrompt = systemPrompt + "\n\n---\n\n" + userPrompt;
        return model.generate(fullPrompt);
    }

    /**
     * Parse a JSON response string into a JsonNode.
     */
    protected JsonNode parseJson(String response) throws JsonProcessingException {
        return JsonParsingUtils.parseJson(response, objectMapper);
    }

    /**
     * Clean up a JSON response that might be wrapped in markdown code blocks.
     * @deprecated Use {@link JsonParsingUtils#cleanJsonResponse(String)} directly
     */
    @Deprecated
    protected String cleanJsonResponse(String response) {
        return JsonParsingUtils.cleanJsonResponse(response);
    }

    /**
     * Check if an exception represents a retryable error.
     */
    protected boolean isRetryableError(Exception e) {
        // Rate limits, timeouts, and temporary failures are retryable
        String message = e.getMessage();
        if (message == null) {
            return true;
        }

        message = message.toLowerCase();
        return message.contains("timeout") ||
                message.contains("rate limit") ||
                message.contains("temporarily") ||
                message.contains("overloaded") ||
                message.contains("503") ||
                message.contains("529");
    }

    /**
     * Sleep with exponential backoff before retry.
     */
    private void sleepWithBackoff(int attempt) {
        long delayMs = (long) (initialRetryDelay.toMillis() * Math.pow(backoffMultiplier, attempt - 1));
        log.debug("Sleeping for {} ms before retry", delayMs);
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AgentException(getRole(), "Interrupted during retry delay", e, false);
        }
    }

    /**
     * Get a string from a JSON node, with a default if missing or null.
     */
    protected String getStringOrDefault(JsonNode node, String field, String defaultValue) {
        return JsonParsingUtils.getStringOrDefault(node, field, defaultValue);
    }

    /**
     * Get a multi-line string from a JSON node, normalizing escaped newlines.
     * Use this for content fields like wikiContent where LLMs may incorrectly escape newlines.
     */
    protected String getMultilineString(JsonNode node, String field, String defaultValue) {
        return JsonParsingUtils.getMultilineString(node, field, defaultValue);
    }

    /**
     * Get an int from a JSON node, with a default if missing or null.
     */
    protected int getIntOrDefault(JsonNode node, String field, int defaultValue) {
        return JsonParsingUtils.getIntOrDefault(node, field, defaultValue);
    }

    /**
     * Get a double from a JSON node, with a default if missing or null.
     */
    protected double getDoubleOrDefault(JsonNode node, String field, double defaultValue) {
        return JsonParsingUtils.getDoubleOrDefault(node, field, defaultValue);
    }
}
