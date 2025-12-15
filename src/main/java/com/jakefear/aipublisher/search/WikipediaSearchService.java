package com.jakefear.aipublisher.search;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for searching Wikipedia to gather research material.
 * Uses the official Wikipedia/MediaWiki API which is free, reliable, and unlimited.
 *
 * Wikipedia content is ideal for generating wiki-style informational articles:
 * - High-quality, fact-checked, encyclopedic information
 * - Already in encyclopedic tone matching wiki style
 * - Related pages map directly to internal wiki links
 * - Categories align with wiki categorization
 */
@Service
public class WikipediaSearchService {

    private static final Logger log = LoggerFactory.getLogger(WikipediaSearchService.class);

    // Wikipedia API endpoints
    private static final String WIKIPEDIA_SEARCH_API = "https://en.wikipedia.org/w/api.php";
    private static final String WIKIPEDIA_REST_API = "https://en.wikipedia.org/api/rest_v1";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 500;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxResults;
    private final boolean enabled;

    /**
     * Default constructor for Spring.
     */
    public WikipediaSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.maxResults = 5;
        this.enabled = true;
    }

    public WikipediaSearchService(
            @Value("${search.max-results:5}") int maxResults,
            @Value("${search.enabled:true}") boolean enabled) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    // Constructor for testing
    WikipediaSearchService(HttpClient httpClient, ObjectMapper objectMapper, int maxResults, boolean enabled) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    /**
     * Search Wikipedia for information about a topic.
     *
     * @param query The search query
     * @return List of search results from Wikipedia
     */
    public List<SearchResult> search(String query) {
        if (!enabled) {
            log.debug("Wikipedia search is disabled, returning empty results");
            return List.of();
        }

        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            // Use Wikipedia's search API
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = WIKIPEDIA_SEARCH_API + "?action=query&list=search" +
                    "&srsearch=" + encodedQuery +
                    "&srlimit=" + maxResults +
                    "&srprop=snippet%7Ctitlesnippet" +  // %7C is URL-encoded pipe character
                    "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher; contact@example.com)")
                    .GET()
                    .build();

            String responseBody = executeWithRetry(request);
            if (responseBody == null) {
                return List.of();
            }

            return parseSearchResults(responseBody);
        } catch (InterruptedException e) {
            log.error("Wikipedia search interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Get a summary of a Wikipedia article by title.
     * Uses the REST API which returns cleaner, structured data.
     *
     * @param title The article title
     * @return SearchResult with article summary, or null if not found
     */
    public SearchResult getArticleSummary(String title) {
        if (!enabled || title == null || title.isBlank()) {
            return null;
        }

        try {
            String encodedTitle = URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8);
            String url = WIKIPEDIA_REST_API + "/page/summary/" + encodedTitle;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher; contact@example.com)")
                    .GET()
                    .build();

            String responseBody = executeWithRetry(request);
            if (responseBody == null) {
                return null;
            }

            return parseArticleSummary(responseBody);
        } catch (InterruptedException e) {
            log.error("Wikipedia article fetch interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Search for information to verify a specific claim.
     *
     * @param claim The claim to verify
     * @return List of search results that may help verify the claim
     */
    public List<SearchResult> searchForVerification(String claim) {
        return search(claim);
    }

    /**
     * Search for official documentation about a topic.
     * For Wikipedia, this just does a regular search since Wikipedia IS authoritative.
     *
     * @param topic The topic to find documentation for
     * @return List of search results
     */
    public List<SearchResult> searchOfficialDocs(String topic) {
        return search(topic);
    }

    /**
     * Get related pages for a given topic.
     * Uses Wikipedia's link API to find related articles.
     *
     * @param title The article title
     * @return List of related page titles
     */
    public List<String> getRelatedPages(String title) {
        if (!enabled || title == null || title.isBlank()) {
            return List.of();
        }

        try {
            String encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8);
            String url = WIKIPEDIA_SEARCH_API + "?action=query&titles=" + encodedTitle +
                    "&prop=links&pllimit=" + maxResults +
                    "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher; contact@example.com)")
                    .GET()
                    .build();

            String responseBody = executeWithRetry(request);
            if (responseBody == null) {
                return List.of();
            }

            return parseRelatedPages(responseBody);
        } catch (InterruptedException e) {
            log.error("Wikipedia related pages fetch interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Execute HTTP request with exponential backoff retry.
     */
    private String executeWithRetry(HttpRequest request) throws InterruptedException {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRIES) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    return response.body();
                } else if (isRetryableStatus(statusCode)) {
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        log.info("Wikipedia request returned status {}, retrying in {}ms (attempt {}/{})",
                                statusCode, backoffMs, attempt, MAX_RETRIES);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        log.warn("Wikipedia request failed with status {} after {} attempts", statusCode, MAX_RETRIES);
                        return null;
                    }
                } else {
                    log.warn("Wikipedia request failed with non-retryable status {}", statusCode);
                    return null;
                }
            } catch (IOException e) {
                attempt++;
                if (attempt < MAX_RETRIES) {
                    log.info("Wikipedia request failed with {}, retrying in {}ms (attempt {}/{})",
                            e.getMessage(), backoffMs, attempt, MAX_RETRIES);
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    log.error("Wikipedia request failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    return null;
                }
            }
        }

        return null;
    }

    /**
     * Check if a status code indicates a transient failure that should be retried.
     */
    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429  // Too Many Requests
                || statusCode == 503  // Service Unavailable
                || statusCode == 504; // Gateway Timeout
    }

    /**
     * Parse Wikipedia search API response into SearchResult objects.
     */
    List<SearchResult> parseSearchResults(String json) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode searchResults = root.path("query").path("search");

            if (searchResults.isArray()) {
                for (JsonNode result : searchResults) {
                    String title = result.path("title").asText("");
                    String snippet = cleanHtml(result.path("snippet").asText(""));

                    // Construct Wikipedia URL
                    String url = "https://en.wikipedia.org/wiki/" +
                            URLEncoder.encode(title.replace(" ", "_"), StandardCharsets.UTF_8);

                    if (!title.isEmpty()) {
                        results.add(new SearchResult(
                                title,
                                url,
                                snippet,
                                SourceReliability.REPUTABLE // Wikipedia is reputable
                        ));
                    }
                }
            }

            log.debug("Parsed {} Wikipedia search results", results.size());
        } catch (Exception e) {
            log.error("Failed to parse Wikipedia search results: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Parse Wikipedia REST API article summary response.
     */
    SearchResult parseArticleSummary(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            String title = root.path("title").asText("");
            String extract = root.path("extract").asText("");
            String url = root.path("content_urls").path("desktop").path("page").asText("");

            if (!title.isEmpty() && !extract.isEmpty()) {
                return new SearchResult(
                        title,
                        url,
                        extract,
                        SourceReliability.REPUTABLE
                );
            }
        } catch (Exception e) {
            log.error("Failed to parse Wikipedia article summary: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Parse related pages from Wikipedia links API response.
     */
    List<String> parseRelatedPages(String json) {
        List<String> pages = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode pagesNode = root.path("query").path("pages");

            if (pagesNode.isObject()) {
                for (JsonNode page : pagesNode) {
                    JsonNode links = page.path("links");
                    if (links.isArray()) {
                        for (JsonNode link : links) {
                            String title = link.path("title").asText("");
                            // Skip Wikipedia internal pages
                            if (!title.isEmpty() && !title.startsWith("Wikipedia:") &&
                                    !title.startsWith("Help:") && !title.startsWith("Category:")) {
                                pages.add(title);
                            }
                        }
                    }
                }
            }

            log.debug("Found {} related Wikipedia pages", pages.size());
        } catch (Exception e) {
            log.error("Failed to parse Wikipedia related pages: {}", e.getMessage());
        }

        return pages;
    }

    /**
     * Clean HTML tags and entities from text.
     */
    private String cleanHtml(String text) {
        if (text == null) return "";

        return text
                .replaceAll("<[^>]+>", "") // Remove HTML tags
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
