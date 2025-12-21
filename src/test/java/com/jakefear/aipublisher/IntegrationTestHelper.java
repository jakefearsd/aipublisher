package com.jakefear.aipublisher;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.time.Duration;

/**
 * Helper class for integration tests that need an LLM.
 * Supports both Anthropic and Ollama based on environment configuration.
 *
 * Configuration priority:
 * 1. If OLLAMA_BASE_URL is set, uses Ollama (free local inference)
 * 2. If ANTHROPIC_API_KEY is set, uses Anthropic (paid API)
 * 3. Otherwise, tests are skipped
 *
 * Environment variables:
 * - OLLAMA_BASE_URL: Ollama server URL (e.g., http://localhost:11434)
 * - OLLAMA_MODEL: Model to use (default: llama3.2)
 * - ANTHROPIC_API_KEY: Anthropic API key
 * - ANTHROPIC_MODEL: Model to use (default: claude-sonnet-4-20250514)
 */
public final class IntegrationTestHelper {

    private IntegrationTestHelper() {
        // Utility class
    }

    /**
     * Check if any LLM provider is available for integration tests.
     */
    public static boolean isLlmAvailable() {
        return isOllamaAvailable() || isAnthropicAvailable();
    }

    /**
     * Check if Ollama is configured and available.
     */
    public static boolean isOllamaAvailable() {
        String baseUrl = System.getenv("OLLAMA_BASE_URL");
        return baseUrl != null && !baseUrl.isBlank();
    }

    /**
     * Check if Anthropic API is configured.
     */
    public static boolean isAnthropicAvailable() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Get the name of the active LLM provider.
     */
    public static String getProviderName() {
        if (isOllamaAvailable()) {
            return "Ollama (" + getOllamaModel() + ")";
        } else if (isAnthropicAvailable()) {
            return "Anthropic (" + getAnthropicModel() + ")";
        }
        return "None";
    }

    /**
     * Build a ChatLanguageModel for integration tests.
     * Prefers Ollama (free) over Anthropic (paid).
     *
     * @param temperature The temperature setting (0.0-1.0)
     * @return Configured ChatLanguageModel
     * @throws IllegalStateException if no LLM provider is configured
     */
    public static ChatLanguageModel buildModel(double temperature) {
        if (isOllamaAvailable()) {
            return buildOllamaModel(temperature);
        } else if (isAnthropicAvailable()) {
            return buildAnthropicModel(temperature);
        }
        throw new IllegalStateException(
                "No LLM provider configured. Set OLLAMA_BASE_URL or ANTHROPIC_API_KEY.");
    }

    /**
     * Build an Ollama model for testing.
     */
    public static ChatLanguageModel buildOllamaModel(double temperature) {
        String baseUrl = System.getenv("OLLAMA_BASE_URL");
        String model = getOllamaModel();

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .numPredict(4096)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    /**
     * Build an Anthropic model for testing.
     */
    public static ChatLanguageModel buildAnthropicModel(double temperature) {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        String model = getAnthropicModel();

        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .maxTokens(4096)
                .temperature(temperature)
                .build();
    }

    private static String getOllamaModel() {
        String model = System.getenv("OLLAMA_MODEL");
        return (model != null && !model.isBlank()) ? model : "qwen2.5:14b";
    }

    private static String getAnthropicModel() {
        String model = System.getenv("ANTHROPIC_MODEL");
        return (model != null && !model.isBlank()) ? model : "claude-sonnet-4-20250514";
    }
}
