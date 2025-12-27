package com.jakefear.aipublisher.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Configuration for LLM integration via LangChain4j.
 * Supports both Anthropic (Claude) and Ollama providers.
 *
 * Provider selection is controlled by the llm.provider property:
 * - "anthropic" (default): Uses Claude API
 * - "ollama": Uses local Ollama server
 */
@Configuration
public class LlmConfig {

    private static final Logger log = LoggerFactory.getLogger(LlmConfig.class);

    @Value("${llm.provider:anthropic}")
    private String provider;

    // Anthropic settings
    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String anthropicModel;

    @Value("${anthropic.max-tokens:4096}")
    private int anthropicMaxTokens;

    @Value("${anthropic.timeout:PT5M}")
    private Duration anthropicTimeout;

    // Ollama settings
    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:qwen3:14b}")
    private String ollamaModel;

    @Value("${ollama.timeout:PT5M}")
    private Duration ollamaTimeout;

    @Value("${ollama.num-predict:4096}")
    private int ollamaNumPredict;

    @Value("${ollama.num-ctx:8192}")
    private int ollamaNumCtx;

    @Value("${ollama.repeat-penalty:1.1}")
    private double ollamaRepeatPenalty;

    // Ollama thinking mode settings (for models that support chain-of-thought reasoning)
    @Value("${ollama.think:true}")
    private boolean ollamaThink;

    @Value("${ollama.return-thinking:true}")
    private boolean ollamaReturnThinking;

    // Temperature settings (shared across providers)
    @Value("${llm.temperature.research:0.3}")
    private double researchTemperature;

    @Value("${llm.temperature.writer:0.7}")
    private double writerTemperature;

    @Value("${llm.temperature.factchecker:0.1}")
    private double factCheckerTemperature;

    @Value("${llm.temperature.editor:0.5}")
    private double editorTemperature;

    @Value("${llm.temperature.critic:0.3}")
    private double criticTemperature;

    /**
     * Default chat model for general use.
     */
    @Bean
    @Primary
    public ChatModel chatModel() {
        return buildModel(0.7);
    }

    /**
     * Chat model configured for research tasks (lower temperature for factual accuracy).
     */
    @Bean
    public ChatModel researchChatModel() {
        return buildModel(researchTemperature);
    }

    /**
     * Chat model configured for writing tasks (higher temperature for creativity).
     */
    @Bean
    public ChatModel writerChatModel() {
        return buildModel(writerTemperature);
    }

    /**
     * Chat model configured for fact-checking (very low temperature for precision).
     */
    @Bean
    public ChatModel factCheckerChatModel() {
        return buildModel(factCheckerTemperature);
    }

    /**
     * Chat model configured for editing tasks (moderate temperature).
     */
    @Bean
    public ChatModel editorChatModel() {
        return buildModel(editorTemperature);
    }

    /**
     * Chat model configured for critic tasks (lower temperature for consistent evaluation).
     */
    @Bean
    public ChatModel criticChatModel() {
        return buildModel(criticTemperature);
    }

    private ChatModel buildModel(double temperature) {
        if ("ollama".equalsIgnoreCase(provider)) {
            return buildOllamaModel(temperature);
        } else {
            return buildAnthropicModel(temperature);
        }
    }

    private ChatModel buildAnthropicModel(double temperature) {
        String apiKey = resolveAnthropicApiKey();
        log.debug("Building Anthropic model: {} with temperature {}, timeout {}", anthropicModel, temperature, anthropicTimeout);
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(anthropicModel)
                .maxTokens(anthropicMaxTokens)
                .temperature(temperature)
                .timeout(anthropicTimeout)
                .build();
    }

    private ChatModel buildOllamaModel(double temperature) {
        log.info("Building Ollama model: {} at {} with temperature {}, numPredict {}, numCtx {}, repeatPenalty {}, think={}, returnThinking={}",
                ollamaModel, ollamaBaseUrl, temperature, ollamaNumPredict, ollamaNumCtx, ollamaRepeatPenalty, ollamaThink, ollamaReturnThinking);
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModel)
                .temperature(temperature)
                .numPredict(ollamaNumPredict)
                .numCtx(ollamaNumCtx)
                .repeatPenalty(ollamaRepeatPenalty)
                .timeout(ollamaTimeout)
                .think(ollamaThink)
                .returnThinking(ollamaReturnThinking)
                .build();
    }

    /**
     * Resolve Anthropic API key with priority: System property > Environment variable > Config property.
     */
    private String resolveAnthropicApiKey() {
        String key = System.getProperty("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        key = System.getenv("ANTHROPIC_API_KEY");
        if (key != null && !key.isBlank()) {
            return key;
        }
        return anthropicApiKey;
    }

    // Getters for testing and introspection

    public String getProvider() {
        return provider;
    }

    public String getModelName() {
        return "ollama".equalsIgnoreCase(provider) ? ollamaModel : anthropicModel;
    }

    public boolean isOllama() {
        return "ollama".equalsIgnoreCase(provider);
    }

    public boolean isAnthropic() {
        return "anthropic".equalsIgnoreCase(provider);
    }

    public boolean isThinkingEnabled() {
        return isOllama() && ollamaThink;
    }

    public boolean isReturnThinkingEnabled() {
        return isOllama() && ollamaReturnThinking;
    }
}
