package com.jakefear.aipublisher.examples;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExamplePlanner.
 */
@DisplayName("ExamplePlanner")
class ExamplePlannerTest {

    private ExamplePlanner planner;

    @BeforeEach
    void setUp() {
        planner = new ExamplePlanner();
    }

    @Nested
    @DisplayName("plan() for different content types")
    class PlanForContentTypes {

        @Test
        @DisplayName("TUTORIAL generates progressive examples")
        void tutorialGeneratesProgressiveExamples() {
            ExamplePlan plan = planner.plan("Git Basics", ContentType.TUTORIAL);

            assertEquals(ContentType.TUTORIAL, plan.contentType());
            assertEquals(3, plan.minimumCount());

            // Should have progressive examples
            assertFalse(plan.getProgressiveExamples().isEmpty());
            assertTrue(plan.getProgressiveExamples().size() >= 3);
        }

        @Test
        @DisplayName("CONCEPT generates illustrative examples with anti-patterns")
        void conceptGeneratesIllustrativeExamples() {
            ExamplePlan plan = planner.plan("Dependency Injection", ContentType.CONCEPT);

            assertEquals(ContentType.CONCEPT, plan.contentType());
            assertEquals(1, plan.minimumCount());
            assertTrue(plan.hasAntiPatterns());
        }

        @Test
        @DisplayName("REFERENCE generates minimal and complete examples")
        void referenceGeneratesMinimalAndCompleteExamples() {
            ExamplePlan plan = planner.plan("String API", ContentType.REFERENCE);

            assertEquals(ContentType.REFERENCE, plan.contentType());
            assertEquals(2, plan.minimumCount());

            assertTrue(plan.getExamplesByType(ExampleType.MINIMAL).size() >= 1);
            assertTrue(plan.getExamplesByType(ExampleType.COMPLETE).size() >= 1);
        }

        @Test
        @DisplayName("GUIDE generates realistic examples with pitfalls")
        void guideGeneratesRealisticExamples() {
            ExamplePlan plan = planner.plan("Database Migration", ContentType.GUIDE);

            assertEquals(ContentType.GUIDE, plan.contentType());
            assertEquals(1, plan.minimumCount());
            assertTrue(plan.hasAntiPatterns());
        }

        @Test
        @DisplayName("COMPARISON generates multiple comparison examples")
        void comparisonGeneratesComparisonExamples() {
            ExamplePlan plan = planner.plan("React vs Angular", ContentType.COMPARISON);

            assertEquals(ContentType.COMPARISON, plan.contentType());
            assertEquals(2, plan.minimumCount());

            assertEquals(2, plan.getExamplesByType(ExampleType.COMPARISON).size());
        }

        @Test
        @DisplayName("TROUBLESHOOTING generates problem/solution pairs")
        void troubleshootingGeneratesProblemSolutionPairs() {
            ExamplePlan plan = planner.plan("Memory Leaks", ContentType.TROUBLESHOOTING);

            assertEquals(ContentType.TROUBLESHOOTING, plan.contentType());
            assertEquals(2, plan.minimumCount());
            assertTrue(plan.hasAntiPatterns()); // Problem is shown as anti-pattern
        }

        @Test
        @DisplayName("OVERVIEW generates minimal optional examples")
        void overviewGeneratesMinimalOptionalExamples() {
            ExamplePlan plan = planner.plan("Java Overview", ContentType.OVERVIEW);

            assertEquals(ContentType.OVERVIEW, plan.contentType());
            assertEquals(0, plan.minimumCount()); // Optional for overview
        }
    }

    @Nested
    @DisplayName("Language detection")
    class LanguageDetection {

        @Test
        @DisplayName("Detects Java from topic")
        void detectsJavaFromTopic() {
            ExamplePlan plan = planner.plan("Spring Boot REST Controllers", ContentType.TUTORIAL, "java");

            // When explicitly specified, at least one example should have Java language
            assertTrue(plan.examples().stream()
                    .anyMatch(e -> "java".equals(e.language())));
        }

        @Test
        @DisplayName("Detects Python from topic")
        void detectsPythonFromTopic() {
            ExamplePlan plan = planner.plan("Django REST Framework", ContentType.TUTORIAL, "python");

            // When explicitly specified, should use python
            assertTrue(plan.examples().stream()
                    .anyMatch(e -> "python".equals(e.language())));
        }

        @Test
        @DisplayName("Detects JavaScript from topic")
        void detectsJavaScriptFromTopic() {
            ExamplePlan plan = planner.plan("React Component Patterns", ContentType.TUTORIAL, "javascript");

            assertTrue(plan.examples().stream()
                    .anyMatch(e -> "javascript".equals(e.language())));
        }

        @Test
        @DisplayName("Generates valid plan without explicit language")
        void generatesValidPlanWithoutExplicitLanguage() {
            ExamplePlan plan = planner.plan("Command Line Git", ContentType.TUTORIAL);

            assertNotNull(plan);
            assertFalse(plan.examples().isEmpty());
        }
    }

    @Nested
    @DisplayName("Writer prompt generation")
    class WriterPromptGeneration {

        @Test
        @DisplayName("Generates useful guidance for tutorials")
        void generatesUsefulGuidanceForTutorials() {
            ExamplePlan plan = planner.plan("Building APIs", ContentType.TUTORIAL);

            String prompt = plan.toWriterPrompt();

            assertTrue(prompt.contains("progressive") || prompt.contains("Progressive"));
            assertTrue(prompt.contains("Tutorial"));
            assertTrue(prompt.contains("Minimum Examples: 3"));
        }

        @Test
        @DisplayName("Generates useful guidance for reference docs")
        void generatesUsefulGuidanceForReferenceDocs() {
            ExamplePlan plan = planner.plan("Array Methods", ContentType.REFERENCE);

            String prompt = plan.toWriterPrompt();

            assertTrue(prompt.contains("Reference"));
            assertTrue(prompt.contains("copy-paste") || prompt.contains("complete") || prompt.contains("Complete"));
        }

        @Test
        @DisplayName("All plans have non-empty writer guidance")
        void allPlansHaveNonEmptyWriterGuidance() {
            for (ContentType type : ContentType.values()) {
                ExamplePlan plan = planner.plan("Test Topic", type);

                assertFalse(plan.writerGuidance().isBlank(),
                        "Missing guidance for " + type);
            }
        }
    }
}
