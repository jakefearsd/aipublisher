package com.jakefear.aipublisher.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LanguageValidator")
class LanguageValidatorTest {

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("Returns valid for pure English content")
        void returnsValidForEnglishContent() {
            String content = "This is a normal English text about investing and finance.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertTrue(result.valid());
            assertTrue(result.issues().isEmpty());
        }

        @Test
        @DisplayName("Returns valid for null content")
        void returnsValidForNullContent() {
            LanguageValidator.ValidationResult result = LanguageValidator.validate(null, "en");
            assertTrue(result.valid());
        }

        @Test
        @DisplayName("Returns valid for empty content")
        void returnsValidForEmptyContent() {
            LanguageValidator.ValidationResult result = LanguageValidator.validate("", "en");
            assertTrue(result.valid());
        }

        @Test
        @DisplayName("Detects Chinese characters")
        void detectsChineseCharacters() {
            String content = "This article discusses 收费标准 pricing models.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Chinese")));
        }

        @Test
        @DisplayName("Detects Cyrillic/Russian characters")
        void detectsCyrillicCharacters() {
            String content = "The concept is called Продолжа in some contexts.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Cyrillic/Russian")));
        }

        @Test
        @DisplayName("Detects Arabic characters")
        void detectsArabicCharacters() {
            String content = "See العربية for more information.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Arabic")));
        }

        @Test
        @DisplayName("Detects Korean characters")
        void detectsKoreanCharacters() {
            String content = "The term 한국어 means Korean.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Korean")));
        }

        @Test
        @DisplayName("Detects Hebrew characters")
        void detectsHebrewCharacters() {
            String content = "Also known as הבע in Hebrew.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Hebrew")));
        }

        @Test
        @DisplayName("Detects Japanese characters")
        void detectsJapaneseCharacters() {
            String content = "This is called ひらがな in Japanese.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.language().equals("Japanese")));
        }

        @Test
        @DisplayName("Detects multiple foreign character types")
        void detectsMultipleForeignTypes() {
            String content = "Mixed 中文 and Русский characters.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            assertFalse(result.valid());
            assertEquals(2, result.issues().size());
        }

        @Test
        @DisplayName("Summary describes found issues")
        void summaryDescribesIssues() {
            String content = "Contains 中文 Chinese text.";

            LanguageValidator.ValidationResult result = LanguageValidator.validate(content, "en");

            String summary = result.getSummary();
            assertTrue(summary.contains("Chinese"));
            assertTrue(summary.contains("中文"));
        }
    }

    @Nested
    @DisplayName("removeForeignText()")
    class RemoveForeignText {

        @Test
        @DisplayName("Returns null for null input")
        void returnsNullForNull() {
            assertNull(LanguageValidator.removeForeignText(null));
        }

        @Test
        @DisplayName("Removes Chinese characters")
        void removesChineseCharacters() {
            String input = "This 收费标准 is a test.";
            String result = LanguageValidator.removeForeignText(input);

            assertFalse(result.contains("收费标准"));
            assertTrue(result.contains("This"));
            assertTrue(result.contains("is a test"));
        }

        @Test
        @DisplayName("Removes Cyrillic characters")
        void removesCyrillicCharacters() {
            String input = "See Продолжа for details.";
            String result = LanguageValidator.removeForeignText(input);

            assertFalse(result.contains("Продолжа"));
        }

        @Test
        @DisplayName("Cleans up resulting double spaces")
        void cleansUpDoubleSpaces() {
            String input = "This 中文 is a test.";
            String result = LanguageValidator.removeForeignText(input);

            assertFalse(result.contains("  "));
        }

        @Test
        @DisplayName("Preserves English content")
        void preservesEnglishContent() {
            String input = "This is pure English text.";
            String result = LanguageValidator.removeForeignText(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("Removes multiple foreign character types")
        void removesMultipleForeignTypes() {
            String input = "Mixed 中文 and Русский characters here.";
            String result = LanguageValidator.removeForeignText(input);

            assertFalse(result.contains("中文"));
            assertFalse(result.contains("Русский"));
            assertTrue(result.contains("Mixed"));
            assertTrue(result.contains("characters here"));
        }
    }

    @Nested
    @DisplayName("ForeignTextIssue")
    class ForeignTextIssueTests {

        @Test
        @DisplayName("Contains language, text, and position")
        void containsAllFields() {
            LanguageValidator.ForeignTextIssue issue =
                    new LanguageValidator.ForeignTextIssue("Chinese", "中文", 10);

            assertEquals("Chinese", issue.language());
            assertEquals("中文", issue.text());
            assertEquals(10, issue.position());
        }
    }
}
