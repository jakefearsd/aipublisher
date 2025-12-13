package com.jakefear.aipublisher.examples;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ExampleValidator.
 */
@DisplayName("ExampleValidator")
class ExampleValidatorTest {

    private ExampleValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ExampleValidator();
    }

    @Nested
    @DisplayName("countCodeBlocks()")
    class CountCodeBlocks {

        @Test
        @DisplayName("Counts JSPWiki code blocks")
        void countsJSPWikiCodeBlocks() {
            String content = """
                Some text.
                {{{
                public class Hello {
                    public static void main(String[] args) {
                        System.out.println("Hello");
                    }
                }
                }}}
                More text.
                {{{
                System.out.println("World");
                }}}
                """;

            assertEquals(2, validator.countCodeBlocks(content));
        }

        @Test
        @DisplayName("Counts Markdown code blocks")
        void countsMarkdownCodeBlocks() {
            String content = """
                Some text.
                ```java
                public class Hello {}
                ```
                More text.
                ```python
                print("hello")
                ```
                """;

            assertEquals(2, validator.countCodeBlocks(content));
        }

        @Test
        @DisplayName("Returns zero for content without code blocks")
        void returnsZeroForContentWithoutCodeBlocks() {
            String content = "Just plain text without any code blocks.";
            assertEquals(0, validator.countCodeBlocks(content));
        }

        @Test
        @DisplayName("Returns zero for null content")
        void returnsZeroForNullContent() {
            assertEquals(0, validator.countCodeBlocks(null));
        }

        @Test
        @DisplayName("Returns zero for blank content")
        void returnsZeroForBlankContent() {
            assertEquals(0, validator.countCodeBlocks("   "));
        }
    }

    @Nested
    @DisplayName("extractCodeBlocks()")
    class ExtractCodeBlocks {

        @Test
        @DisplayName("Extracts JSPWiki code blocks")
        void extractsJSPWikiCodeBlocks() {
            String content = "{{{System.out.println(\"test\");}}}";

            List<ExampleValidator.CodeBlock> blocks = validator.extractCodeBlocks(content);

            assertEquals(1, blocks.size());
            assertTrue(blocks.get(0).code().contains("println"));
            assertNull(blocks.get(0).language());
        }

        @Test
        @DisplayName("Extracts Markdown code blocks with language")
        void extractsMarkdownCodeBlocksWithLanguage() {
            String content = """
                ```java
                public void test() {}
                ```
                """;

            List<ExampleValidator.CodeBlock> blocks = validator.extractCodeBlocks(content);

            assertEquals(1, blocks.size());
            assertEquals("java", blocks.get(0).language());
            assertTrue(blocks.get(0).code().contains("public void"));
        }

        @Test
        @DisplayName("Returns sorted by position")
        void returnsSortedByPosition() {
            String content = """
                First: {{{code1}}}
                Second: {{{code2}}}
                """;

            List<ExampleValidator.CodeBlock> blocks = validator.extractCodeBlocks(content);

            assertEquals(2, blocks.size());
            assertTrue(blocks.get(0).position() < blocks.get(1).position());
        }
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("Returns valid for content meeting plan")
        void returnsValidForContentMeetingPlan() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.REFERENCE)
                    .addExample(ExampleSpec.minimal("ex1", "test", "java"))
                    .minimumCount(1)
                    .build();

            String content = """
                Here is some test content.
                {{{
                public class Test {}
                }}}
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            assertTrue(result.valid());
            assertEquals(1, result.exampleCount());
        }

        @Test
        @DisplayName("Reports insufficient examples")
        void reportsInsufficientExamples() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.TUTORIAL)
                    .minimumCount(3)
                    .build();

            String content = """
                Only one example:
                {{{
                code here
                }}}
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.contains("Insufficient examples")));
        }

        @Test
        @DisplayName("Warns about missing anti-pattern indicators")
        void warnsAboutMissingAntiPatternIndicators() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.GUIDE)
                    .addExample(ExampleSpec.antiPattern("bad", "test", "wrong way"))
                    .minimumCount(0)
                    .build();

            String content = """
                Here's how to do it properly.
                {{{good code}}}
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            assertTrue(result.hasWarnings());
            assertTrue(result.warnings().stream()
                    .anyMatch(w -> w.contains("Anti-pattern")));
        }

        @Test
        @DisplayName("Accepts content with anti-pattern indicators")
        void acceptsContentWithAntiPatternIndicators() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.GUIDE)
                    .addExample(ExampleSpec.antiPattern("bad", "test", "wrong way"))
                    .minimumCount(1)
                    .build();

            String content = """
                Don't do this - it's wrong:
                {{{bad code}}}
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            // Should not have anti-pattern warning
            assertFalse(result.warnings().stream()
                    .anyMatch(w -> w.contains("Anti-pattern")));
        }

        @Test
        @DisplayName("Detects unbalanced braces in Java code")
        void detectsUnbalancedBracesInJavaCode() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.REFERENCE)
                    .minimumCount(0)
                    .build();

            String content = """
                ```java
                public class Test {
                    public void method() {
                    // missing closing braces
                ```
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.contains("unbalanced")));
        }

        @Test
        @DisplayName("Accepts well-formed code")
        void acceptsWellFormedCode() {
            ExamplePlan plan = ExamplePlan.builder("Test", ContentType.REFERENCE)
                    .minimumCount(1)
                    .build();

            String content = """
                ```java
                public class Test {
                    public void method() {
                        System.out.println("Hello");
                    }
                }
                ```
                """;

            ExampleValidator.ValidationResult result = validator.validate(content, plan);

            // Should not have syntax issues for well-formed code
            assertFalse(result.issues().stream()
                    .anyMatch(i -> i.contains("unbalanced braces")));
        }
    }

    @Nested
    @DisplayName("ValidationResult")
    class ValidationResultTests {

        @Test
        @DisplayName("hasWarnings() returns correct value")
        void hasWarningsReturnsCorrectValue() {
            ExampleValidator.ValidationResult withWarnings = new ExampleValidator.ValidationResult(
                    true, List.of(), List.of("warning"), 1
            );
            assertTrue(withWarnings.hasWarnings());

            ExampleValidator.ValidationResult noWarnings = new ExampleValidator.ValidationResult(
                    true, List.of(), List.of(), 1
            );
            assertFalse(noWarnings.hasWarnings());
        }
    }
}
