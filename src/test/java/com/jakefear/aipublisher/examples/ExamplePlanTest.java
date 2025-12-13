package com.jakefear.aipublisher.examples;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExamplePlan record.
 */
@DisplayName("ExamplePlan")
class ExamplePlanTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid example plan")
        void createsValidExamplePlan() {
            ExampleSpec spec = ExampleSpec.minimal("test", "concept", "java");
            ExamplePlan plan = new ExamplePlan(
                    "Git Basics",
                    ContentType.TUTORIAL,
                    List.of(spec),
                    2,
                    "Write progressive examples"
            );

            assertEquals("Git Basics", plan.topic());
            assertEquals(ContentType.TUTORIAL, plan.contentType());
            assertEquals(1, plan.examples().size());
            assertEquals(2, plan.minimumCount());
            assertEquals("Write progressive examples", plan.writerGuidance());
        }

        @Test
        @DisplayName("Rejects null topic")
        void rejectsNullTopic() {
            assertThrows(NullPointerException.class, () ->
                    new ExamplePlan(null, ContentType.CONCEPT, List.of(), 1, ""));
        }

        @Test
        @DisplayName("Rejects blank topic")
        void rejectsBlankTopic() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExamplePlan("  ", ContentType.CONCEPT, List.of(), 1, ""));
        }

        @Test
        @DisplayName("Rejects null content type")
        void rejectsNullContentType() {
            assertThrows(NullPointerException.class, () ->
                    new ExamplePlan("topic", null, List.of(), 1, ""));
        }

        @Test
        @DisplayName("Rejects negative minimum count")
        void rejectsNegativeMinimumCount() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExamplePlan("topic", ContentType.CONCEPT, List.of(), -1, ""));
        }

        @Test
        @DisplayName("Handles null examples list")
        void handlesNullExamplesList() {
            ExamplePlan plan = new ExamplePlan("topic", ContentType.CONCEPT, null, 0, "");
            assertNotNull(plan.examples());
            assertTrue(plan.examples().isEmpty());
        }

        @Test
        @DisplayName("Handles null guidance")
        void handlesNullGuidance() {
            ExamplePlan plan = new ExamplePlan("topic", ContentType.CONCEPT, List.of(), 0, null);
            assertNotNull(plan.writerGuidance());
            assertEquals("", plan.writerGuidance());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        private ExamplePlan createTestPlan() {
            return ExamplePlan.builder("Test Topic", ContentType.TUTORIAL)
                    .addExample(ExampleSpec.minimal("min", "concept", "java"))
                    .addExample(ExampleSpec.progressive("step1", "concept", "First step", 1))
                    .addExample(ExampleSpec.progressive("step2", "concept", "Second step", 2))
                    .addExample(ExampleSpec.antiPattern("bad", "concept", "Don't do this"))
                    .minimumCount(2)
                    .guidance("Include all steps")
                    .build();
        }

        @Test
        @DisplayName("getExamplesByType() filters correctly")
        void getExamplesByTypeFiltersCorrectly() {
            ExamplePlan plan = createTestPlan();

            List<ExampleSpec> progressive = plan.getExamplesByType(ExampleType.PROGRESSIVE);
            assertEquals(2, progressive.size());

            List<ExampleSpec> antiPattern = plan.getExamplesByType(ExampleType.ANTI_PATTERN);
            assertEquals(1, antiPattern.size());
        }

        @Test
        @DisplayName("getRequiredExamples() returns only required")
        void getRequiredExamplesReturnsOnlyRequired() {
            ExamplePlan plan = createTestPlan();

            List<ExampleSpec> required = plan.getRequiredExamples();
            // minimal and progressive are required, anti-pattern is not
            assertEquals(3, required.size());
            assertTrue(required.stream().allMatch(ExampleSpec::required));
        }

        @Test
        @DisplayName("getProgressiveExamples() returns sorted by sequence")
        void getProgressiveExamplesReturnsSortedBySequence() {
            ExamplePlan plan = createTestPlan();

            List<ExampleSpec> progressive = plan.getProgressiveExamples();
            assertEquals(2, progressive.size());
            assertEquals("step1", progressive.get(0).id());
            assertEquals("step2", progressive.get(1).id());
        }

        @Test
        @DisplayName("hasAntiPatterns() detects anti-pattern examples")
        void hasAntiPatternsDetectsAntiPatternExamples() {
            ExamplePlan planWithAnti = createTestPlan();
            assertTrue(planWithAnti.hasAntiPatterns());

            ExamplePlan planWithout = ExamplePlan.builder("topic", ContentType.REFERENCE)
                    .addExample(ExampleSpec.minimal("min", "concept", "java"))
                    .build();
            assertFalse(planWithout.hasAntiPatterns());
        }

        @Test
        @DisplayName("meetsMinimum() checks correctly")
        void meetsMinimumChecksCorrectly() {
            ExamplePlan plan = createTestPlan();

            assertTrue(plan.meetsMinimum(2));
            assertTrue(plan.meetsMinimum(3));
            assertFalse(plan.meetsMinimum(1));
        }
    }

    @Nested
    @DisplayName("toWriterPrompt()")
    class ToWriterPrompt {

        @Test
        @DisplayName("Generates complete prompt section")
        void generatesCompletePromptSection() {
            ExamplePlan plan = ExamplePlan.builder("REST API Design", ContentType.GUIDE)
                    .addExample(ExampleSpec.realistic("main", "REST", "Real scenario", "java"))
                    .minimumCount(1)
                    .guidance("Use realistic scenarios")
                    .build();

            String prompt = plan.toWriterPrompt();

            assertTrue(prompt.contains("Example Requirements"));
            assertTrue(prompt.contains("Guide"));
            assertTrue(prompt.contains("Minimum Examples: 1"));
            assertTrue(prompt.contains("Use realistic scenarios"));
            assertTrue(prompt.contains("main"));
            assertTrue(prompt.contains("Realistic"));
            assertTrue(prompt.contains("java"));
        }
    }

    @Nested
    @DisplayName("Factory methods and builder")
    class FactoryMethodsAndBuilder {

        @Test
        @DisplayName("empty() creates valid empty plan")
        void emptyCreatesValidEmptyPlan() {
            ExamplePlan plan = ExamplePlan.empty("Simple Topic", ContentType.OVERVIEW);

            assertEquals("Simple Topic", plan.topic());
            assertEquals(ContentType.OVERVIEW, plan.contentType());
            assertTrue(plan.examples().isEmpty());
            assertEquals(0, plan.minimumCount());
            assertFalse(plan.writerGuidance().isBlank());
        }

        @Test
        @DisplayName("Builder creates proper plan")
        void builderCreatesProperPlan() {
            ExamplePlan plan = ExamplePlan.builder("Builder Test", ContentType.CONCEPT)
                    .addExample(ExampleSpec.minimal("ex1", "concept", "python"))
                    .addExample(ExampleSpec.antiPattern("ex2", "concept", "bad practice"))
                    .minimumCount(1)
                    .guidance("Be thorough")
                    .build();

            assertEquals("Builder Test", plan.topic());
            assertEquals(ContentType.CONCEPT, plan.contentType());
            assertEquals(2, plan.examples().size());
            assertEquals(1, plan.minimumCount());
            assertEquals("Be thorough", plan.writerGuidance());
        }
    }
}
