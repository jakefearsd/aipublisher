package com.jakefear.aipublisher.search;

import java.util.List;

/**
 * Abstract interface for search providers.
 * Allows swapping between different search backends (Wikipedia, Google, custom APIs).
 */
public interface SearchProvider {

    /**
     * Search for information about a query.
     *
     * @param query The search query
     * @return List of search results, empty if none found or search disabled
     */
    List<SearchResult> search(String query);

    /**
     * Search for a topic within a domain context.
     * May use domain to refine/filter results.
     *
     * @param topic The topic to search for
     * @param domain The domain context (e.g., "Investing Basics")
     * @return List of search results
     */
    default List<SearchResult> searchInDomain(String topic, String domain) {
        // Default implementation: combine topic and domain in query
        return search(topic + " " + domain);
    }

    /**
     * Get related topics/concepts for a given topic.
     * Uses the source's link structure (e.g., Wikipedia's internal links).
     *
     * @param topic The topic to find related concepts for
     * @return List of related topic names
     */
    List<String> getRelatedTopics(String topic);

    /**
     * Get a summary/extract for a specific topic.
     *
     * @param topic The topic title
     * @return SearchResult with summary, or null if topic not found
     */
    SearchResult getTopicSummary(String topic);

    /**
     * Validate that a topic exists in the source.
     *
     * @param topic The topic to validate
     * @return Confidence score 0.0-1.0 (0 = doesn't exist, 1 = exact match found)
     */
    default double validateTopic(String topic) {
        SearchResult summary = getTopicSummary(topic);
        if (summary == null) {
            // Try a search as fallback
            List<SearchResult> results = search(topic);
            if (results.isEmpty()) {
                return 0.0;
            }
            // Check if any result title closely matches
            for (SearchResult result : results) {
                if (result.title().equalsIgnoreCase(topic)) {
                    return 0.9;
                }
                if (result.title().toLowerCase().contains(topic.toLowerCase())) {
                    return 0.6;
                }
            }
            return 0.3; // Found something but not exact match
        }
        return 1.0; // Exact topic found
    }

    /**
     * Check if this provider is enabled and available.
     *
     * @return true if the provider can perform searches
     */
    boolean isEnabled();

    /**
     * Get the name of this provider (e.g., "wikipedia", "google").
     *
     * @return Provider name for logging and selection
     */
    String getProviderName();
}
