package com.jakefear.aipublisher.examples;

import com.jakefear.aipublisher.content.ContentType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans examples for an article based on content type and topic.
 */
@Component
public class ExamplePlanner {

    /**
     * Generate an example plan for a given topic and content type.
     *
     * @param topic The article topic
     * @param contentType The type of content being written
     * @param language Optional programming language (for code-focused content)
     * @return An example plan with appropriate specifications
     */
    public ExamplePlan plan(String topic, ContentType contentType, String language) {
        return switch (contentType) {
            case TUTORIAL -> planTutorial(topic, language);
            case CONCEPT -> planConcept(topic, language);
            case REFERENCE -> planReference(topic, language);
            case GUIDE -> planGuide(topic, language);
            case COMPARISON -> planComparison(topic, language);
            case TROUBLESHOOTING -> planTroubleshooting(topic, language);
            case OVERVIEW -> planOverview(topic, language);
        };
    }

    /**
     * Generate an example plan for a given topic with automatic content type detection.
     */
    public ExamplePlan plan(String topic, ContentType contentType) {
        return plan(topic, contentType, detectLanguage(topic));
    }

    /**
     * Tutorials need progressive examples that build on each other.
     */
    private ExamplePlan planTutorial(String topic, String language) {
        List<ExampleSpec> examples = new ArrayList<>();

        // Start with minimal "hello world" style example
        examples.add(ExampleSpec.progressive(
                "step1",
                topic,
                "Basic setup and first working example",
                1,
                language
        ));

        // Build up complexity
        examples.add(ExampleSpec.progressive(
                "step2",
                topic,
                "Add error handling and validation",
                2,
                language
        ));

        examples.add(ExampleSpec.progressive(
                "step3",
                topic,
                "Complete working implementation with best practices",
                3,
                language
        ));

        return ExamplePlan.builder(topic, ContentType.TUTORIAL)
                .addExample(examples.get(0))
                .addExample(examples.get(1))
                .addExample(examples.get(2))
                .minimumCount(3)
                .guidance("Create progressive examples that build on each other. " +
                        "Start simple, then add complexity. Each example should compile/run independently.")
                .build();
    }

    /**
     * Concepts need clear, illustrative examples.
     */
    private ExamplePlan planConcept(String topic, String language) {
        ExamplePlan.Builder builder = ExamplePlan.builder(topic, ContentType.CONCEPT);

        builder.addExample(ExampleSpec.realistic(
                "main",
                topic,
                "Illustrate the core concept with a practical scenario",
                language
        ));

        // Anti-pattern to clarify what NOT to do
        builder.addExample(ExampleSpec.antiPattern(
                "antipattern",
                topic,
                "Common misconception or incorrect usage"
        ));

        return builder
                .minimumCount(1)
                .guidance("Use examples to illuminate the concept. " +
                        "Show WHY the concept matters, not just HOW it works.")
                .build();
    }

    /**
     * Reference docs need minimal, complete examples for each feature.
     */
    private ExamplePlan planReference(String topic, String language) {
        ExamplePlan.Builder builder = ExamplePlan.builder(topic, ContentType.REFERENCE);

        builder.addExample(ExampleSpec.minimal(
                "basic",
                topic,
                language
        ));

        builder.addExample(new ExampleSpec(
                "complete",
                ExampleType.COMPLETE,
                "Show all options and parameters",
                topic,
                language,
                0,
                true
        ));

        return builder
                .minimumCount(2)
                .guidance("Provide copy-paste ready examples. " +
                        "Include all necessary imports and context. Be precise and complete.")
                .build();
    }

    /**
     * Guides need realistic, practical examples.
     */
    private ExamplePlan planGuide(String topic, String language) {
        ExamplePlan.Builder builder = ExamplePlan.builder(topic, ContentType.GUIDE);

        builder.addExample(ExampleSpec.realistic(
                "scenario",
                topic,
                "Real-world scenario demonstrating the guide's approach",
                language
        ));

        builder.addExample(ExampleSpec.antiPattern(
                "pitfall",
                topic,
                "Common mistake to avoid"
        ));

        return builder
                .minimumCount(1)
                .guidance("Use realistic examples from actual projects. " +
                        "Include context about when to use each approach.")
                .build();
    }

    /**
     * Comparisons need side-by-side examples for each option.
     */
    private ExamplePlan planComparison(String topic, String language) {
        ExamplePlan.Builder builder = ExamplePlan.builder(topic, ContentType.COMPARISON);

        builder.addExample(ExampleSpec.comparison(
                "option-a",
                topic,
                "First approach implementation"
        ));

        builder.addExample(ExampleSpec.comparison(
                "option-b",
                topic,
                "Second approach implementation"
        ));

        return builder
                .minimumCount(2)
                .guidance("Show equivalent functionality using different approaches. " +
                        "Make comparisons fair and highlight trade-offs.")
                .build();
    }

    /**
     * Troubleshooting needs problem/solution pairs.
     */
    private ExamplePlan planTroubleshooting(String topic, String language) {
        ExamplePlan.Builder builder = ExamplePlan.builder(topic, ContentType.TROUBLESHOOTING);

        builder.addExample(ExampleSpec.antiPattern(
                "problem",
                topic,
                "Code that causes the problem"
        ));

        builder.addExample(ExampleSpec.realistic(
                "solution",
                topic,
                "Corrected code that fixes the issue",
                language
        ));

        return builder
                .minimumCount(2)
                .guidance("Show the problematic code first, then the fix. " +
                        "Explain WHY the fix works.")
                .build();
    }

    /**
     * Overviews need minimal, representative examples.
     */
    private ExamplePlan planOverview(String topic, String language) {
        return ExamplePlan.builder(topic, ContentType.OVERVIEW)
                .addExample(ExampleSpec.minimal(
                        "taste",
                        topic,
                        language
                ))
                .minimumCount(0)  // Optional for overviews
                .guidance("Keep examples brief - just enough to give a taste. " +
                        "Link to detailed pages for full examples.")
                .build();
    }

    /**
     * Detect programming language from topic if possible.
     */
    private String detectLanguage(String topic) {
        String lower = topic.toLowerCase();

        if (lower.contains("java") || lower.contains("spring") || lower.contains("maven")) {
            return "java";
        }
        if (lower.contains("python") || lower.contains("django") || lower.contains("flask")) {
            return "python";
        }
        if (lower.contains("javascript") || lower.contains("typescript") || lower.contains("react") ||
                lower.contains("node") || lower.contains("npm")) {
            return "javascript";
        }
        if (lower.contains("rust") || lower.contains("cargo")) {
            return "rust";
        }
        if (lower.contains("go") || lower.contains("golang")) {
            return "go";
        }
        if (lower.contains("sql") || lower.contains("database") || lower.contains("query")) {
            return "sql";
        }
        if (lower.contains("bash") || lower.contains("shell") || lower.contains("terminal") ||
                lower.contains("command line")) {
            return "bash";
        }

        // Default to no specific language
        return null;
    }
}
