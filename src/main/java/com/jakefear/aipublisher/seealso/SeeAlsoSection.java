package com.jakefear.aipublisher.seealso;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A complete "See Also" section for a wiki article.
 */
public record SeeAlsoSection(
        String sourceTopic,
        List<SeeAlsoEntry> entries
) {
    /**
     * Compact constructor with validation.
     */
    public SeeAlsoSection {
        Objects.requireNonNull(sourceTopic, "sourceTopic cannot be null");
        if (sourceTopic.isBlank()) {
            throw new IllegalArgumentException("sourceTopic cannot be blank");
        }
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    /**
     * Create an empty section.
     */
    public static SeeAlsoSection empty(String sourceTopic) {
        return new SeeAlsoSection(sourceTopic, List.of());
    }

    /**
     * Create a builder for constructing a section.
     */
    public static Builder builder(String sourceTopic) {
        return new Builder(sourceTopic);
    }

    /**
     * Check if this section is empty.
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * Get the total number of entries.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Get entries of a specific type.
     */
    public List<SeeAlsoEntry> getEntriesByType(SeeAlsoType type) {
        return entries.stream()
                .filter(e -> e.type() == type)
                .toList();
    }

    /**
     * Get entries for the main "See Also" section (broader, narrower, related).
     */
    public List<SeeAlsoEntry> getMainEntries() {
        return entries.stream()
                .filter(e -> e.type().isMainSection())
                .toList();
    }

    /**
     * Get internal wiki link entries.
     */
    public List<SeeAlsoEntry> getInternalEntries() {
        return entries.stream()
                .filter(e -> e.type().isInternal())
                .toList();
    }

    /**
     * Get external link entries.
     */
    public List<SeeAlsoEntry> getExternalEntries() {
        return entries.stream()
                .filter(e -> !e.type().isInternal())
                .toList();
    }

    /**
     * Get entries sorted by relevance (highest first).
     */
    public List<SeeAlsoEntry> getEntriesByRelevance() {
        return entries.stream()
                .sorted(Comparator.comparingDouble(SeeAlsoEntry::relevanceScore).reversed())
                .toList();
    }

    /**
     * Get top N entries by relevance.
     */
    public List<SeeAlsoEntry> getTopEntries(int count) {
        return getEntriesByRelevance().stream()
                .limit(count)
                .toList();
    }

    /**
     * Generate JSPWiki syntax for the "See Also" section.
     * Groups entries by type for better organization.
     */
    public String toWikiText() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("!!! See Also\n\n");

        // Group entries by type
        Map<SeeAlsoType, List<SeeAlsoEntry>> byType = entries.stream()
                .collect(Collectors.groupingBy(SeeAlsoEntry::type));

        // Order: broader, narrower, related, tutorial, reference, comparison, external
        List<SeeAlsoType> typeOrder = List.of(
                SeeAlsoType.BROADER,
                SeeAlsoType.NARROWER,
                SeeAlsoType.RELATED,
                SeeAlsoType.TUTORIAL,
                SeeAlsoType.REFERENCE,
                SeeAlsoType.COMPARISON,
                SeeAlsoType.EXTERNAL
        );

        boolean needsNewline = false;
        for (SeeAlsoType type : typeOrder) {
            List<SeeAlsoEntry> typeEntries = byType.get(type);
            if (typeEntries != null && !typeEntries.isEmpty()) {
                if (needsNewline) {
                    sb.append("\n");
                }

                // Add sub-heading for each type if there are multiple types
                if (byType.size() > 1) {
                    sb.append("__").append(type.getSectionHeading()).append("__\n");
                }

                // Add entries as bullet list
                for (SeeAlsoEntry entry : typeEntries) {
                    sb.append("* ").append(entry.toWikiText()).append("\n");
                }

                needsNewline = true;
            }
        }

        return sb.toString();
    }

    /**
     * Generate a simple flat "See Also" section without sub-headings.
     */
    public String toSimpleWikiText() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("!!! See Also\n\n");

        for (SeeAlsoEntry entry : entries) {
            sb.append("* ").append(entry.toWikiText()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Generate prompt format for AI writers.
     */
    public String toPromptFormat() {
        if (isEmpty()) {
            return "No suggested 'See Also' entries.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Suggested 'See Also' entries for '").append(sourceTopic).append("':\n\n");

        Map<SeeAlsoType, List<SeeAlsoEntry>> byType = entries.stream()
                .collect(Collectors.groupingBy(SeeAlsoEntry::type));

        for (Map.Entry<SeeAlsoType, List<SeeAlsoEntry>> typeGroup : byType.entrySet()) {
            sb.append(typeGroup.getKey().getDisplayName()).append(":\n");
            for (SeeAlsoEntry entry : typeGroup.getValue()) {
                sb.append("  - ").append(entry.title());
                if (entry.hasWikiPage()) {
                    sb.append(" (link: ").append(entry.wikiPage()).append(")");
                }
                if (entry.hasDescription()) {
                    sb.append(" - ").append(entry.description());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Builder for constructing SeeAlsoSection.
     */
    public static class Builder {
        private final String sourceTopic;
        private final List<SeeAlsoEntry> entries = new ArrayList<>();

        public Builder(String sourceTopic) {
            this.sourceTopic = sourceTopic;
        }

        public Builder add(SeeAlsoEntry entry) {
            entries.add(entry);
            return this;
        }

        public Builder addRelated(String title, String wikiPage, String description) {
            entries.add(SeeAlsoEntry.related(title, wikiPage, description));
            return this;
        }

        public Builder addBroader(String title, String wikiPage, String description) {
            entries.add(SeeAlsoEntry.broader(title, wikiPage, description));
            return this;
        }

        public Builder addNarrower(String title, String wikiPage, String description) {
            entries.add(SeeAlsoEntry.narrower(title, wikiPage, description));
            return this;
        }

        public Builder addTutorial(String title, String wikiPage, String description) {
            entries.add(SeeAlsoEntry.tutorial(title, wikiPage, description));
            return this;
        }

        public Builder addExternal(String title, String url, String description) {
            entries.add(SeeAlsoEntry.external(title, url, description));
            return this;
        }

        public SeeAlsoSection build() {
            return new SeeAlsoSection(sourceTopic, entries);
        }
    }
}
