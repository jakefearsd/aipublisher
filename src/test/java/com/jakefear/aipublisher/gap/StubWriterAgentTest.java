package com.jakefear.aipublisher.gap;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StubWriterAgent.
 */
class StubWriterAgentTest {

    private ChatLanguageModel mockModel;
    private StubWriterAgent agent;

    @BeforeEach
    void setUp() {
        mockModel = mock(ChatLanguageModel.class);
        agent = new StubWriterAgent(mockModel);
    }

    @Test
    void generateStub_definitionType_callsLlm() {
        String expectedContent = """
                !!! Present Value

                __Present Value__ is the current worth of a future sum of money.

                !! See Also
                * [CompoundInterest]

                [{SET categories='Finance'}]
                """;

        when(mockModel.generate(anyString())).thenReturn(expectedContent);

        GapConcept gap = GapConcept.definition(
                "Present Value",
                List.of("CompoundInterest"),
                "Finance"
        );

        String result = agent.generateStub(gap, "Investing Basics", "general readers");

        assertNotNull(result);
        verify(mockModel).generate(anyString());
        assertTrue(result.contains("Present Value"));
    }

    @Test
    void generateStub_redirectType_noLlmCall() {
        GapConcept gap = GapConcept.redirect("compound interest", "CompoundInterest");

        String result = agent.generateStub(gap, "Investing Basics", "general readers");

        assertNotNull(result);
        verify(mockModel, never()).generate(anyString());
        // Uses native JSPWiki ALIAS directive for automatic redirect
        assertTrue(result.contains("[{ALIAS CompoundInterest}]"));
    }

    @Test
    void generateStub_fullArticleType_returnsNull() {
        GapConcept gap = GapConcept.of("Complex Topic", GapType.FULL_ARTICLE);

        String result = agent.generateStub(gap, "Test Universe", "general readers");

        assertNull(result);
    }

    @Test
    void generateStub_ignoreType_returnsNull() {
        GapConcept gap = GapConcept.of("money", GapType.IGNORE);

        String result = agent.generateStub(gap, "Test Universe", "general readers");

        assertNull(result);
    }

    @Test
    void generateDefinitionPage_includesContextInPrompt() {
        when(mockModel.generate(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            // Verify prompt contains the context
            assertTrue(prompt.contains("Finance Wiki"));
            assertTrue(prompt.contains("financial professionals"));
            assertTrue(prompt.contains("SourceArticle"));
            return "!!! Test\nTest content";
        });

        GapConcept gap = GapConcept.definition(
                "Test Term",
                List.of("SourceArticle"),
                "Finance"
        );

        agent.generateDefinitionPage(gap, "Finance Wiki", "financial professionals");

        verify(mockModel).generate(anyString());
    }

    @Test
    void generateDefinitionPage_llmFailure_returnsFallback() {
        when(mockModel.generate(anyString())).thenThrow(new RuntimeException("API error"));

        GapConcept gap = GapConcept.definition(
                "Test Term",
                List.of("SourceArticle"),
                "Finance"
        );

        String result = agent.generateDefinitionPage(gap, "Test Universe", "general readers");

        assertNotNull(result);
        assertTrue(result.contains("Test Term"));
        assertTrue(result.contains("[SourceArticle]"));
    }

    @Test
    void generateRedirectPage_withValidTarget() {
        GapConcept gap = GapConcept.redirect("CI", "CompoundInterest");

        String result = agent.generateRedirectPage(gap);

        assertNotNull(result);
        // Uses native JSPWiki ALIAS directive for automatic redirect
        assertTrue(result.contains("[{ALIAS CompoundInterest}]"));
    }

    @Test
    void generateRedirectPage_noTarget_returnsNull() {
        GapConcept gap = new GapConcept("orphan", null, GapType.REDIRECT, List.of(), "", null);

        String result = agent.generateRedirectPage(gap);

        assertNull(result);
    }

    @Test
    void generateFallbackDefinition_basicStructure() {
        GapConcept gap = GapConcept.definition(
                "Test Concept",
                List.of("Article1", "Article2"),
                "TestCategory"
        );

        String result = agent.generateFallbackDefinition(gap);

        assertTrue(result.contains("!!! Test Concept"));
        assertTrue(result.contains("__Test Concept__"));
        assertTrue(result.contains("[Article1]"));
        assertTrue(result.contains("[Article2]"));
        assertTrue(result.contains("TestCategory"));
    }

    @Test
    void generateFallbackDefinition_noReferences() {
        GapConcept gap = GapConcept.of("Orphan Term", GapType.DEFINITION);

        String result = agent.generateFallbackDefinition(gap);

        assertTrue(result.contains("!!! Orphan Term"));
        assertTrue(result.contains("__Orphan Term__"));
        // Should not have See Also section
        assertFalse(result.contains("!! See Also"));
    }

    @Test
    void cleanResponse_removesMarkdownCodeFences() {
        String response = """
                ```wiki
                !!! Title
                Content here
                ```""";

        String cleaned = agent.cleanResponse(response);

        assertFalse(cleaned.contains("```"));
        assertTrue(cleaned.contains("!!! Title"));
    }

    @Test
    void cleanResponse_handlesNullInput() {
        assertNull(agent.cleanResponse(null));
    }

    @Test
    void cleanResponse_trimsWhitespace() {
        String response = "  \n\n  Content  \n\n  ";

        String cleaned = agent.cleanResponse(response);

        assertEquals("Content", cleaned);
    }

    @Test
    void cleanResponse_appliesWikiSyntaxAutoFix() {
        // LLM sometimes returns Markdown syntax that needs conversion
        String response = """
                # Heading
                - List item
                **Bold text**
                """;

        String cleaned = agent.cleanResponse(response);

        // Should convert Markdown to JSPWiki syntax
        assertFalse(cleaned.contains("# Heading"));
        assertFalse(cleaned.contains("- List"));
        assertFalse(cleaned.contains("**Bold"));
        assertTrue(cleaned.contains("!!! Heading"));
        assertTrue(cleaned.contains("* List item"));
        assertTrue(cleaned.contains("__Bold text__"));
    }

    @Test
    void generateStubs_batchGeneration() {
        when(mockModel.generate(anyString())).thenReturn("Generated content");

        List<GapConcept> gaps = List.of(
                GapConcept.of("Term1", GapType.DEFINITION),
                GapConcept.redirect("term2", "Term2"),
                GapConcept.of("Term3", GapType.DEFINITION)
        );

        List<String> results = agent.generateStubs(gaps, "Test Universe", "general readers");

        assertEquals(3, results.size());
        assertNotNull(results.get(0)); // Definition
        assertNotNull(results.get(1)); // Redirect
        assertNotNull(results.get(2)); // Definition
    }

    @Test
    void generateStubs_handlesIndividualFailures() {
        when(mockModel.generate(anyString()))
                .thenReturn("Success")
                .thenThrow(new RuntimeException("API error"))
                .thenReturn("Success");

        List<GapConcept> gaps = List.of(
                GapConcept.of("Term1", GapType.DEFINITION),
                GapConcept.of("Term2", GapType.DEFINITION),
                GapConcept.of("Term3", GapType.DEFINITION)
        );

        List<String> results = agent.generateStubs(gaps, "Test Universe", "general readers");

        assertEquals(3, results.size());
        assertNotNull(results.get(0));
        // Second one gets fallback due to exception
        assertNotNull(results.get(1)); // Fallback is still generated
        assertNotNull(results.get(2));
    }
}
