package com.jakefear.aipublisher.prerequisites;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Set of prerequisites for a topic.
 */
public record PrerequisiteSet(
        /**
         * The topic these prerequisites are for.
         */
        String topic,

        /**
         * List of prerequisites.
         */
        List<Prerequisite> prerequisites
) {
    public PrerequisiteSet {
        Objects.requireNonNull(topic, "topic must not be null");
        prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);

        if (topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
    }

    /**
     * Get hard (required) prerequisites only.
     */
    public List<Prerequisite> getHardPrerequisites() {
        return prerequisites.stream()
                .filter(p -> p.type() == PrerequisiteType.HARD)
                .collect(Collectors.toList());
    }

    /**
     * Get soft (recommended) prerequisites only.
     */
    public List<Prerequisite> getSoftPrerequisites() {
        return prerequisites.stream()
                .filter(p -> p.type() == PrerequisiteType.SOFT)
                .collect(Collectors.toList());
    }

    /**
     * Get assumed knowledge prerequisites.
     */
    public List<Prerequisite> getAssumedPrerequisites() {
        return prerequisites.stream()
                .filter(p -> p.type() == PrerequisiteType.ASSUMED)
                .collect(Collectors.toList());
    }

    /**
     * Check if there are any hard prerequisites.
     */
    public boolean hasHardPrerequisites() {
        return prerequisites.stream().anyMatch(p -> p.type() == PrerequisiteType.HARD);
    }

    /**
     * Check if there are any prerequisites at all.
     */
    public boolean isEmpty() {
        return prerequisites.isEmpty();
    }

    /**
     * Get total count of prerequisites.
     */
    public int size() {
        return prerequisites.size();
    }

    /**
     * Generate a JSPWiki prerequisite callout section.
     */
    public String toWikiCallout() {
        if (isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Hard prerequisites get a warning-style callout
        List<Prerequisite> hard = getHardPrerequisites();
        if (!hard.isEmpty()) {
            sb.append("%%warning\n");
            sb.append("__Before you begin:__ This article assumes you understand:\n");
            for (Prerequisite p : hard) {
                sb.append("* ").append(p.toWikiText());
                if (!p.reason().isBlank()) {
                    sb.append(" - ").append(p.reason());
                }
                sb.append("\n");
            }
            sb.append("%%\n\n");
        }

        // Soft prerequisites get an info-style callout
        List<Prerequisite> soft = getSoftPrerequisites();
        if (!soft.isEmpty()) {
            sb.append("%%information\n");
            sb.append("__Helpful background:__ You may find it useful to first read:\n");
            for (Prerequisite p : soft) {
                sb.append("* ").append(p.toWikiText());
                if (!p.reason().isBlank()) {
                    sb.append(" - ").append(p.reason());
                }
                sb.append("\n");
            }
            sb.append("%%\n\n");
        }

        return sb.toString();
    }

    /**
     * Generate a simple list format for writer prompts.
     */
    public String toPromptFormat() {
        if (isEmpty()) {
            return "No specific prerequisites identified.";
        }

        StringBuilder sb = new StringBuilder();

        List<Prerequisite> hard = getHardPrerequisites();
        if (!hard.isEmpty()) {
            sb.append("Required Prerequisites:\n");
            for (Prerequisite p : hard) {
                sb.append("- ").append(p.topic());
                if (!p.reason().isBlank()) {
                    sb.append(": ").append(p.reason());
                }
                sb.append("\n");
            }
        }

        List<Prerequisite> soft = getSoftPrerequisites();
        if (!soft.isEmpty()) {
            if (!hard.isEmpty()) sb.append("\n");
            sb.append("Recommended Background:\n");
            for (Prerequisite p : soft) {
                sb.append("- ").append(p.topic());
                if (!p.reason().isBlank()) {
                    sb.append(": ").append(p.reason());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Create an empty prerequisite set.
     */
    public static PrerequisiteSet empty(String topic) {
        return new PrerequisiteSet(topic, List.of());
    }

    /**
     * Builder for creating prerequisite sets.
     */
    public static Builder builder(String topic) {
        return new Builder(topic);
    }

    public static class Builder {
        private final String topic;
        private final List<Prerequisite> prerequisites = new ArrayList<>();

        private Builder(String topic) {
            this.topic = topic;
        }

        public Builder addHard(String prereqTopic, String wikiPage, String reason) {
            prerequisites.add(Prerequisite.hard(prereqTopic, wikiPage, reason));
            return this;
        }

        public Builder addHard(String prereqTopic, String reason) {
            prerequisites.add(Prerequisite.hard(prereqTopic, reason));
            return this;
        }

        public Builder addSoft(String prereqTopic, String wikiPage, String reason) {
            prerequisites.add(Prerequisite.soft(prereqTopic, wikiPage, reason));
            return this;
        }

        public Builder addSoft(String prereqTopic, String reason) {
            prerequisites.add(Prerequisite.soft(prereqTopic, reason));
            return this;
        }

        public Builder addAssumed(String prereqTopic) {
            prerequisites.add(Prerequisite.assumed(prereqTopic));
            return this;
        }

        public Builder add(Prerequisite prerequisite) {
            prerequisites.add(prerequisite);
            return this;
        }

        public PrerequisiteSet build() {
            return new PrerequisiteSet(topic, prerequisites);
        }
    }
}
