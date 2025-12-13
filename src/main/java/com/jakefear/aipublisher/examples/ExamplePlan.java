package com.jakefear.aipublisher.examples;

import com.jakefear.aipublisher.content.ContentType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Plan for examples in an article.
 * Specifies what examples should be included based on content type and topic.
 */
public record ExamplePlan(
        /**
         * The topic the examples are for.
         */
        String topic,

        /**
         * The content type that determined the example requirements.
         */
        ContentType contentType,

        /**
         * List of example specifications in order.
         */
        List<ExampleSpec> examples,

        /**
         * Minimum number of examples required.
         */
        int minimumCount,

        /**
         * Notes for the writer about example expectations.
         */
        String writerGuidance
) {
    public ExamplePlan {
        Objects.requireNonNull(topic, "topic must not be null");
        Objects.requireNonNull(contentType, "contentType must not be null");
        examples = examples == null ? List.of() : List.copyOf(examples);
        writerGuidance = writerGuidance == null ? "" : writerGuidance;

        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        if (minimumCount < 0) {
            throw new IllegalArgumentException("minimumCount must be non-negative");
        }
    }

    /**
     * Get examples of a specific type.
     */
    public List<ExampleSpec> getExamplesByType(ExampleType type) {
        return examples.stream()
                .filter(e -> e.type() == type)
                .collect(Collectors.toList());
    }

    /**
     * Get required examples only.
     */
    public List<ExampleSpec> getRequiredExamples() {
        return examples.stream()
                .filter(ExampleSpec::required)
                .collect(Collectors.toList());
    }

    /**
     * Get progressive examples in order.
     */
    public List<ExampleSpec> getProgressiveExamples() {
        return examples.stream()
                .filter(e -> e.type() == ExampleType.PROGRESSIVE)
                .sorted(Comparator.comparingInt(ExampleSpec::sequence))
                .collect(Collectors.toList());
    }

    /**
     * Check if this plan has anti-pattern examples.
     */
    public boolean hasAntiPatterns() {
        return examples.stream().anyMatch(e -> e.type() == ExampleType.ANTI_PATTERN);
    }

    /**
     * Check if the minimum example count is satisfied.
     */
    public boolean meetsMinimum(int actualCount) {
        return actualCount >= minimumCount;
    }

    /**
     * Generate a prompt section for the writer agent.
     */
    public String toWriterPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("## Example Requirements\n\n");
        sb.append("Content Type: ").append(contentType.getDisplayName()).append("\n");
        sb.append("Minimum Examples: ").append(minimumCount).append("\n\n");

        if (!writerGuidance.isBlank()) {
            sb.append("Guidance: ").append(writerGuidance).append("\n\n");
        }

        if (!examples.isEmpty()) {
            sb.append("Planned Examples:\n");
            for (ExampleSpec spec : examples) {
                sb.append(spec.toPromptDescription()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Create an empty plan (no examples required).
     */
    public static ExamplePlan empty(String topic, ContentType contentType) {
        return new ExamplePlan(
                topic,
                contentType,
                List.of(),
                0,
                "No specific examples required for this content type."
        );
    }

    /**
     * Builder for creating example plans.
     */
    public static Builder builder(String topic, ContentType contentType) {
        return new Builder(topic, contentType);
    }

    public static class Builder {
        private final String topic;
        private final ContentType contentType;
        private final List<ExampleSpec> examples = new ArrayList<>();
        private int minimumCount = 1;
        private String writerGuidance = "";

        private Builder(String topic, ContentType contentType) {
            this.topic = topic;
            this.contentType = contentType;
        }

        public Builder addExample(ExampleSpec example) {
            examples.add(example);
            return this;
        }

        public Builder minimumCount(int count) {
            this.minimumCount = count;
            return this;
        }

        public Builder guidance(String guidance) {
            this.writerGuidance = guidance;
            return this;
        }

        public ExamplePlan build() {
            return new ExamplePlan(topic, contentType, examples, minimumCount, writerGuidance);
        }
    }
}
