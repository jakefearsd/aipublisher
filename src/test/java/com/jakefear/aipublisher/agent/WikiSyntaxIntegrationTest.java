package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.EnabledIfLlmAvailable;
import com.jakefear.aipublisher.IntegrationTestHelper;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.util.WikiSyntaxValidator;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests to verify LLM-generated content uses JSPWiki syntax, not Markdown.
 *
 * These tests are designed to provoke Markdown output from the LLM by using
 * prompts that typically trigger Markdown formatting. If the LLM produces
 * Markdown instead of JSPWiki syntax, the test fails.
 *
 * Run with: mvn test -Dtest=WikiSyntaxIntegrationTest
 * Or run all integration tests: mvn test -Dgroups=integration
 *
 * Environment:
 * - OLLAMA_BASE_URL: Use local Ollama (e.g., http://localhost:11434)
 * - OLLAMA_MODEL: Model to test (e.g., qwen2.5:14b)
 */
@Tag("integration")
@EnabledIfLlmAvailable
@DisplayName("Wiki Syntax Integration Tests")
class WikiSyntaxIntegrationTest {

    private ChatLanguageModel model;

    @BeforeEach
    void setUp() {
        model = IntegrationTestHelper.buildModel(0.7);
        System.out.println("Using LLM: " + IntegrationTestHelper.getProviderName());
    }

    @Nested
    @DisplayName("WriterAgent Markdown Detection")
    class WriterAgentMarkdownDetection {

        private WriterAgent agent;

        @BeforeEach
        void setUp() {
            agent = new WriterAgent(model, AgentPrompts.WRITER);
        }

        @Test
        @DisplayName("Article about code formatting should not use Markdown")
        void articleAboutCodeFormattingShouldNotUseMarkdown() {
            // This prompt is designed to potentially trigger Markdown code formatting
            TopicBrief brief = TopicBrief.simple(
                    "Python code formatting and syntax best practices",
                    "software developers",
                    800
            );
            PublishingDocument document = createDocumentWithResearch(brief,
                    List.of(
                            KeyFact.unsourced("Use consistent indentation with 4 spaces"),
                            KeyFact.unsourced("Follow PEP 8 style guidelines"),
                            KeyFact.unsourced("Use meaningful variable names"),
                            KeyFact.unsourced("Document functions with docstrings")
                    ),
                    List.of("Introduction", "Formatting Rules", "Code Examples", "Best Practices")
            );

            // Act
            PublishingDocument result = agent.process(document);

            // Assert
            ArticleDraft draft = result.getDraft();
            assertNotNull(draft, "Draft should not be null");

            WikiSyntaxValidator.ValidationResult validation =
                    WikiSyntaxValidator.validate(draft.wikiContent());

            if (!validation.valid()) {
                System.err.println("=== MARKDOWN DETECTED ===");
                System.err.println(validation.getSummary());
                System.err.println("\n=== Content Preview ===");
                System.err.println(draft.wikiContent().substring(0, Math.min(1000, draft.wikiContent().length())));
                fail("Writer produced Markdown syntax instead of JSPWiki: " + validation.getSummary());
            }

            System.out.println("SUCCESS: No Markdown detected in code formatting article");
        }

        @Test
        @DisplayName("Technical documentation should not use Markdown")
        void technicalDocumentationShouldNotUseMarkdown() {
            // This prompt mentions "documentation" which LLMs often associate with Markdown
            TopicBrief brief = TopicBrief.simple(
                    "API documentation best practices",
                    "technical writers",
                    600
            );
            PublishingDocument document = createDocumentWithResearch(brief,
                    List.of(
                            KeyFact.unsourced("Include clear endpoint descriptions"),
                            KeyFact.unsourced("Provide request and response examples"),
                            KeyFact.unsourced("Document error codes and handling"),
                            KeyFact.unsourced("Keep examples up to date")
                    ),
                    List.of("Overview", "Endpoint Documentation", "Examples", "Error Handling")
            );

            // Act
            PublishingDocument result = agent.process(document);

            // Assert
            ArticleDraft draft = result.getDraft();
            WikiSyntaxValidator.ValidationResult validation =
                    WikiSyntaxValidator.validate(draft.wikiContent());

            if (!validation.valid()) {
                System.err.println("=== MARKDOWN DETECTED ===");
                System.err.println(validation.getSummary());
                fail("Writer produced Markdown syntax: " + validation.getSummary());
            }

            System.out.println("SUCCESS: No Markdown detected in API documentation article");
        }

        @Test
        @DisplayName("Article with lists and formatting should use JSPWiki syntax")
        void articleWithListsAndFormattingShouldUseJSPWiki() {
            // This prompt is designed to trigger list and bold formatting
            TopicBrief brief = TopicBrief.simple(
                    "Key investment strategies for beginners",
                    "financial beginners",
                    500
            );
            PublishingDocument document = createDocumentWithResearch(brief,
                    List.of(
                            KeyFact.unsourced("Diversification reduces risk"),
                            KeyFact.unsourced("Start with index funds"),
                            KeyFact.unsourced("Invest regularly regardless of market conditions"),
                            KeyFact.unsourced("Keep fees low"),
                            KeyFact.unsourced("Have an emergency fund first")
                    ),
                    List.of("Introduction", "Key Strategies", "Getting Started", "Common Mistakes")
            );

            // Act
            PublishingDocument result = agent.process(document);

            // Assert
            ArticleDraft draft = result.getDraft();
            WikiSyntaxValidator.ValidationResult validation =
                    WikiSyntaxValidator.validate(draft.wikiContent());

            if (!validation.valid()) {
                System.err.println("=== MARKDOWN DETECTED ===");
                System.err.println(validation.getSummary());
                fail("Writer produced Markdown syntax: " + validation.getSummary());
            }

            // Verify it actually has JSPWiki formatting
            String content = draft.wikiContent();
            assertTrue(content.contains("!!!"), "Should have JSPWiki title heading (!!!)");
            assertTrue(content.contains("!!") || content.contains("!"),
                    "Should have JSPWiki section headings");

            System.out.println("SUCCESS: Article uses proper JSPWiki syntax");
        }
    }

    @Nested
    @DisplayName("EditorAgent Markdown Correction")
    class EditorAgentMarkdownCorrection {

        private EditorAgent agent;

        @BeforeEach
        void setUp() {
            agent = new EditorAgent(model, AgentPrompts.EDITOR);
        }

        @Test
        @DisplayName("Editor should fix any Markdown that slipped through")
        void editorShouldFixMarkdownThatSlippedThrough() {
            // Create a document with deliberate Markdown (simulating writer failure)
            TopicBrief brief = TopicBrief.simple("Test Topic", "testers", 300);
            PublishingDocument document = new PublishingDocument(brief);
            document.transitionTo(DocumentState.RESEARCHING);
            document.setResearchBrief(createSimpleResearchBrief());
            document.transitionTo(DocumentState.DRAFTING);

            // Set a draft with Markdown (simulating a broken writer output)
            ArticleDraft badDraft = new ArticleDraft(
                    """
                    # Test Topic

                    This is **bold** text and *italic* text.

                    ## Section One

                    Here is some `inline code` and a [link](https://example.com).

                    ### Subsection

                    - List item 1
                    - List item 2
                    """,
                    "A test article",
                    List.of(),
                    List.of("Testing"),
                    Map.of()
            );
            document.setDraft(badDraft);
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.setFactCheckReport(createSimpleFactCheckReport(badDraft));
            document.transitionTo(DocumentState.EDITING);

            // Act
            PublishingDocument result = agent.process(document);

            // Assert
            FinalArticle article = result.getFinalArticle();
            assertNotNull(article, "Final article should not be null");

            WikiSyntaxValidator.ValidationResult validation =
                    WikiSyntaxValidator.validate(article.wikiContent());

            if (!validation.valid()) {
                System.err.println("=== MARKDOWN STILL PRESENT AFTER EDITING ===");
                System.err.println(validation.getSummary());
                System.err.println("\n=== Content ===");
                System.err.println(article.wikiContent());
                fail("Editor failed to convert Markdown to JSPWiki: " + validation.getSummary());
            }

            System.out.println("SUCCESS: Editor successfully converted Markdown to JSPWiki");
            System.out.println("=== Corrected Content ===");
            System.out.println(article.wikiContent());
        }
    }

    @Nested
    @DisplayName("Full Pipeline Markdown Prevention")
    class FullPipelineMarkdownPrevention {

        @Test
        @DisplayName("Complete article generation should produce valid JSPWiki")
        void completeArticleGenerationShouldProduceValidJSPWiki() {
            // Use writer and editor in sequence
            WriterAgent writer = new WriterAgent(model, AgentPrompts.WRITER);
            EditorAgent editor = new EditorAgent(model, AgentPrompts.EDITOR);

            TopicBrief brief = TopicBrief.simple(
                    "Understanding compound interest",
                    "financial beginners",
                    600
            );
            PublishingDocument document = createDocumentWithResearch(brief,
                    List.of(
                            KeyFact.unsourced("Compound interest is interest on interest"),
                            KeyFact.unsourced("The formula is A = P(1 + r/n)^(nt)"),
                            KeyFact.unsourced("More frequent compounding yields higher returns"),
                            KeyFact.unsourced("Time is the most important factor")
                    ),
                    List.of("Introduction", "The Formula", "Examples", "Impact of Time")
            );

            // Writer phase
            document = writer.process(document);
            ArticleDraft draft = document.getDraft();
            assertNotNull(draft, "Draft should not be null");

            WikiSyntaxValidator.ValidationResult draftValidation =
                    WikiSyntaxValidator.validate(draft.wikiContent());
            if (!draftValidation.valid()) {
                System.out.println("NOTE: Writer produced some Markdown (will be fixed by editor): "
                        + draftValidation.getSummary());
            }

            // Editor phase
            document.transitionTo(DocumentState.FACT_CHECKING);
            document.setFactCheckReport(createSimpleFactCheckReport(draft));
            document.transitionTo(DocumentState.EDITING);

            document = editor.process(document);
            FinalArticle article = document.getFinalArticle();
            assertNotNull(article, "Final article should not be null");

            // Final validation - this MUST pass
            WikiSyntaxValidator.ValidationResult finalValidation =
                    WikiSyntaxValidator.validate(article.wikiContent());

            if (!finalValidation.valid()) {
                System.err.println("=== MARKDOWN IN FINAL OUTPUT ===");
                System.err.println(finalValidation.getSummary());
                System.err.println("\n=== Final Content ===");
                System.err.println(article.wikiContent());
                fail("Final article contains Markdown syntax: " + finalValidation.getSummary());
            }

            // Verify proper JSPWiki structure
            String content = article.wikiContent();
            assertTrue(content.contains("!!!"), "Should have main title with !!!");
            assertFalse(content.contains("# "), "Should NOT have Markdown headings");
            assertFalse(content.contains("**"), "Should NOT have Markdown bold");

            System.out.println("SUCCESS: Full pipeline produced valid JSPWiki");
            System.out.println("Word count: " + article.estimateWordCount());
        }
    }

    // Helper methods

    private PublishingDocument createDocumentWithResearch(TopicBrief brief,
                                                           List<KeyFact> facts,
                                                           List<String> outline) {
        PublishingDocument document = new PublishingDocument(brief);
        document.transitionTo(DocumentState.RESEARCHING);

        ResearchBrief researchBrief = new ResearchBrief(
                facts,
                List.of(new SourceCitation("General knowledge", ConfidenceLevel.MEDIUM)),
                outline,
                List.of(),
                Map.of(),
                List.of()
        );
        document.setResearchBrief(researchBrief);
        document.transitionTo(DocumentState.DRAFTING);

        return document;
    }

    private ResearchBrief createSimpleResearchBrief() {
        return new ResearchBrief(
                List.of(KeyFact.unsourced("Test fact 1"), KeyFact.unsourced("Test fact 2")),
                List.of(),
                List.of("Introduction", "Main Content"),
                List.of(),
                Map.of(),
                List.of()
        );
    }

    private FactCheckReport createSimpleFactCheckReport(ArticleDraft draft) {
        return new FactCheckReport(
                draft.wikiContent(),
                List.of(VerifiedClaim.verified("Test claim", 0)),
                List.of(),
                List.of(),
                ConfidenceLevel.HIGH,
                RecommendedAction.APPROVE
        );
    }
}
