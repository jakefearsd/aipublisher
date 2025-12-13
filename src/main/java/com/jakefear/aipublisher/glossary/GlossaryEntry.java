package com.jakefear.aipublisher.glossary;

import java.util.List;
import java.util.Objects;

/**
 * A glossary entry representing a term and its definition.
 */
public record GlossaryEntry(
        /**
         * The term being defined (e.g., "Event Sourcing", "API Gateway").
         */
        String term,

        /**
         * The canonical form of the term (lowercase, normalized).
         */
        String canonicalForm,

        /**
         * A clear, concise definition of the term.
         */
        String definition,

        /**
         * Alternative forms or aliases for this term.
         */
        List<String> aliases,

        /**
         * The source article where this term was first defined.
         */
        String sourceArticle,

        /**
         * Category for grouping related terms (e.g., "Architecture", "Messaging").
         */
        String category,

        /**
         * Related terms that should be cross-referenced.
         */
        List<String> relatedTerms,

        /**
         * Whether this is a primary/authoritative definition.
         */
        boolean primary
) {
    public GlossaryEntry {
        Objects.requireNonNull(term, "term must not be null");
        Objects.requireNonNull(definition, "definition must not be null");

        if (term.isBlank()) {
            throw new IllegalArgumentException("term must not be blank");
        }
        if (definition.isBlank()) {
            throw new IllegalArgumentException("definition must not be blank");
        }

        // Normalize the canonical form
        canonicalForm = term.toLowerCase().trim();

        // Ensure immutable lists
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
        relatedTerms = relatedTerms == null ? List.of() : List.copyOf(relatedTerms);
    }

    /**
     * Create a simple glossary entry.
     */
    public static GlossaryEntry simple(String term, String definition) {
        return new GlossaryEntry(term, null, definition, List.of(), null, null, List.of(), true);
    }

    /**
     * Create a glossary entry with category.
     */
    public static GlossaryEntry withCategory(String term, String definition, String category) {
        return new GlossaryEntry(term, null, definition, List.of(), null, category, List.of(), true);
    }

    /**
     * Check if this entry matches a given term (case-insensitive).
     */
    public boolean matches(String searchTerm) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return false;
        }
        String normalized = searchTerm.toLowerCase().trim();
        if (canonicalForm.equals(normalized)) {
            return true;
        }
        return aliases.stream()
                .map(String::toLowerCase)
                .anyMatch(alias -> alias.equals(normalized));
    }

    /**
     * Get a formatted reference suitable for wiki linking.
     */
    public String getWikiLink() {
        // Convert to CamelCase for wiki page name
        return term.replaceAll("\\s+", "");
    }
}
