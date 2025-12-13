package com.jakefear.aipublisher.examples;

import java.util.Objects;

/**
 * Specification for an example to be included in an article.
 */
public record ExampleSpec(
        /**
         * Unique identifier for this example within the article.
         */
        String id,

        /**
         * The type of example (minimal, realistic, anti-pattern, etc.).
         */
        ExampleType type,

        /**
         * Brief description of what the example demonstrates.
         */
        String purpose,

        /**
         * The concept or technique being illustrated.
         */
        String concept,

        /**
         * Optional programming language for code examples.
         */
        String language,

        /**
         * Order in the article (for progressive examples).
         */
        int sequence,

        /**
         * Whether this example is required or optional.
         */
        boolean required
) {
    public ExampleSpec {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(purpose, "purpose must not be null");
        Objects.requireNonNull(concept, "concept must not be null");

        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must not be blank");
        }
        if (concept.isBlank()) {
            throw new IllegalArgumentException("concept must not be blank");
        }
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence must be non-negative");
        }
    }

    /**
     * Create a minimal example spec.
     */
    public static ExampleSpec minimal(String id, String concept, String language) {
        return new ExampleSpec(
                id,
                ExampleType.MINIMAL,
                "Demonstrate basic " + concept + " syntax",
                concept,
                language,
                0,
                true
        );
    }

    /**
     * Create a realistic example spec.
     */
    public static ExampleSpec realistic(String id, String concept, String purpose, String language) {
        return new ExampleSpec(
                id,
                ExampleType.REALISTIC,
                purpose,
                concept,
                language,
                0,
                true
        );
    }

    /**
     * Create a progressive example spec.
     */
    public static ExampleSpec progressive(String id, String concept, String purpose, int sequence) {
        return new ExampleSpec(
                id,
                ExampleType.PROGRESSIVE,
                purpose,
                concept,
                null,
                sequence,
                true
        );
    }

    /**
     * Create a progressive example spec with language.
     */
    public static ExampleSpec progressive(String id, String concept, String purpose, int sequence, String language) {
        return new ExampleSpec(
                id,
                ExampleType.PROGRESSIVE,
                purpose,
                concept,
                language,
                sequence,
                true
        );
    }

    /**
     * Create an anti-pattern example spec.
     */
    public static ExampleSpec antiPattern(String id, String concept, String whatToAvoid) {
        return new ExampleSpec(
                id,
                ExampleType.ANTI_PATTERN,
                "Show incorrect approach: " + whatToAvoid,
                concept,
                null,
                0,
                false
        );
    }

    /**
     * Create a comparison example spec.
     */
    public static ExampleSpec comparison(String id, String concept, String comparisonPurpose) {
        return new ExampleSpec(
                id,
                ExampleType.COMPARISON,
                comparisonPurpose,
                concept,
                null,
                0,
                true
        );
    }

    /**
     * Check if this example requires code.
     */
    public boolean requiresCode() {
        return language != null && !language.isBlank();
    }

    /**
     * Get a description suitable for inclusion in writer prompts.
     */
    public String toPromptDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("- Example ").append(id).append(" (").append(type.getDisplayName()).append("): ");
        sb.append(purpose);
        if (language != null && !language.isBlank()) {
            sb.append(" [").append(language).append("]");
        }
        if (required) {
            sb.append(" (REQUIRED)");
        }
        return sb.toString();
    }
}
