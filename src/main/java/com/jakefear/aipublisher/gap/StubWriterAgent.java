package com.jakefear.aipublisher.gap;

import com.jakefear.aipublisher.util.WikiSyntaxValidator;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Lightweight agent for generating stub/definition pages.
 * Skips research, fact-checking, and critique phases for fast generation.
 */
@Component
public class StubWriterAgent {

    private static final Logger log = LoggerFactory.getLogger(StubWriterAgent.class);

    private static final String DEFINITION_PROMPT = """
            Write a brief wiki definition page for the term "%s".

            CRITICAL CONTEXT:
            - This wiki collection is about: %s
            - Target audience: %s
            - This term is referenced by these articles: %s
            %s

            DOMAIN RULES (CRITICAL):
            - ONLY describe what "%s" means in the context of %s
            - DO NOT conflate this topic with unrelated domains
            - DO NOT mention AI, machine learning, Ollama, or language models
              unless the term is specifically about AI
            - DO NOT add categories from unrelated domains

            REQUIREMENTS:
            - Use JSPWiki syntax ONLY (NOT Markdown!):
              - Headings: !!! (H1), !! (H2), ! (H3)  -- NOT # ## ###
              - Bold: __text__  -- NOT **text**
              - Italic: ''text''  -- NOT *text*
              - Links: [PageName] or [Display Text|PageName]  -- NOT [text](url)
              - Lists: * for bullets  -- NOT - for bullets
              - Horizontal rules: ----  -- NOT * * *
            - Keep it brief: 100-200 words maximum
            - Start with the term in bold followed by a clear definition
            - Include 1-2 sentences of context or explanation
            - Add a "See Also" section linking back to related articles
            - End with category metadata: [{SET categories='Category1,Category2'}]
            - Categories MUST be relevant to the domain (%s), not unrelated topics

            FORBIDDEN PATTERNS:
            - # ## ### headings
            - **bold** text
            - `code` backticks
            - - bullet lists
            - [[double bracket links]]

            Write ONLY the wiki page content. No explanations or commentary.
            """;

    private static final String REDIRECT_CONTENT = "[{ALIAS %s}]\n";

    private final ChatLanguageModel writerModel;

    public StubWriterAgent(@Qualifier("writerChatModel") ChatLanguageModel writerModel) {
        this.writerModel = writerModel;
    }

    /**
     * Generate a stub page for a gap concept.
     *
     * @param gap The gap concept to generate content for
     * @param universeName The universe/collection name for context
     * @param targetAudience The target audience
     * @return The generated wiki content
     */
    public String generateStub(GapConcept gap, String universeName, String targetAudience) {
        switch (gap.type()) {
            case DEFINITION:
                return generateDefinitionPage(gap, universeName, targetAudience);
            case REDIRECT:
                return generateRedirectPage(gap);
            default:
                log.warn("Cannot generate stub for gap type: {}", gap.type());
                return null;
        }
    }

    /**
     * Generate a definition page using the LLM.
     */
    String generateDefinitionPage(GapConcept gap, String universeName, String targetAudience) {
        String categoryHint = gap.category() != null && !gap.category().isBlank()
                ? "- Suggested category: " + gap.category()
                : "";

        String referencedByList = gap.referencedBy().isEmpty()
                ? "none specified"
                : String.join(", ", gap.referencedBy());

        String prompt = String.format(DEFINITION_PROMPT,
                gap.name(),           // %s - term name (1st)
                universeName,         // %s - universe context (2nd)
                targetAudience,       // %s - audience (3rd)
                referencedByList,     // %s - referenced by (4th)
                categoryHint,         // %s - category hint (5th)
                gap.name(),           // %s - term name again (6th) - for DOMAIN RULES
                universeName,         // %s - universe context again (7th) - for DOMAIN RULES
                universeName);        // %s - universe for categories (8th)

        try {
            String response = writerModel.generate(prompt);
            return cleanResponse(response);
        } catch (Exception e) {
            log.error("Failed to generate definition for '{}': {}", gap.name(), e.getMessage());
            return generateFallbackDefinition(gap);
        }
    }

    /**
     * Generate a redirect page (no LLM needed).
     */
    String generateRedirectPage(GapConcept gap) {
        if (gap.redirectTarget() == null || gap.redirectTarget().isBlank()) {
            log.warn("Redirect gap '{}' has no target", gap.name());
            return null;
        }

        return String.format(REDIRECT_CONTENT, gap.redirectTarget());
    }

    /**
     * Generate a fallback definition if LLM fails.
     */
    String generateFallbackDefinition(GapConcept gap) {
        StringBuilder content = new StringBuilder();
        content.append("!!! ").append(gap.name()).append("\n\n");
        content.append("__").append(gap.name()).append("__ is a term referenced in this wiki collection.\n\n");

        if (!gap.referencedBy().isEmpty()) {
            content.append("!! See Also\n\n");
            for (String ref : gap.referencedBy()) {
                content.append("* [").append(ref).append("]\n");
            }
            content.append("\n");
        }

        String category = gap.category() != null && !gap.category().isBlank()
                ? gap.category()
                : "Definitions";
        content.append("[{SET categories='").append(category).append("'}]\n");

        return content.toString();
    }

    /**
     * Clean LLM response by removing any markdown code fences, extra whitespace,
     * and converting any Markdown syntax to JSPWiki.
     */
    String cleanResponse(String response) {
        if (response == null) return null;

        String cleaned = response.trim();

        // Remove markdown code fences if present
        if (cleaned.startsWith("```")) {
            int endOfFirstLine = cleaned.indexOf('\n');
            if (endOfFirstLine > 0) {
                cleaned = cleaned.substring(endOfFirstLine + 1);
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        // Apply WikiSyntaxValidator autoFix to convert any remaining Markdown to JSPWiki
        cleaned = WikiSyntaxValidator.autoFix(cleaned);

        return cleaned;
    }

    /**
     * Batch generate stubs for multiple gaps.
     *
     * @param gaps List of gap concepts
     * @param universeName Universe name for context
     * @param targetAudience Target audience
     * @return List of generated content (same order as input, null for failures)
     */
    public List<String> generateStubs(List<GapConcept> gaps, String universeName, String targetAudience) {
        return gaps.stream()
                .map(gap -> {
                    try {
                        return generateStub(gap, universeName, targetAudience);
                    } catch (Exception e) {
                        log.error("Failed to generate stub for '{}': {}", gap.name(), e.getMessage());
                        return null;
                    }
                })
                .toList();
    }
}
