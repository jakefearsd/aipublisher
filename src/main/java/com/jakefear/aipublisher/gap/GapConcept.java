package com.jakefear.aipublisher.gap;

import com.jakefear.aipublisher.util.PageNameUtils;

import java.util.List;
import java.util.Objects;

/**
 * Represents a gap concept - a wiki page that is referenced by internal links
 * but does not exist. Used for stub generation.
 */
public record GapConcept(
        /**
         * The concept name as it appears in links (may have spaces, mixed case).
         */
        String name,

        /**
         * The normalized CamelCase page name for the wiki.
         */
        String pageName,

        /**
         * How this gap should be handled.
         */
        GapType type,

        /**
         * List of page names that reference this concept.
         */
        List<String> referencedBy,

        /**
         * For REDIRECT type: the existing page this should redirect to.
         */
        String redirectTarget,

        /**
         * Suggested category for the stub page.
         */
        String category
) {
    /**
     * Compact constructor with validation.
     */
    public GapConcept {
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }
        Objects.requireNonNull(type, "type cannot be null");

        // Normalize page name if not provided
        if (pageName == null || pageName.isBlank()) {
            pageName = PageNameUtils.toCamelCaseOrDefault(name, "UnnamedPage");
        }

        // Ensure immutable list
        referencedBy = referencedBy == null ? List.of() : List.copyOf(referencedBy);

        // Defaults
        if (redirectTarget == null) redirectTarget = "";
        if (category == null) category = "";
    }

    /**
     * Create a gap concept with just name and type (for simple cases).
     */
    public static GapConcept of(String name, GapType type) {
        return new GapConcept(name, null, type, List.of(), null, null);
    }

    /**
     * Create a gap concept with references.
     */
    public static GapConcept withReferences(String name, GapType type, List<String> referencedBy) {
        return new GapConcept(name, null, type, referencedBy, null, null);
    }

    /**
     * Create a redirect gap concept.
     */
    public static GapConcept redirect(String name, String redirectTarget) {
        return new GapConcept(name, null, GapType.REDIRECT, List.of(), redirectTarget, null);
    }

    /**
     * Create a definition gap concept with category.
     */
    public static GapConcept definition(String name, List<String> referencedBy, String category) {
        return new GapConcept(name, null, GapType.DEFINITION, referencedBy, null, category);
    }

    /**
     * Check if this gap needs a page to be generated.
     */
    public boolean needsGeneration() {
        return type == GapType.DEFINITION || type == GapType.REDIRECT;
    }

    /**
     * Check if this gap should be flagged for user review.
     */
    public boolean needsReview() {
        return type == GapType.FULL_ARTICLE;
    }

    /**
     * Get a display string for reporting.
     */
    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" [").append(type.getDisplayName()).append("]");
        if (type == GapType.REDIRECT && !redirectTarget.isBlank()) {
            sb.append(" -> ").append(redirectTarget);
        }
        if (!referencedBy.isEmpty()) {
            sb.append(" (referenced by: ").append(String.join(", ", referencedBy)).append(")");
        }
        return sb.toString();
    }

    /**
     * Create a builder for more complex construction.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private String name;
        private String pageName;
        private GapType type = GapType.DEFINITION;
        private List<String> referencedBy = List.of();
        private String redirectTarget;
        private String category;

        public Builder(String name) {
            this.name = name;
        }

        public Builder pageName(String pageName) {
            this.pageName = pageName;
            return this;
        }

        public Builder type(GapType type) {
            this.type = type;
            return this;
        }

        public Builder referencedBy(List<String> referencedBy) {
            this.referencedBy = referencedBy;
            return this;
        }

        public Builder addReference(String reference) {
            this.referencedBy = new java.util.ArrayList<>(this.referencedBy);
            this.referencedBy.add(reference);
            return this;
        }

        public Builder redirectTarget(String redirectTarget) {
            this.redirectTarget = redirectTarget;
            return this;
        }

        public Builder category(String category) {
            this.category = category;
            return this;
        }

        public GapConcept build() {
            return new GapConcept(name, pageName, type, referencedBy, redirectTarget, category);
        }
    }
}
