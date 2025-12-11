package com.jakefear.aipublisher;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Disabled - replaced by AiPublisherCommand with Picocli
// @Component
public class HelloClaudeRunner implements CommandLineRunner {

    @Value("${anthropic.api.key}")
    private String apiKey;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String modelName;

    @Override
    public void run(String... args) {
        System.out.println("=".repeat(60));
        System.out.println("AI Publisher - Hello World with Claude");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxTokens(1024)
                .build();

        String prompt = "Say hello and briefly introduce yourself in 2-3 sentences. " +
                "Mention that you'll be helping with a multi-agent article writing workflow.";

        System.out.println("\nSending prompt to Claude (" + modelName + ")...\n");

        String response = model.generate(prompt);

        System.out.println("Claude's response:");
        System.out.println("-".repeat(40));
        System.out.println(response);
        System.out.println("-".repeat(40));
        System.out.println("\nHello World test complete!");
    }
}
