package com.jakefear.aipublisher.domain;

import java.time.Instant;
import java.util.*;

/**
 * Rich context about a domain, built from search results and user input.
 * Flows through to article generation for consistent terminology and themes.
 */
public record DomainContext(
        /**
         * Summary of the domain from search (e.g., Wikipedia intro paragraph).
         */
        String domainSummary,

        /**
         * Key themes discovered across the domain for consistency.
         */
        List<String> keyThemes,

        /**
         * Core terminology with definitions for consistent usage.
         */
        Map<String, String> glossary,

        /**
         * Authoritative source URLs discovered during research.
         */
        List<String> authorityUrls,

        /**
         * Topics grouped by theme/cluster for organization.
         */
        Map<String, List<String>> topicClusters,

        /**
         * Writing guidelines derived from scope and search context.
         */
        String writingGuidelines,

        /**
         * When this context was last refreshed from search.
         */
        Instant lastUpdated
) {
    /**
     * Compact constructor with normalization.
     */
    public DomainContext {
        if (domainSummary == null) domainSummary = "";
        keyThemes = keyThemes == null ? List.of() : List.copyOf(keyThemes);
        glossary = glossary == null ? Map.of() : Map.copyOf(glossary);
        authorityUrls = authorityUrls == null ? List.of() : List.copyOf(authorityUrls);
        topicClusters = topicClusters == null ? Map.of() : copyOfClusters(topicClusters);
        if (writingGuidelines == null) writingGuidelines = "";
        if (lastUpdated == null) lastUpdated = Instant.now();
    }

    /**
     * Deep copy of topic clusters map.
     */
    private static Map<String, List<String>> copyOfClusters(Map<String, List<String>> clusters) {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : clusters.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }

    /**
     * Create an empty domain context.
     */
    public static DomainContext empty() {
        return new DomainContext(
                "", List.of(), Map.of(), List.of(), Map.of(), "", Instant.now()
        );
    }

    /**
     * Create a builder for constructing domain context.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder from this context.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Check if this context has meaningful content.
     */
    public boolean hasContent() {
        return !domainSummary.isBlank() ||
                !keyThemes.isEmpty() ||
                !glossary.isEmpty();
    }

    /**
     * Format context for inclusion in LLM prompts.
     * Combines with ScopeConfiguration for complete context.
     */
    public String toPromptFormat(ScopeConfiguration scope) {
        StringBuilder sb = new StringBuilder();

        // Domain background from search
        if (!domainSummary.isBlank()) {
            sb.append("=== DOMAIN BACKGROUND ===\n");
            sb.append(domainSummary).append("\n\n");
        }

        // Include scope configuration
        if (scope != null) {
            String scopeFormat = scope.toPromptFormat();
            if (!scopeFormat.isBlank()) {
                sb.append("=== SCOPE & AUDIENCE ===\n");
                sb.append(scopeFormat).append("\n");
            }
        }

        // Key themes to maintain consistency
        if (!keyThemes.isEmpty()) {
            sb.append("=== KEY THEMES (maintain throughout) ===\n");
            for (String theme : keyThemes) {
                sb.append("- ").append(theme).append("\n");
            }
            sb.append("\n");
        }

        // Terminology for consistent usage
        if (!glossary.isEmpty()) {
            sb.append("=== DOMAIN TERMINOLOGY ===\n");
            for (Map.Entry<String, String> entry : glossary.entrySet()) {
                sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n");
        }

        // Writing guidelines
        if (!writingGuidelines.isBlank()) {
            sb.append("=== WRITING GUIDELINES ===\n");
            sb.append(writingGuidelines).append("\n");
        }

        return sb.toString();
    }

    /**
     * Format context without scope (for simpler use cases).
     */
    public String toPromptFormat() {
        return toPromptFormat(null);
    }

    /**
     * Builder for DomainContext.
     */
    public static class Builder {
        private String domainSummary = "";
        private List<String> keyThemes = new ArrayList<>();
        private Map<String, String> glossary = new LinkedHashMap<>();
        private List<String> authorityUrls = new ArrayList<>();
        private Map<String, List<String>> topicClusters = new LinkedHashMap<>();
        private String writingGuidelines = "";
        private Instant lastUpdated = Instant.now();

        public Builder() {}

        public Builder(DomainContext context) {
            this.domainSummary = context.domainSummary;
            this.keyThemes = new ArrayList<>(context.keyThemes);
            this.glossary = new LinkedHashMap<>(context.glossary);
            this.authorityUrls = new ArrayList<>(context.authorityUrls);
            this.topicClusters = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : context.topicClusters.entrySet()) {
                this.topicClusters.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            this.writingGuidelines = context.writingGuidelines;
            this.lastUpdated = context.lastUpdated;
        }

        public Builder domainSummary(String summary) {
            this.domainSummary = summary;
            return this;
        }

        public Builder keyThemes(List<String> themes) {
            this.keyThemes = new ArrayList<>(themes);
            return this;
        }

        public Builder addKeyTheme(String theme) {
            this.keyThemes.add(theme);
            return this;
        }

        public Builder glossary(Map<String, String> glossary) {
            this.glossary = new LinkedHashMap<>(glossary);
            return this;
        }

        public Builder addGlossaryTerm(String term, String definition) {
            this.glossary.put(term, definition);
            return this;
        }

        public Builder authorityUrls(List<String> urls) {
            this.authorityUrls = new ArrayList<>(urls);
            return this;
        }

        public Builder addAuthorityUrl(String url) {
            this.authorityUrls.add(url);
            return this;
        }

        public Builder topicClusters(Map<String, List<String>> clusters) {
            this.topicClusters = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> entry : clusters.entrySet()) {
                this.topicClusters.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return this;
        }

        public Builder addTopicCluster(String clusterName, List<String> topics) {
            this.topicClusters.put(clusterName, new ArrayList<>(topics));
            return this;
        }

        public Builder writingGuidelines(String guidelines) {
            this.writingGuidelines = guidelines;
            return this;
        }

        public Builder lastUpdated(Instant lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public DomainContext build() {
            return new DomainContext(
                    domainSummary, keyThemes, glossary, authorityUrls,
                    topicClusters, writingGuidelines, lastUpdated
            );
        }
    }
}
