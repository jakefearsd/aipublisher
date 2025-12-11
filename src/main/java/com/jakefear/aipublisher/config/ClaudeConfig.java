package com.jakefear.aipublisher.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Configuration for Claude/Anthropic API integration via LangChain4j.
 * Provides ChatLanguageModel beans for each agent with appropriate temperature settings.
 *
 * All beans are lazy-loaded to allow --help and --version to work without an API key.
 */
@Configuration
@Lazy
public class ClaudeConfig {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String modelName;

    @Value("${anthropic.max-tokens:4096}")
    private int maxTokens;

    @Value("${anthropic.temperature.research:0.3}")
    private double researchTemperature;

    @Value("${anthropic.temperature.writer:0.7}")
    private double writerTemperature;

    @Value("${anthropic.temperature.factchecker:0.1}")
    private double factCheckerTemperature;

    @Value("${anthropic.temperature.editor:0.5}")
    private double editorTemperature;

    /**
     * Default chat model for general use.
     */
    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return buildModel(0.7);
    }

    /**
     * Chat model configured for research tasks (lower temperature for factual accuracy).
     */
    @Bean
    public ChatLanguageModel researchChatModel() {
        return buildModel(researchTemperature);
    }

    /**
     * Chat model configured for writing tasks (higher temperature for creativity).
     */
    @Bean
    public ChatLanguageModel writerChatModel() {
        return buildModel(writerTemperature);
    }

    /**
     * Chat model configured for fact-checking (very low temperature for precision).
     */
    @Bean
    public ChatLanguageModel factCheckerChatModel() {
        return buildModel(factCheckerTemperature);
    }

    /**
     * Chat model configured for editing tasks (moderate temperature).
     */
    @Bean
    public ChatLanguageModel editorChatModel() {
        return buildModel(editorTemperature);
    }

    private ChatLanguageModel buildModel(double temperature) {
        return AnthropicChatModel.builder()
                .apiKey(resolveApiKey())
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    /**
     * Resolve API key with priority: System property > Environment variable > Config property.
     * This allows CLI options (-k, --key-file) to override the configured value.
     */
    private String resolveApiKey() {
        // Check system property first (set by CLI options)
        String key = System.getProperty("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        // Fall back to environment variable
        key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        // Finally use the configured value (which may also come from env var via Spring)
        return apiKey;
    }

    // Getters for testing and introspection

    public String getApiKey() {
        return apiKey;
    }

    public String getModelName() {
        return modelName;
    }

    public int getMaxTokens() {
        return maxTokens;
    }
}
