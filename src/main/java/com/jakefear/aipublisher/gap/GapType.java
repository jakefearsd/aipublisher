package com.jakefear.aipublisher.gap;

/**
 * Classification of gap concepts found in generated wiki content.
 * Used to determine how to handle missing pages referenced by internal links.
 */
public enum GapType {

    /**
     * Technical term or concept that needs a brief definition page.
     * These are typically financial, technical, or domain-specific terms
     * that benefit from a short explanation (100-250 words).
     */
    DEFINITION("Definition", "Brief definition page needed"),

    /**
     * Alias or alternate name for an existing page.
     * Should create a redirect page pointing to the canonical article.
     */
    REDIRECT("Redirect", "Alias for existing page"),

    /**
     * Significant concept that deserves a full article.
     * These gaps should be flagged for user review and potential
     * addition to the universe for full generation.
     */
    FULL_ARTICLE("Full Article", "Significant gap - needs full coverage"),

    /**
     * Generic term that doesn't need its own page.
     * Common words like "investment", "money", etc. that are
     * too broad to warrant dedicated pages.
     */
    IGNORE("Ignore", "Too generic - no page needed");

    private final String displayName;
    private final String description;

    GapType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Parse gap type from string, case-insensitive.
     */
    public static GapType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Try matching by display name
            for (GapType type : values()) {
                if (type.displayName.equalsIgnoreCase(value.trim())) {
                    return type;
                }
            }
            return null;
        }
    }
}
