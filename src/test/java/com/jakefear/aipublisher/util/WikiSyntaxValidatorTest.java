package com.jakefear.aipublisher.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WikiSyntaxValidator")
class WikiSyntaxValidatorTest {

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("Returns valid for proper JSPWiki content")
        void returnsValidForProperJSPWiki() {
            String content = """
                    !!! Main Heading

                    This is __bold__ and ''italic'' text.

                    !! Subheading

                    Here is a [link|https://example.com] and a [WikiPage].

                    {{{
                    code block
                    }}}

                    And {{inline code}} here.
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertTrue(result.valid());
            assertTrue(result.issues().isEmpty());
        }

        @Test
        @DisplayName("Returns valid for null content")
        void returnsValidForNullContent() {
            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(null);
            assertTrue(result.valid());
        }

        @Test
        @DisplayName("Returns valid for empty content")
        void returnsValidForEmptyContent() {
            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate("");
            assertTrue(result.valid());
        }

        @Test
        @DisplayName("Detects Markdown headings")
        void detectsMarkdownHeadings() {
            String content = """
                    # Main Heading

                    Some text.

                    ## Subheading

                    More text.

                    ### Sub-subheading
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.hasMarkdownSyntax());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_HEADING")));
        }

        @Test
        @DisplayName("Detects Markdown bold")
        void detectsMarkdownBold() {
            String content = "This is **bold text** in Markdown.";

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_BOLD")));
        }

        @Test
        @DisplayName("Detects Markdown links")
        void detectsMarkdownLinks() {
            String content = "Click [here](https://example.com) for more info.";

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_LINK")));
        }

        @Test
        @DisplayName("Detects Markdown code blocks")
        void detectsMarkdownCodeBlocks() {
            String content = """
                    Here is code:
                    ```java
                    public void test() {}
                    ```
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_CODE_BLOCK")));
        }

        @Test
        @DisplayName("Detects Markdown inline code")
        void detectsMarkdownInlineCode() {
            String content = "Use the `print()` function.";

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_INLINE_CODE")));
        }

        @Test
        @DisplayName("Detects Markdown table separator")
        void detectsMarkdownTableSeparator() {
            String content = """
                    | Header 1 | Header 2 |
                    |----------|----------|
                    | Cell 1   | Cell 2   |
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_TABLE")));
        }

        @Test
        @DisplayName("Detects multiple Markdown issues")
        void detectsMultipleMarkdownIssues() {
            String content = """
                    # Heading

                    This is **bold** and [link](url).
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertEquals(3, result.issues().size());
        }

        @Test
        @DisplayName("Detects double-bracket wiki links")
        void detectsDoubleBracketLinks() {
            String content = "See the [[Main Page]] for more information.";

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_DOUBLE_BRACKET")));
        }

        @Test
        @DisplayName("Detects Markdown dash lists")
        void detectsMarkdownDashLists() {
            String content = """
                    Items:
                    - First item
                    - Second item
                    - Third item
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_LIST_DASH")));
        }

        @Test
        @DisplayName("Detects Markdown horizontal rules (* * *)")
        void detectsMarkdownHorizontalRules() {
            String content = """
                    First section.

                    * * *

                    Second section.
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("MARKDOWN_HORIZONTAL_RULE")));
        }

        @Test
        @DisplayName("Detects chain-of-thought think tags")
        void detectsThinkTags() {
            String content = """
                    <think>
                    Let me think about this...
                    </think>

                    The answer is 42.
                    """;

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            assertFalse(result.valid());
            assertTrue(result.issues().stream()
                    .anyMatch(i -> i.type().equals("THINK_TAG")));
        }

        @Test
        @DisplayName("Summary includes all issues")
        void summaryIncludesAllIssues() {
            String content = "# Heading\n\n**bold**";

            WikiSyntaxValidator.ValidationResult result = WikiSyntaxValidator.validate(content);

            String summary = result.getSummary();
            assertTrue(summary.contains("MARKDOWN_HEADING"));
            assertTrue(summary.contains("MARKDOWN_BOLD"));
        }
    }

    @Nested
    @DisplayName("containsMarkdown()")
    class ContainsMarkdown {

        @Test
        @DisplayName("Returns false for valid JSPWiki")
        void returnsFalseForValidJSPWiki() {
            String content = "!!! Heading\n\n__bold__ text";
            assertFalse(WikiSyntaxValidator.containsMarkdown(content));
        }

        @Test
        @DisplayName("Returns false for null")
        void returnsFalseForNull() {
            assertFalse(WikiSyntaxValidator.containsMarkdown(null));
        }

        @Test
        @DisplayName("Returns false for empty string")
        void returnsFalseForEmpty() {
            assertFalse(WikiSyntaxValidator.containsMarkdown(""));
        }

        @Test
        @DisplayName("Returns true for Markdown heading")
        void returnsTrueForMarkdownHeading() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("# Heading"));
        }

        @Test
        @DisplayName("Returns true for Markdown bold")
        void returnsTrueForMarkdownBold() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("**bold**"));
        }

        @Test
        @DisplayName("Returns true for Markdown link")
        void returnsTrueForMarkdownLink() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("[text](url)"));
        }

        @Test
        @DisplayName("Returns true for double-bracket link")
        void returnsTrueForDoubleBracketLink() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("[[PageName]]"));
        }

        @Test
        @DisplayName("Returns true for dash list")
        void returnsTrueForDashList() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("- list item"));
        }

        @Test
        @DisplayName("Returns true for horizontal rule")
        void returnsTrueForHorizontalRule() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("* * *"));
        }

        @Test
        @DisplayName("Returns true for think tags")
        void returnsTrueForThinkTags() {
            assertTrue(WikiSyntaxValidator.containsMarkdown("<think>something</think>"));
        }
    }

    @Nested
    @DisplayName("autoFix()")
    class AutoFix {

        @Test
        @DisplayName("Returns null for null input")
        void returnsNullForNull() {
            assertNull(WikiSyntaxValidator.autoFix(null));
        }

        @Test
        @DisplayName("Converts Markdown headings to JSPWiki")
        void convertsMarkdownHeadings() {
            String input = "# H1\n## H2\n### H3\n#### H4";
            String expected = "!!! H1\n!! H2\n!! H3\n! H4";

            assertEquals(expected, WikiSyntaxValidator.autoFix(input));
        }

        @Test
        @DisplayName("Converts Markdown bold to JSPWiki")
        void convertsMarkdownBold() {
            String input = "This is **bold** text.";
            String expected = "This is __bold__ text.";

            assertEquals(expected, WikiSyntaxValidator.autoFix(input));
        }

        @Test
        @DisplayName("Converts Markdown links to JSPWiki")
        void convertsMarkdownLinks() {
            String input = "Click [here](https://example.com) for info.";
            String expected = "Click [here|https://example.com] for info.";

            assertEquals(expected, WikiSyntaxValidator.autoFix(input));
        }

        @Test
        @DisplayName("Converts Markdown inline code to JSPWiki")
        void convertsMarkdownInlineCode() {
            String input = "Use the `print()` function.";
            String expected = "Use the {{print()}} function.";

            assertEquals(expected, WikiSyntaxValidator.autoFix(input));
        }

        @Test
        @DisplayName("Removes Markdown table separators")
        void removesMarkdownTableSeparators() {
            String input = "| H1 | H2 |\n|---|---|\n| C1 | C2 |";
            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(result.contains("|---|"));
            assertTrue(result.contains("| H1 | H2 |"));
            assertTrue(result.contains("| C1 | C2 |"));
        }

        @Test
        @DisplayName("Handles mixed content")
        void handlesMixedContent() {
            String input = """
                    # Main Title

                    This is **bold** and here is a [link](url).

                    ## Section

                    Use `code` here.
                    """;

            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(WikiSyntaxValidator.containsMarkdown(result));
            assertTrue(result.contains("!!! Main Title"));
            assertTrue(result.contains("__bold__"));
            assertTrue(result.contains("[link|url]"));
            assertTrue(result.contains("!! Section"));
            assertTrue(result.contains("{{code}}"));
        }

        @Test
        @DisplayName("Preserves valid JSPWiki content")
        void preservesValidJSPWikiContent() {
            String input = "!!! Heading\n\n__bold__ and [link|url]";
            String result = WikiSyntaxValidator.autoFix(input);

            assertEquals(input, result);
        }

        @Test
        @DisplayName("Converts double-bracket links to single brackets")
        void convertsDoubleBracketLinks() {
            String input = "See [[Main Page]] and [[Another|Page]].";
            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(result.contains("[["));
            assertTrue(result.contains("[Main Page]"));
            assertTrue(result.contains("[Another|Page]"));
        }

        @Test
        @DisplayName("Converts dash lists to asterisk lists")
        void convertsDashListsToAsterisk() {
            String input = """
                    Items:
                    - First item
                    - Second item
                      - Nested item
                    """;
            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(result.contains("- First"));
            assertTrue(result.contains("* First item"));
            assertTrue(result.contains("* Second item"));
            assertTrue(result.contains("* Nested item"));
        }

        @Test
        @DisplayName("Converts horizontal rules to JSPWiki format")
        void convertsHorizontalRules() {
            String input = """
                    First section.

                    * * *

                    Second section.
                    """;
            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(result.contains("* * *"));
            assertTrue(result.contains("----"));
        }

        @Test
        @DisplayName("Removes think tags")
        void removesThinkTags() {
            String input = """
                    <think>
                    Let me reason about this...
                    The answer seems to be...
                    </think>

                    The answer is 42.
                    """;
            String result = WikiSyntaxValidator.autoFix(input);

            assertFalse(result.contains("<think>"));
            assertFalse(result.contains("</think>"));
            assertFalse(result.contains("Let me reason"));
            assertTrue(result.contains("The answer is 42."));
        }
    }

    @Nested
    @DisplayName("ValidationIssue")
    class ValidationIssueTests {

        @Test
        @DisplayName("Creates markdown issue with correct type prefix")
        void createsMarkdownIssueWithPrefix() {
            WikiSyntaxValidator.ValidationIssue issue =
                    WikiSyntaxValidator.ValidationIssue.markdown("HEADING", "desc", "# H1", 1);

            assertEquals("MARKDOWN_HEADING", issue.type());
            assertEquals("desc", issue.description());
            assertEquals("# H1", issue.example());
            assertEquals(1, issue.count());
        }
    }
}
