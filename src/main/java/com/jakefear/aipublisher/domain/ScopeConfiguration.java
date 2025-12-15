package com.jakefear.aipublisher.domain;

import java.util.*;

/**
 * Configuration defining the scope and boundaries of a topic universe.
 * Captures user decisions about what to include, exclude, and emphasize.
 */
public record ScopeConfiguration(
        Set<String> assumedKnowledge,
        Set<String> outOfScope,
        Set<String> focusAreas,
        String preferredLanguage,
        String audienceDescription,
        String domainDescription,
        String intent
) {
    /**
     * Compact constructor with normalization.
     */
    public ScopeConfiguration {
        assumedKnowledge = assumedKnowledge == null ? Set.of() : Set.copyOf(assumedKnowledge);
        outOfScope = outOfScope == null ? Set.of() : Set.copyOf(outOfScope);
        focusAreas = focusAreas == null ? Set.of() : Set.copyOf(focusAreas);
        if (preferredLanguage == null) preferredLanguage = "";
        if (audienceDescription == null) audienceDescription = "";
        if (domainDescription == null) domainDescription = "";
        if (intent == null) intent = "";
    }

    /**
     * Create an empty scope configuration.
     */
    public static ScopeConfiguration empty() {
        return new ScopeConfiguration(
                Set.of(), Set.of(), Set.of(), "", "", "", ""
        );
    }

    /**
     * Create a builder for constructing scope configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder from this configuration.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Check if a topic/concept is assumed knowledge.
     */
    public boolean isAssumed(String concept) {
        return assumedKnowledge.stream()
                .anyMatch(k -> k.equalsIgnoreCase(concept) ||
                        concept.toLowerCase().contains(k.toLowerCase()));
    }

    /**
     * Check if a topic is explicitly out of scope.
     */
    public boolean isOutOfScope(String topic) {
        return outOfScope.stream()
                .anyMatch(e -> e.equalsIgnoreCase(topic) ||
                        topic.toLowerCase().contains(e.toLowerCase()));
    }

    /**
     * Check if an aspect is a focus area.
     */
    public boolean isFocusArea(String aspect) {
        return focusAreas.stream()
                .anyMatch(f -> f.equalsIgnoreCase(aspect) ||
                        aspect.toLowerCase().contains(f.toLowerCase()));
    }

    /**
     * Check if this configuration has any constraints.
     */
    public boolean hasConstraints() {
        return !assumedKnowledge.isEmpty() ||
                !outOfScope.isEmpty() ||
                !focusAreas.isEmpty();
    }

    /**
     * Format scope for inclusion in AI prompts.
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();

        if (!audienceDescription.isBlank()) {
            sb.append("Target Audience: ").append(audienceDescription).append("\n\n");
        }

        if (!domainDescription.isBlank()) {
            sb.append("Domain Context: ").append(domainDescription).append("\n\n");
        }

        if (!intent.isBlank()) {
            sb.append("Writing Intent: ").append(intent).append("\n\n");
        }

        if (!assumedKnowledge.isEmpty()) {
            sb.append("Assumed Knowledge (do not explain):\n");
            for (String knowledge : assumedKnowledge) {
                sb.append("  - ").append(knowledge).append("\n");
            }
            sb.append("\n");
        }

        if (!outOfScope.isEmpty()) {
            sb.append("Out of Scope (do not cover):\n");
            for (String excluded : outOfScope) {
                sb.append("  - ").append(excluded).append("\n");
            }
            sb.append("\n");
        }

        if (!focusAreas.isEmpty()) {
            sb.append("Focus Areas (emphasize):\n");
            for (String focus : focusAreas) {
                sb.append("  - ").append(focus).append("\n");
            }
            sb.append("\n");
        }

        if (!preferredLanguage.isBlank()) {
            sb.append("Preferred Programming Language: ").append(preferredLanguage).append("\n");
        }

        return sb.toString();
    }

    /**
     * Builder for ScopeConfiguration.
     */
    public static class Builder {
        private Set<String> assumedKnowledge = new LinkedHashSet<>();
        private Set<String> outOfScope = new LinkedHashSet<>();
        private Set<String> focusAreas = new LinkedHashSet<>();
        private String preferredLanguage = "";
        private String audienceDescription = "";
        private String domainDescription = "";
        private String intent = "";

        public Builder() {}

        public Builder(ScopeConfiguration config) {
            this.assumedKnowledge = new LinkedHashSet<>(config.assumedKnowledge);
            this.outOfScope = new LinkedHashSet<>(config.outOfScope);
            this.focusAreas = new LinkedHashSet<>(config.focusAreas);
            this.preferredLanguage = config.preferredLanguage;
            this.audienceDescription = config.audienceDescription;
            this.domainDescription = config.domainDescription;
            this.intent = config.intent;
        }

        public Builder addAssumedKnowledge(String knowledge) {
            this.assumedKnowledge.add(knowledge);
            return this;
        }

        public Builder assumedKnowledge(Collection<String> knowledge) {
            this.assumedKnowledge = new LinkedHashSet<>(knowledge);
            return this;
        }

        public Builder addOutOfScope(String excluded) {
            this.outOfScope.add(excluded);
            return this;
        }

        public Builder outOfScope(Collection<String> excluded) {
            this.outOfScope = new LinkedHashSet<>(excluded);
            return this;
        }

        public Builder addFocusArea(String focus) {
            this.focusAreas.add(focus);
            return this;
        }

        public Builder focusAreas(Collection<String> focus) {
            this.focusAreas = new LinkedHashSet<>(focus);
            return this;
        }

        public Builder preferredLanguage(String language) {
            this.preferredLanguage = language;
            return this;
        }

        public Builder audienceDescription(String description) {
            this.audienceDescription = description;
            return this;
        }

        public Builder domainDescription(String description) {
            this.domainDescription = description;
            return this;
        }

        public Builder intent(String intent) {
            this.intent = intent;
            return this;
        }

        public ScopeConfiguration build() {
            return new ScopeConfiguration(
                    assumedKnowledge, outOfScope, focusAreas,
                    preferredLanguage, audienceDescription, domainDescription, intent
            );
        }
    }
}
