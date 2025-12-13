package com.jakefear.aipublisher.glossary;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing glossary entries across articles.
 *
 * Provides:
 * - Term storage and retrieval
 * - Duplicate detection and conflict resolution
 * - Category-based organization
 * - Term consistency checking
 */
@Service
public class GlossaryService {

    // Store entries by canonical form for fast lookup
    private final Map<String, GlossaryEntry> entriesByCanonicalForm = new ConcurrentHashMap<>();

    // Store entries by category for organized access
    private final Map<String, Set<String>> entriesByCategory = new ConcurrentHashMap<>();

    /**
     * Add or update a glossary entry.
     *
     * @param entry The entry to add
     * @return The existing entry if one exists, or null if this is new
     */
    public GlossaryEntry addEntry(GlossaryEntry entry) {
        GlossaryEntry existing = entriesByCanonicalForm.put(entry.canonicalForm(), entry);

        // Update category index
        if (entry.category() != null && !entry.category().isBlank()) {
            entriesByCategory
                    .computeIfAbsent(entry.category().toLowerCase(), k -> ConcurrentHashMap.newKeySet())
                    .add(entry.canonicalForm());
        }

        return existing;
    }

    /**
     * Add entries from a simple map (term -> definition).
     *
     * @param glossaryMap Map of terms to definitions
     * @param sourceArticle The article these terms came from
     */
    public void addFromMap(Map<String, String> glossaryMap, String sourceArticle) {
        if (glossaryMap == null) {
            return;
        }

        for (Map.Entry<String, String> entry : glossaryMap.entrySet()) {
            GlossaryEntry glossaryEntry = new GlossaryEntry(
                    entry.getKey(),
                    null,
                    entry.getValue(),
                    List.of(),
                    sourceArticle,
                    null,
                    List.of(),
                    true,
                    null
            );
            addEntry(glossaryEntry);
        }
    }

    /**
     * Look up a term by its canonical form or alias.
     *
     * @param term The term to look up
     * @return The glossary entry, or Optional.empty() if not found
     */
    public Optional<GlossaryEntry> lookup(String term) {
        if (term == null || term.isBlank()) {
            return Optional.empty();
        }

        String normalized = term.toLowerCase().trim();
        GlossaryEntry direct = entriesByCanonicalForm.get(normalized);
        if (direct != null) {
            return Optional.of(direct);
        }

        // Search by alias
        return entriesByCanonicalForm.values().stream()
                .filter(entry -> entry.matches(term))
                .findFirst();
    }

    /**
     * Get all entries in a specific category.
     *
     * @param category The category to filter by
     * @return List of entries in that category
     */
    public List<GlossaryEntry> getByCategory(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }

        Set<String> canonicalForms = entriesByCategory.get(category.toLowerCase());
        if (canonicalForms == null) {
            return List.of();
        }

        return canonicalForms.stream()
                .map(entriesByCanonicalForm::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(GlossaryEntry::term))
                .collect(Collectors.toList());
    }

    /**
     * Get all categories with entry counts.
     *
     * @return Map of category names to entry counts
     */
    public Map<String, Integer> getCategoryCounts() {
        Map<String, Integer> counts = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : entriesByCategory.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }
        return counts;
    }

    /**
     * Find terms used in text that have glossary entries.
     *
     * @param text The text to search
     * @return List of found glossary entries
     */
    public List<GlossaryEntry> findTermsInText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        String lowerText = text.toLowerCase();
        return entriesByCanonicalForm.values().stream()
                .filter(entry -> {
                    // Check if the canonical term appears in the text
                    if (lowerText.contains(entry.canonicalForm())) {
                        return true;
                    }
                    // Check aliases
                    return entry.aliases().stream()
                            .map(String::toLowerCase)
                            .anyMatch(lowerText::contains);
                })
                .sorted(Comparator.comparing(GlossaryEntry::term))
                .collect(Collectors.toList());
    }

    /**
     * Check for inconsistent definitions of the same term.
     *
     * @return Map of terms to list of conflicting definitions
     */
    public Map<String, List<String>> findInconsistencies() {
        // Currently we only store one definition per term
        // This would be useful when importing from multiple sources
        return Map.of();
    }

    /**
     * Generate a formatted glossary section for inclusion in an article.
     *
     * @param entries The entries to include
     * @return JSPWiki-formatted glossary section
     */
    public String generateGlossarySection(List<GlossaryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("!! Glossary\n\n");

        for (GlossaryEntry entry : entries) {
            sb.append(";").append(entry.term()).append("\n");
            sb.append(":").append(entry.definition()).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Get all glossary entries sorted alphabetically.
     *
     * @return List of all entries
     */
    public List<GlossaryEntry> getAllEntries() {
        return entriesByCanonicalForm.values().stream()
                .sorted(Comparator.comparing(GlossaryEntry::term))
                .collect(Collectors.toList());
    }

    /**
     * Get the total number of glossary entries.
     *
     * @return Entry count
     */
    public int getEntryCount() {
        return entriesByCanonicalForm.size();
    }

    /**
     * Clear all glossary entries.
     */
    public void clear() {
        entriesByCanonicalForm.clear();
        entriesByCategory.clear();
    }
}
