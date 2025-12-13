package com.jakefear.aipublisher.search;

/**
 * Represents a single web search result.
 *
 * @param title The title of the search result
 * @param url The URL of the result
 * @param snippet A brief excerpt/description from the result
 * @param reliability The assessed reliability of the source
 */
public record SearchResult(
        String title,
        String url,
        String snippet,
        SourceReliability reliability
) {
    /**
     * Format this result for inclusion in an LLM prompt.
     */
    public String toPromptFormat() {
        StringBuilder sb = new StringBuilder();
        sb.append("- **").append(title).append("**\n");
        sb.append("  URL: ").append(url).append("\n");
        sb.append("  Reliability: ").append(reliability).append("\n");
        if (snippet != null && !snippet.isBlank()) {
            sb.append("  Excerpt: ").append(snippet).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format this result as a citation.
     */
    public String toCitation() {
        return String.format("[%s](%s) (%s)", title, url, reliability);
    }
}
