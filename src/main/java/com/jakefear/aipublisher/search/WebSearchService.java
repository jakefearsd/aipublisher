package com.jakefear.aipublisher.search;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for performing web searches to augment research and fact-checking.
 * Uses DuckDuckGo's HTML interface for search (no API key required).
 */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);
    private static final String DUCKDUCKGO_URL = "https://lite.duckduckgo.com/lite/";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000; // 1 second

    private final HttpClient httpClient;
    private final int maxResults;
    private final boolean enabled;

    /**
     * Default constructor for Spring.
     */
    public WebSearchService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.maxResults = 5;
        this.enabled = true;
    }

    public WebSearchService(
            @Value("${search.max-results:5}") int maxResults,
            @Value("${search.enabled:true}") boolean enabled) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    // Constructor for testing
    WebSearchService(HttpClient httpClient, int maxResults, boolean enabled) {
        this.httpClient = httpClient;
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    /**
     * Search the web for information about a topic.
     *
     * @param query The search query
     * @return List of search results
     */
    public List<SearchResult> search(String query) {
        if (!enabled) {
            log.debug("Web search is disabled, returning empty results");
            return List.of();
        }

        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = DUCKDUCKGO_URL + "?q=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .GET()
                    .build();

            return executeWithRetry(request);
        } catch (InterruptedException e) {
            log.error("Search interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Execute HTTP request with exponential backoff retry for transient failures.
     */
    private List<SearchResult> executeWithRetry(HttpRequest request) throws InterruptedException {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (attempt < MAX_RETRIES) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();

                if (statusCode == 200) {
                    return parseSearchResults(response.body());
                } else if (isRetryableStatus(statusCode)) {
                    attempt++;
                    if (attempt < MAX_RETRIES) {
                        log.info("Search request returned status {}, retrying in {}ms (attempt {}/{})",
                                statusCode, backoffMs, attempt, MAX_RETRIES);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2; // Exponential backoff
                    } else {
                        log.warn("Search request failed with status {} after {} attempts", statusCode, MAX_RETRIES);
                        return List.of();
                    }
                } else {
                    log.warn("Search request failed with non-retryable status {}", statusCode);
                    return List.of();
                }
            } catch (IOException e) {
                attempt++;
                if (attempt < MAX_RETRIES) {
                    log.info("Search request failed with {}, retrying in {}ms (attempt {}/{})",
                            e.getMessage(), backoffMs, attempt, MAX_RETRIES);
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    log.error("Search request failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    return List.of();
                }
            }
        }

        return List.of();
    }

    /**
     * Check if a status code indicates a transient failure that should be retried.
     */
    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 202  // Accepted (processing)
                || statusCode == 429  // Too Many Requests
                || statusCode == 503  // Service Unavailable
                || statusCode == 504; // Gateway Timeout
    }

    /**
     * Search for information to verify a specific claim.
     *
     * @param claim The claim to verify
     * @return List of search results that may help verify the claim
     */
    public List<SearchResult> searchForVerification(String claim) {
        // Add verification-oriented keywords
        String verificationQuery = claim + " fact check verify";
        return search(verificationQuery);
    }

    /**
     * Search for official documentation about a topic.
     *
     * @param topic The topic to find documentation for
     * @return List of search results focused on official docs
     */
    public List<SearchResult> searchOfficialDocs(String topic) {
        String docsQuery = topic + " official documentation site:*.org OR site:*.io OR site:github.com";
        return search(docsQuery);
    }

    /**
     * Parse DuckDuckGo HTML results into SearchResult objects.
     */
    List<SearchResult> parseSearchResults(String html) {
        List<SearchResult> results = new ArrayList<>();

        // Pattern to match DuckDuckGo result entries
        // Results are in <a class="result__a" href="...">title</a>
        // with snippets in <a class="result__snippet">...</a>
        Pattern resultPattern = Pattern.compile(
                "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>([^<]+)</a>",
                Pattern.CASE_INSENSITIVE
        );

        Pattern snippetPattern = Pattern.compile(
                "<a[^>]*class=\"result__snippet\"[^>]*>([^<]+(?:<[^>]+>[^<]*</[^>]+>)*[^<]*)</a>",
                Pattern.CASE_INSENSITIVE
        );

        Matcher resultMatcher = resultPattern.matcher(html);
        Matcher snippetMatcher = snippetPattern.matcher(html);

        while (resultMatcher.find() && results.size() < maxResults) {
            String url = resultMatcher.group(1);
            String title = cleanHtml(resultMatcher.group(2));

            String snippet = "";
            if (snippetMatcher.find()) {
                snippet = cleanHtml(snippetMatcher.group(1));
            }

            // Skip DuckDuckGo internal links
            if (url.startsWith("//duckduckgo.com") || url.contains("duckduckgo.com")) {
                continue;
            }

            // Clean up the URL (DuckDuckGo sometimes wraps URLs)
            url = extractActualUrl(url);

            if (!url.isEmpty() && !title.isEmpty()) {
                results.add(new SearchResult(title, url, snippet, assessReliability(url)));
            }
        }

        log.debug("Parsed {} search results", results.size());
        return results;
    }

    /**
     * Extract the actual URL from DuckDuckGo's redirect URL.
     */
    private String extractActualUrl(String url) {
        // DuckDuckGo sometimes uses uddg= parameter for the actual URL
        if (url.contains("uddg=")) {
            Pattern uddgPattern = Pattern.compile("uddg=([^&]+)");
            Matcher matcher = uddgPattern.matcher(url);
            if (matcher.find()) {
                try {
                    return java.net.URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    // Fall through to return original
                }
            }
        }
        return url;
    }

    /**
     * Assess the reliability of a source based on its URL.
     */
    SourceReliability assessReliability(String url) {
        String lowerUrl = url.toLowerCase();

        // Official documentation sites
        if (lowerUrl.contains("docs.") || lowerUrl.contains("/documentation") ||
                lowerUrl.contains("developer.") || lowerUrl.contains("spec.")) {
            return SourceReliability.OFFICIAL;
        }

        // Academic sources
        if (lowerUrl.contains(".edu") || lowerUrl.contains("arxiv.org") ||
                lowerUrl.contains("acm.org") || lowerUrl.contains("ieee.org")) {
            return SourceReliability.ACADEMIC;
        }

        // Major tech publishers
        if (lowerUrl.contains("oreilly.com") || lowerUrl.contains("manning.com") ||
                lowerUrl.contains("infoq.com") || lowerUrl.contains("martinfowler.com")) {
            return SourceReliability.AUTHORITATIVE;
        }

        // Reputable tech sites
        if (lowerUrl.contains("github.com") || lowerUrl.contains("stackoverflow.com") ||
                lowerUrl.contains("medium.com") || lowerUrl.contains("dev.to") ||
                lowerUrl.contains("wikipedia.org")) {
            return SourceReliability.REPUTABLE;
        }

        // Community sources
        if (lowerUrl.contains("reddit.com") || lowerUrl.contains("quora.com") ||
                lowerUrl.contains("forum") || lowerUrl.contains("community")) {
            return SourceReliability.COMMUNITY;
        }

        return SourceReliability.UNCERTAIN;
    }

    /**
     * Clean HTML entities and tags from text.
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
