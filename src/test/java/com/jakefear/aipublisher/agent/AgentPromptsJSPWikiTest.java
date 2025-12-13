package com.jakefear.aipublisher.agent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that agent prompts correctly specify JSPWiki syntax
 * and not Markdown syntax.
 */
@DisplayName("AgentPrompts JSPWiki Syntax")
class AgentPromptsJSPWikiTest {

    @Nested
    @DisplayName("Writer Prompt")
    class WriterPrompt {

        @Test
        @DisplayName("Uses JSPWiki heading syntax (!) not Markdown (#)")
        void usesJSPWikiHeadingSyntax() {
            String prompt = AgentPrompts.WRITER;

            // Should mention JSPWiki heading syntax
            assertTrue(prompt.contains("!!!"), "Should mention !!! for H1 headings");
            assertTrue(prompt.contains("!!"), "Should mention !! for H2 headings");
            assertTrue(prompt.contains("! "), "Should mention ! for H3 headings");

            // Should explicitly say NOT to use # symbols
            assertTrue(prompt.contains("DO NOT use # symbols") || prompt.contains("NOT Markdown"),
                    "Should warn against using # symbols for headings");
        }

        @Test
        @DisplayName("Uses JSPWiki bold syntax (__) not Markdown (**)")
        void usesJSPWikiBoldSyntax() {
            String prompt = AgentPrompts.WRITER;

            // Should mention JSPWiki bold syntax
            assertTrue(prompt.contains("__bold") || prompt.contains("__text__"),
                    "Should mention __ for bold text");

            // Should mention NOT asterisks
            assertTrue(prompt.contains("NOT asterisks") || prompt.contains("double underscores"),
                    "Should clarify bold uses underscores not asterisks");
        }

        @Test
        @DisplayName("Uses JSPWiki italic syntax ('') not Markdown (*)")
        void usesJSPWikiItalicSyntax() {
            String prompt = AgentPrompts.WRITER;

            // Should mention JSPWiki italic syntax
            assertTrue(prompt.contains("''italic") || prompt.contains("''text''"),
                    "Should mention '' for italic text");
        }

        @Test
        @DisplayName("Uses JSPWiki link syntax ([PageName]) not Markdown ([text](url))")
        void usesJSPWikiLinkSyntax() {
            String prompt = AgentPrompts.WRITER;

            // Should mention JSPWiki link syntax
            assertTrue(prompt.contains("[PageName]") || prompt.contains("[Display Text|"),
                    "Should mention JSPWiki link syntax");

            // Should NOT recommend Markdown link syntax
            assertFalse(prompt.contains("[text](url)") && !prompt.contains("DO NOT") && !prompt.contains("NEVER"),
                    "Should not recommend Markdown link syntax without warning");
        }

        @Test
        @DisplayName("Uses JSPWiki code block syntax ({{{)))")
        void usesJSPWikiCodeBlockSyntax() {
            String prompt = AgentPrompts.WRITER;

            // Should mention JSPWiki code block syntax
            assertTrue(prompt.contains("{{{") && prompt.contains("}}}"),
                    "Should mention {{{ and }}} for code blocks");
        }

        @Test
        @DisplayName("Mentions TableOfContents plugin syntax")
        void mentionsTableOfContents() {
            String prompt = AgentPrompts.WRITER;

            assertTrue(prompt.contains("[{TableOfContents}]"),
                    "Should mention [{TableOfContents}] plugin syntax");
        }

        @Test
        @DisplayName("Mentions JSPWiki in title")
        void mentionsJSPWikiInTitle() {
            String prompt = AgentPrompts.WRITER;

            assertTrue(prompt.toLowerCase().contains("jspwiki"),
                    "Should mention JSPWiki in the prompt");
        }
    }

    @Nested
    @DisplayName("Editor Prompt")
    class EditorPrompt {

        @Test
        @DisplayName("Uses JSPWiki heading syntax (!) not Markdown (#)")
        void usesJSPWikiHeadingSyntax() {
            String prompt = AgentPrompts.EDITOR;

            // Should mention JSPWiki heading syntax
            assertTrue(prompt.contains("!!!"), "Should mention !!! for H1 headings");
            assertTrue(prompt.contains("!!"), "Should mention !! for H2 headings");

            // Should explicitly say NOT to use # symbols
            assertTrue(prompt.contains("must use ! not #") || prompt.contains("NOT Markdown"),
                    "Should clarify to use ! not # for headings");
        }

        @Test
        @DisplayName("Uses JSPWiki bold syntax (__) not Markdown (**)")
        void usesJSPWikiBoldSyntax() {
            String prompt = AgentPrompts.EDITOR;

            // Should mention JSPWiki bold syntax
            assertTrue(prompt.contains("__bold") || prompt.contains("__text__"),
                    "Should mention __ for bold text");
        }

        @Test
        @DisplayName("Uses JSPWiki link syntax")
        void usesJSPWikiLinkSyntax() {
            String prompt = AgentPrompts.EDITOR;

            // Should mention JSPWiki link syntax
            assertTrue(prompt.contains("[PageName]") || prompt.contains("[Display Text|"),
                    "Should mention JSPWiki link syntax");

            // Should explicitly warn against Markdown syntax
            assertTrue(prompt.contains("NEVER use Markdown") || prompt.contains("NOT Markdown"),
                    "Should warn against Markdown link syntax");
        }

