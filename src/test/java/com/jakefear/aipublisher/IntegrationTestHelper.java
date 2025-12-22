package com.jakefear.aipublisher;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

/**
 * Helper class for integration tests that need an LLM.
 * Uses Ollama for local inference (no API key required).
 *
 * Configuration:
 * - OLLAMA_BASE_URL: Ollama server URL (default: http://inference.jakefear.com:11434)
 * - OLLAMA_MODEL: Model to use (default: qwen2.5:14b)
 */
public final class IntegrationTestHelper {

    private static final String DEFAULT_OLLAMA_URL = "http://inference.jakefear.com:11434";
    private static final String DEFAULT_OLLAMA_MODEL = "qwen2.5:14b";

    private IntegrationTestHelper() {
        // Utility class
    }

    /**
     * Check if the Ollama LLM provider is available for integration tests.
     * Performs a connectivity check to the Ollama server.
     */
    public static boolean isLlmAvailable() {
        return isOllamaReachable();
    }

    /**
     * Check if Ollama server is reachable.
     */
    public static boolean isOllamaReachable() {
        String baseUrl = getOllamaBaseUrl();
        try {
            URI uri = URI.create(baseUrl + "/api/tags");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the name of the active LLM provider.
     */
    public static String getProviderName() {
        return "Ollama (" + getOllamaModel() + " @ " + getOllamaBaseUrl() + ")";
    }

    /**
     * Build a ChatLanguageModel for integration tests using Ollama.
     *
     * @param temperature The temperature setting (0.0-1.0)
     * @return Configured ChatLanguageModel
     * @throws IllegalStateException if Ollama is not reachable
     */
    public static ChatLanguageModel buildModel(double temperature) {
        if (!isOllamaReachable()) {
            throw new IllegalStateException(
                    "Ollama server not reachable at " + getOllamaBaseUrl() +
                    ". Ensure Ollama is running.");
        }
        return buildOllamaModel(temperature);
    }

    /**
     * Build an Ollama model for testing.
     */
    public static ChatLanguageModel buildOllamaModel(double temperature) {
        String baseUrl = getOllamaBaseUrl();
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
     * Get the Ollama base URL from environment or use default.
     */
    public static String getOllamaBaseUrl() {
        String baseUrl = System.getenv("OLLAMA_BASE_URL");
        return (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_OLLAMA_URL;
    }

    /**
     * Get the Ollama model from environment or use default.
     */
    public static String getOllamaModel() {
        String model = System.getenv("OLLAMA_MODEL");
        return (model != null && !model.isBlank()) ? model : DEFAULT_OLLAMA_MODEL;
    }
}
