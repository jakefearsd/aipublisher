package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.document.TopicBrief;
import com.jakefear.aipublisher.pipeline.PipelineResult;
import com.jakefear.aipublisher.pipeline.PublishingPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Command-line runner for executing the publishing pipeline.
 *
 * This runner is activated when command-line arguments are provided or
 * when the "publisher.auto-run" property is set to true.
 *
 * Usage:
 *   java -jar aipublisher.jar "Topic Name" "target audience" 1000
 *   java -jar aipublisher.jar --topic="Topic Name" --audience="developers" --words=1000
 */
// Disabled - replaced by AiPublisherCommand with Picocli
// @Component
// @ConditionalOnProperty(name = "publisher.enabled", havingValue = "true", matchIfMissing = true)
public class PublishingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PublishingRunner.class);

    private final PublishingPipeline pipeline;

    public PublishingRunner(PublishingPipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            log.info("No arguments provided. Use: java -jar aipublisher.jar \"Topic\" \"audience\" wordCount");
            log.info("Or set publisher.topic, publisher.audience, publisher.wordCount in application.properties");
            return;
        }

        TopicBrief topicBrief = parseArguments(args);
        if (topicBrief == null) {
            return;
        }

        log.info("=".repeat(60));
        log.info("AI Publisher - Starting article generation");
        log.info("=".repeat(60));
        log.info("Topic: {}", topicBrief.topic());
        log.info("Audience: {}", topicBrief.targetAudience());
        log.info("Target words: {}", topicBrief.targetWordCount());
        log.info("=".repeat(60));

        PipelineResult result = pipeline.execute(topicBrief);

        log.info("=".repeat(60));
        if (result.success()) {
            log.info("SUCCESS! Article published.");
            log.info("Output: {}", result.outputPath());
            log.info("Quality score: {}", result.document().getFinalArticle().qualityScore());
            log.info("Word count: {}", result.document().getFinalArticle().estimateWordCount());
        } else {
            log.error("FAILED at phase: {}", result.failedAtState());
            log.error("Error: {}", result.errorMessage());
        }
        log.info("Total time: {} ms", result.totalTime().toMillis());
        log.info("=".repeat(60));
    }

    /**
     * Parse command-line arguments into a TopicBrief.
     *
     * Supports two formats:
     * 1. Positional: topic audience wordCount
     * 2. Named: --topic=X --audience=Y --words=Z
     */
    private TopicBrief parseArguments(String[] args) {
        String topic = null;
        String audience = "general readers";
        int wordCount = 800;
        List<String> requiredSections = List.of();
        List<String> relatedPages = List.of();

        // Check for named arguments
        boolean hasNamedArgs = false;
        for (String arg : args) {
            if (arg.startsWith("--")) {
                hasNamedArgs = true;
                break;
            }
        }

        if (hasNamedArgs) {
            for (String arg : args) {
                if (arg.startsWith("--topic=")) {
                    topic = arg.substring(8);
                } else if (arg.startsWith("--audience=")) {
                    audience = arg.substring(11);
                } else if (arg.startsWith("--words=")) {
                    try {
                        wordCount = Integer.parseInt(arg.substring(8));
                    } catch (NumberFormatException e) {
                        log.error("Invalid word count: {}", arg.substring(8));
                        return null;
                    }
                } else if (arg.startsWith("--sections=")) {
                    requiredSections = List.of(arg.substring(11).split(","));
                } else if (arg.startsWith("--related=")) {
                    relatedPages = List.of(arg.substring(10).split(","));
                }
            }
        } else {
            // Positional arguments
            if (args.length >= 1) {
                topic = args[0];
            }
            if (args.length >= 2) {
                audience = args[1];
            }
            if (args.length >= 3) {
                try {
                    wordCount = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    log.error("Invalid word count: {}", args[2]);
                    return null;
                }
            }
        }

        if (topic == null || topic.isBlank()) {
            log.error("Topic is required.");
            printUsage();
            return null;
        }

        return new TopicBrief(topic, audience, wordCount, requiredSections, relatedPages, List.of());
    }

    private void printUsage() {
        log.info("Usage:");
        log.info("  Positional: java -jar aipublisher.jar \"Topic Name\" \"target audience\" wordCount");
        log.info("  Named: java -jar aipublisher.jar --topic=\"Topic\" --audience=\"developers\" --words=1000");
        log.info("");
        log.info("Arguments:");
        log.info("  topic     - The topic to write about (required)");
        log.info("  audience  - Target audience (default: general readers)");
        log.info("  words     - Target word count (default: 800)");
        log.info("  sections  - Comma-separated required sections (optional)");
        log.info("  related   - Comma-separated related pages (optional)");
    }
}
