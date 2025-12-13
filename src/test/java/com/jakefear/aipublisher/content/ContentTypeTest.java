package com.jakefear.aipublisher.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentType")
class ContentTypeTest {

    @Nested
    @DisplayName("Value Properties")
    class ValueProperties {

        @Test
        @DisplayName("Each content type has a display name")
        void eachTypeHasDisplayName() {
            for (ContentType type : ContentType.values()) {
                assertNotNull(type.getDisplayName());
                assertFalse(type.getDisplayName().isBlank());
            }
        }

        @Test
        @DisplayName("Each content type has a description")
        void eachTypeHasDescription() {
            for (ContentType type : ContentType.values()) {
                assertNotNull(type.getDescription());
                assertFalse(type.getDescription().isBlank());
            }
        }

        @Test
        @DisplayName("Each content type has valid word count range")
        void eachTypeHasValidWordCountRange() {
            for (ContentType type : ContentType.values()) {
                assertTrue(type.getMinWordCount() > 0);
                assertTrue(type.getMaxWordCount() >= type.getMinWordCount());
                assertTrue(type.getDefaultWordCount() >= type.getMinWordCount());
                assertTrue(type.getDefaultWordCount() <= type.getMaxWordCount());
            }
        }
    }

    @Nested
    @DisplayName("fromString Parsing")
    class FromStringParsing {

        @Test
        @DisplayName("Parses lowercase type names")
        void parsesLowercaseTypeNames() {
            assertEquals(ContentType.CONCEPT, ContentType.fromString("concept"));
            assertEquals(ContentType.TUTORIAL, ContentType.fromString("tutorial"));
            assertEquals(ContentType.COMPARISON, ContentType.fromString("comparison"));
        }

        @Test
        @DisplayName("Parses uppercase type names")
        void parsesUppercaseTypeNames() {
            assertEquals(ContentType.CONCEPT, ContentType.fromString("CONCEPT"));
            assertEquals(ContentType.TUTORIAL, ContentType.fromString("TUTORIAL"));
        }

        @Test
        @DisplayName("Parses mixed case type names")
        void parsesMixedCaseTypeNames() {
            assertEquals(ContentType.CONCEPT, ContentType.fromString("Concept"));
            assertEquals(ContentType.TROUBLESHOOTING, ContentType.fromString("TroubleShooTing"));
        }

        @Test
        @DisplayName("Returns null for unknown types")
        void returnsNullForUnknownTypes() {
            assertNull(ContentType.fromString("unknown"));
            assertNull(ContentType.fromString(""));
            assertNull(ContentType.fromString(null));
        }

        @Test
        @DisplayName("Handles whitespace in type names")
        void handlesWhitespaceInTypeNames() {
            assertEquals(ContentType.CONCEPT, ContentType.fromString("  concept  "));
            assertEquals(ContentType.TUTORIAL, ContentType.fromString("\ttutorial\n"));
        }
    }

    @Nested
    @DisplayName("Examples Required")
    class ExamplesRequired {

        @Test
        @DisplayName("Tutorials require examples")
        void tutorialsRequireExamples() {
            assertTrue(ContentType.TUTORIAL.requiresExamples());
        }

        @Test
        @DisplayName("Concepts require examples")
        void conceptsRequireExamples() {
            assertTrue(ContentType.CONCEPT.requiresExamples());
        }

        @Test
        @DisplayName("References do not require examples")
        void referencesDoNotRequireExamples() {
            assertFalse(ContentType.REFERENCE.requiresExamples());
        }

        @Test
        @DisplayName("Troubleshooting does not require examples")
        void troubleshootingDoesNotRequireExamples() {
            assertFalse(ContentType.TROUBLESHOOTING.requiresExamples());
        }
    }
}