        @Test
        @DisplayName("Instructs to convert Markdown to JSPWiki")
        void instructsToConvertMarkdown() {
            String prompt = AgentPrompts.EDITOR;

            assertTrue(prompt.contains("Convert") && prompt.contains("Markdown") ||
                            prompt.contains("NOT Markdown") ||
                            prompt.contains("JSPWiki syntax"),
                    "Should instruct to verify/convert Markdown to JSPWiki");
        }

        @Test
        @DisplayName("Mentions JSPWiki in description")
        void mentionsJSPWiki() {
            String prompt = AgentPrompts.EDITOR;

            assertTrue(prompt.toLowerCase().contains("jspwiki"),
                    "Should mention JSPWiki in the prompt");
        }
    }

    @Nested
    @DisplayName("Critic Prompt")
    class CriticPrompt {

        @Test
        @DisplayName("Specifies Markdown vs JSPWiki comparison")
        void specifiesMarkdownVsJSPWiki() {
            String prompt = AgentPrompts.CRITIC;

            // Should have a clear comparison section
            assertTrue(prompt.contains("Markdown") && prompt.contains("JSPWiki"),
                    "Should compare Markdown vs JSPWiki syntax");
        }

        @Test
        @DisplayName("Lists Markdown heading as wrong syntax")
        void listsMarkdownHeadingAsWrong() {
            String prompt = AgentPrompts.CRITIC;

            // Should indicate # is wrong
            assertTrue(prompt.contains("# Heading") || prompt.contains("# "),
                    "Should list # as wrong (Markdown) syntax");

            // Should indicate ! is correct
            assertTrue(prompt.contains("!!! Heading") || prompt.contains("!!!"),
                    "Should list !!! as correct (JSPWiki) syntax");
        }

        @Test
        @DisplayName("Lists Markdown bold as wrong syntax")
        void listsMarkdownBoldAsWrong() {
            String prompt = AgentPrompts.CRITIC;

            // Should indicate **bold** is wrong
            assertTrue(prompt.contains("**bold**") || prompt.contains("**"),
                    "Should list **bold** as wrong (Markdown) syntax");

            // Should indicate __bold__ is correct
            assertTrue(prompt.contains("__bold__") || prompt.contains("__"),
                    "Should list __bold__ as correct (JSPWiki) syntax");
        }

        @Test
        @DisplayName("Lists Markdown link as wrong syntax")
        void listsMarkdownLinkAsWrong() {
            String prompt = AgentPrompts.CRITIC;

            // Should indicate [text](url) is wrong
            assertTrue(prompt.contains("[text](url)") || prompt.contains("]("),
                    "Should list [text](url) as wrong (Markdown) syntax");

            // Should indicate [text|url] is correct
            assertTrue(prompt.contains("[text|url]") || prompt.contains("|url]"),
                    "Should list [text|url] as correct (JSPWiki) syntax");
        }

        @Test
        @DisplayName("Instructs to flag Markdown syntax as issues")
        void instructsToFlagMarkdownSyntax() {
            String prompt = AgentPrompts.CRITIC;

            assertTrue(prompt.contains("Flag") || prompt.contains("critical") || prompt.contains("syntax issues"),
                    "Should instruct to flag Markdown syntax as issues");
        }

        @Test
        @DisplayName("Has syntaxScore in output format")
        void hasSyntaxScoreInOutput() {
            String prompt = AgentPrompts.CRITIC;

            assertTrue(prompt.contains("syntaxScore"),
                    "Output format should include syntaxScore");
        }

        @Test
        @DisplayName("Has syntaxIssues in output format")
        void hasSyntaxIssuesInOutput() {
            String prompt = AgentPrompts.CRITIC;

            assertTrue(prompt.contains("syntaxIssues"),
                    "Output format should include syntaxIssues list");
        }
    }

    @Nested
    @DisplayName("Consistency Across Prompts")
    class ConsistencyAcrossPrompts {

        @Test
        @DisplayName("All content-producing prompts mention JSPWiki")
        void allContentPromptsmentionJSPWiki() {
            assertTrue(AgentPrompts.WRITER.toLowerCase().contains("jspwiki"),
                    "Writer prompt should mention JSPWiki");
            assertTrue(AgentPrompts.EDITOR.toLowerCase().contains("jspwiki"),
                    "Editor prompt should mention JSPWiki");
            assertTrue(AgentPrompts.CRITIC.toLowerCase().contains("jspwiki"),
                    "Critic prompt should mention JSPWiki");
        }

        @Test
        @DisplayName("All content-producing prompts use consistent heading syntax")
        void allPromptsUseConsistentHeadingSyntax() {
            // All should use !!! for title
            assertTrue(AgentPrompts.WRITER.contains("!!!"),
                    "Writer should use !!! for headings");
            assertTrue(AgentPrompts.EDITOR.contains("!!!"),
                    "Editor should use !!! for headings");
            assertTrue(AgentPrompts.CRITIC.contains("!!!"),
                    "Critic should mention !!! for headings");
        }
    }
}
