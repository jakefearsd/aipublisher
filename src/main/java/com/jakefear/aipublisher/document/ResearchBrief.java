package com.jakefear.aipublisher.document;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Output from the Research Agent containing gathered information and suggested structure.
 */
public record ResearchBrief(
        /**
         * Key facts and information gathered during research.
         */
        List<KeyFact> keyFacts,

        /**
         * Sources consulted with reliability assessments.
         */
        List<SourceCitation> sources,

        /**
         * Suggested outline/structure for the article.
         */
        List<String> suggestedOutline,

        /**
         * Suggested wiki pages for internal linking (CamelCase names).
         */
        List<String> relatedPageSuggestions,

        /**
         * Glossary of terms and definitions.
         */
        Map<String, String> glossary,

        /**
         * Areas where information is uncertain or needs verification.
         */
        List<String> uncertainAreas
) {
    public ResearchBrief {
        Objects.requireNonNull(keyFacts, "keyFacts must not be null");
        Objects.requireNonNull(suggestedOutline, "suggestedOutline must not be null");

        // Ensure immutable collections
        keyFacts = List.copyOf(keyFacts);
        sources = sources == null ? List.of() : List.copyOf(sources);
        suggestedOutline = List.copyOf(suggestedOutline);
        relatedPageSuggestions = relatedPageSuggestions == null ? List.of() : List.copyOf(relatedPageSuggestions);
        glossary = glossary == null ? Map.of() : Map.copyOf(glossary);
        uncertainAreas = uncertainAreas == null ? List.of() : List.copyOf(uncertainAreas);
    }

    /**
     * Check if the research brief has sufficient content to proceed.
     */
    public boolean isValid() {
        return !keyFacts.isEmpty() && !suggestedOutline.isEmpty();
    }

    /**
     * Get the count of key facts.
     */
    public int getFactCount() {
        return keyFacts.size();
    }
}
