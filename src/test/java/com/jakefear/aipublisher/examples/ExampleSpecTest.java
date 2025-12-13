package com.jakefear.aipublisher.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExampleSpec record.
 */
@DisplayName("ExampleSpec")
class ExampleSpecTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid example spec")
        void createsValidExampleSpec() {
            ExampleSpec spec = new ExampleSpec(
                    "test-1",
                    ExampleType.MINIMAL,
                    "Demonstrate basic usage",
                    "REST API",
                    "java",
                    0,
                    true
            );

            assertEquals("test-1", spec.id());
            assertEquals(ExampleType.MINIMAL, spec.type());
            assertEquals("Demonstrate basic usage", spec.purpose());
            assertEquals("REST API", spec.concept());
            assertEquals("java", spec.language());
            assertEquals(0, spec.sequence());
            assertTrue(spec.required());
        }

        @Test
        @DisplayName("Rejects null id")
        void rejectsNullId() {
            assertThrows(NullPointerException.class, () ->
                    new ExampleSpec(null, ExampleType.MINIMAL, "purpose", "concept", null, 0, true));
        }

        @Test
        @DisplayName("Rejects blank id")
        void rejectsBlankId() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExampleSpec("  ", ExampleType.MINIMAL, "purpose", "concept", null, 0, true));
        }

        @Test
        @DisplayName("Rejects null type")
        void rejectsNullType() {
            assertThrows(NullPointerException.class, () ->
                    new ExampleSpec("id", null, "purpose", "concept", null, 0, true));
        }

        @Test
        @DisplayName("Rejects blank purpose")
        void rejectsBlankPurpose() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExampleSpec("id", ExampleType.MINIMAL, "  ", "concept", null, 0, true));
        }

        @Test
        @DisplayName("Rejects blank concept")
        void rejectsBlankConcept() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExampleSpec("id", ExampleType.MINIMAL, "purpose", "  ", null, 0, true));
        }

        @Test
        @DisplayName("Rejects negative sequence")
        void rejectsNegativeSequence() {
            assertThrows(IllegalArgumentException.class, () ->
                    new ExampleSpec("id", ExampleType.MINIMAL, "purpose", "concept", null, -1, true));
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("minimal() creates minimal type spec")
        void minimalCreatesMinimalTypeSpec() {
            ExampleSpec spec = ExampleSpec.minimal("basic", "Git commands", "bash");

            assertEquals("basic", spec.id());
            assertEquals(ExampleType.MINIMAL, spec.type());
            assertEquals("Git commands", spec.concept());
            assertEquals("bash", spec.language());
            assertTrue(spec.required());
        }

        @Test
        @DisplayName("realistic() creates realistic type spec")
        void realisticCreatesRealisticTypeSpec() {
            ExampleSpec spec = ExampleSpec.realistic("main", "API calls", "Making HTTP requests", "python");

            assertEquals("main", spec.id());
            assertEquals(ExampleType.REALISTIC, spec.type());
            assertEquals("Making HTTP requests", spec.purpose());
            assertEquals("API calls", spec.concept());
            assertEquals("python", spec.language());
        }

        @Test
        @DisplayName("progressive() creates progressive type spec")
        void progressiveCreatesProgressiveTypeSpec() {
            ExampleSpec spec = ExampleSpec.progressive("step2", "Authentication", "Add login", 2);

            assertEquals("step2", spec.id());
            assertEquals(ExampleType.PROGRESSIVE, spec.type());
            assertEquals("Authentication", spec.concept());
            assertEquals(2, spec.sequence());
            assertNull(spec.language());
        }

        @Test
        @DisplayName("antiPattern() creates anti-pattern type spec")
        void antiPatternCreatesAntiPatternTypeSpec() {
            ExampleSpec spec = ExampleSpec.antiPattern("wrong", "SQL injection", "Unescaped input");

            assertEquals("wrong", spec.id());
            assertEquals(ExampleType.ANTI_PATTERN, spec.type());
            assertEquals("SQL injection", spec.concept());
            assertFalse(spec.required());
        }

        @Test
        @DisplayName("comparison() creates comparison type spec")
        void comparisonCreatesComparisonTypeSpec() {
            ExampleSpec spec = ExampleSpec.comparison("optA", "Sorting", "Bubble sort vs Quick sort");

            assertEquals("optA", spec.id());
            assertEquals(ExampleType.COMPARISON, spec.type());
            assertEquals("Bubble sort vs Quick sort", spec.purpose());
            assertTrue(spec.required());
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("requiresCode() returns true when language specified")
        void requiresCodeReturnsTrueWhenLanguageSpecified() {
            ExampleSpec withLang = ExampleSpec.minimal("id", "concept", "java");
            assertTrue(withLang.requiresCode());
        }

        @Test
        @DisplayName("requiresCode() returns false when no language")
        void requiresCodeReturnsFalseWhenNoLanguage() {
            ExampleSpec noLang = ExampleSpec.progressive("id", "concept", "purpose", 1);
            assertFalse(noLang.requiresCode());
        }

        @Test
        @DisplayName("toPromptDescription() generates readable description")
        void toPromptDescriptionGeneratesReadableDescription() {
            ExampleSpec spec = ExampleSpec.minimal("basic", "REST API", "java");
            String desc = spec.toPromptDescription();

            assertTrue(desc.contains("basic"));
            assertTrue(desc.contains("Minimal"));
            assertTrue(desc.contains("java"));
            assertTrue(desc.contains("REQUIRED"));
        }

        @Test
        @DisplayName("toPromptDescription() omits language when null")
        void toPromptDescriptionOmitsLanguageWhenNull() {
            ExampleSpec spec = ExampleSpec.antiPattern("wrong", "concept", "avoid this");
            String desc = spec.toPromptDescription();

            assertFalse(desc.contains("[null]"));
            assertFalse(desc.contains("REQUIRED")); // anti-patterns are not required
        }
    }
}
