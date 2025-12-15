package com.jakefear.aipublisher.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScopeConfiguration record.
 */
@DisplayName("ScopeConfiguration")
class ScopeConfigurationTest {

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("empty() creates empty configuration")
        void emptyCreatesEmptyConfiguration() {
            ScopeConfiguration empty = ScopeConfiguration.empty();

            assertTrue(empty.assumedKnowledge().isEmpty());
            assertTrue(empty.outOfScope().isEmpty());
            assertTrue(empty.focusAreas().isEmpty());
            assertEquals("", empty.preferredLanguage());
            assertEquals("", empty.audienceDescription());
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("Builder creates complete configuration")
        void builderCreatesCompleteConfiguration() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java basics")
                    .addAssumedKnowledge("HTTP")
                    .addOutOfScope("Cloud deployment")
                    .addOutOfScope("Kubernetes")
                    .addFocusArea("Patterns")
                    .addFocusArea("Best practices")
                    .preferredLanguage("Java")
                    .audienceDescription("Backend developers")
                    .domainDescription("Event-driven systems")
                    .build();

            assertEquals(2, config.assumedKnowledge().size());
            assertEquals(2, config.outOfScope().size());
            assertEquals(2, config.focusAreas().size());
            assertEquals("Java", config.preferredLanguage());
            assertEquals("Backend developers", config.audienceDescription());
            assertEquals("Event-driven systems", config.domainDescription());
        }

        @Test
        @DisplayName("toBuilder creates modifiable copy")
        void toBuilderCreatesModifiableCopy() {
            ScopeConfiguration original = ScopeConfiguration.builder()
                    .addFocusArea("Patterns")
                    .build();

            ScopeConfiguration modified = original.toBuilder()
                    .addFocusArea("Performance")
                    .preferredLanguage("Kotlin")
                    .build();

            assertEquals(1, original.focusAreas().size());
            assertEquals(2, modified.focusAreas().size());
            assertEquals("Kotlin", modified.preferredLanguage());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        @Test
        @DisplayName("isAssumed matches exact knowledge")
        void isAssumedMatchesExactKnowledge() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java basics")
                    .build();

            assertTrue(config.isAssumed("Java basics"));
            assertTrue(config.isAssumed("java basics")); // Case insensitive
        }

        @Test
        @DisplayName("isAssumed matches partial knowledge")
        void isAssumedMatchesPartialKnowledge() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java")
                    .build();

            assertTrue(config.isAssumed("Java programming"));
            assertTrue(config.isAssumed("Basic Java"));
        }

        @Test
        @DisplayName("isOutOfScope matches exclusions")
        void isOutOfScopeMatchesExclusions() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addOutOfScope("Kubernetes")
                    .build();

            assertTrue(config.isOutOfScope("Kubernetes"));
            assertTrue(config.isOutOfScope("Kubernetes deployment"));
        }

        @Test
        @DisplayName("isFocusArea matches focus areas")
        void isFocusAreaMatchesFocusAreas() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addFocusArea("Patterns")
                    .build();

            assertTrue(config.isFocusArea("Patterns"));
            assertTrue(config.isFocusArea("Design Patterns"));
        }

        @Test
        @DisplayName("hasConstraints returns true when configured")
        void hasConstraintsReturnsTrueWhenConfigured() {
            ScopeConfiguration empty = ScopeConfiguration.empty();
            assertFalse(empty.hasConstraints());

            ScopeConfiguration withKnowledge = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java")
                    .build();
            assertTrue(withKnowledge.hasConstraints());

            ScopeConfiguration withExclusion = ScopeConfiguration.builder()
                    .addOutOfScope("Legacy")
                    .build();
            assertTrue(withExclusion.hasConstraints());

            ScopeConfiguration withFocus = ScopeConfiguration.builder()
                    .addFocusArea("Performance")
                    .build();
            assertTrue(withFocus.hasConstraints());
        }
    }

    @Nested
    @DisplayName("toPromptFormat")
    class ToPromptFormat {

        @Test
        @DisplayName("Formats assumed knowledge")
        void formatsAssumedKnowledge() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addAssumedKnowledge("Java")
                    .addAssumedKnowledge("HTTP")
                    .build();

            String prompt = config.toPromptFormat();

            assertTrue(prompt.contains("Assumed Knowledge"));
            assertTrue(prompt.contains("Java"));
            assertTrue(prompt.contains("HTTP"));
        }

        @Test
        @DisplayName("Formats out of scope")
        void formatsOutOfScope() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addOutOfScope("Cloud deployment")
                    .build();

            String prompt = config.toPromptFormat();

            assertTrue(prompt.contains("Out of Scope"));
            assertTrue(prompt.contains("Cloud deployment"));
        }

        @Test
        @DisplayName("Formats focus areas")
        void formatsFocusAreas() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .addFocusArea("Patterns")
                    .build();

            String prompt = config.toPromptFormat();

            assertTrue(prompt.contains("Focus Areas"));
            assertTrue(prompt.contains("Patterns"));
        }

        @Test
        @DisplayName("Includes audience description")
        void includesAudienceDescription() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .audienceDescription("Senior backend developers")
                    .build();

            String prompt = config.toPromptFormat();

            assertTrue(prompt.contains("Target Audience"));
            assertTrue(prompt.contains("Senior backend developers"));
        }

        @Test
        @DisplayName("Includes preferred language")
        void includesPreferredLanguage() {
            ScopeConfiguration config = ScopeConfiguration.builder()
                    .preferredLanguage("Kotlin")
                    .build();

            String prompt = config.toPromptFormat();

            assertTrue(prompt.contains("Preferred Programming Language"));
            assertTrue(prompt.contains("Kotlin"));
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("Handles null values gracefully")
        void handlesNullValuesGracefully() {
            ScopeConfiguration config = new ScopeConfiguration(
                    null, null, null, null, null, null, null
            );

            assertNotNull(config.assumedKnowledge());
            assertNotNull(config.outOfScope());
            assertNotNull(config.focusAreas());
            assertEquals("", config.preferredLanguage());
            assertEquals("", config.audienceDescription());
            assertEquals("", config.domainDescription());
            assertEquals("", config.intent());
        }
    }
}
