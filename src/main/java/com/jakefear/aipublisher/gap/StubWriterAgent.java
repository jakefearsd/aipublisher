package com.jakefear.aipublisher.gap;

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

            CONTEXT:
            - This is for a wiki collection about: %s
            - Target audience: %s
            - This term is referenced by these articles: %s
            %s

            REQUIREMENTS:
            - Use JSPWiki syntax (NOT Markdown):
              - Headings: !!! (H1), !! (H2), ! (H3)
              - Bold: __text__
              - Italic: ''text''
              - Links: [PageName] or [Display Text|PageName]
              - Lists: * for bullets
            - Keep it brief: 100-200 words maximum
            - Start with the term in bold followed by a clear definition
            - Include 1-2 sentences of context or explanation
            - Add a "See Also" section linking back to related articles
            - End with category metadata: [{SET categories='Category1,Category2'}]

            Write ONLY the wiki page content. No explanations or commentary.
            """;

    private static final String REDIRECT_CONTENT = """
            This page redirects to [%s].

            If you are not automatically redirected, click the link above.

            [{SET categories='Redirects'}]
            """;

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
                gap.name(),
                universeName,
                targetAudience,
                referencedByList,
                categoryHint);

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
     * Clean LLM response by removing any markdown code fences or extra whitespace.
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

        return cleaned.trim();
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
