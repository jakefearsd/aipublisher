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
import java.util.*;

/**
 * Search provider using Wikidata's knowledge base.
 *
 * Advantages over Wikipedia:
 * - wbsearchentities searches labels AND aliases (more forgiving than exact title match)
 * - Structured entity data with descriptions
 * - Links to Wikipedia articles when they exist
 * - Better coverage for concepts that don't have dedicated Wikipedia articles
 *
 * Free, unlimited, no API key required.
 */
@Service
public class WikidataSearchService implements SearchProvider {

    private static final Logger log = LoggerFactory.getLogger(WikidataSearchService.class);

    private static final String WIKIDATA_API = "https://www.wikidata.org/w/api.php";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 500;

    // Properties for finding related entities
    private static final List<String> RELATED_PROPERTIES = List.of(
            "P279",  // subclass of
            "P31",   // instance of
            "P361",  // part of
            "P527",  // has part
            "P1535", // used by
            "P366",  // has use
            "P1269", // facet of
            "P461"   // opposite of
    );

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxResults;
    private final boolean enabled;

    public WikidataSearchService() {
        this(5, true);
    }

    public WikidataSearchService(
            @Value("${search.max-results:5}") int maxResults,
            @Value("${search.wikidata.enabled:true}") boolean enabled) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.objectMapper = new ObjectMapper();
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    // Constructor for testing
    WikidataSearchService(HttpClient httpClient, ObjectMapper objectMapper, int maxResults, boolean enabled) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.maxResults = maxResults;
        this.enabled = enabled;
    }

    @Override
    public String getProviderName() {
        return "wikidata";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxResults() {
        return maxResults;
    }

    /**
     * Search for entities matching the query.
     * Uses wbsearchentities which searches labels and aliases.
     */
    @Override
    public List<SearchResult> search(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = WIKIDATA_API + "?action=wbsearchentities" +
                    "&search=" + encodedQuery +
                    "&language=en" +
                    "&limit=" + maxResults +
                    "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher)")
                    .GET()
                    .build();

            String responseBody = executeWithRetry(request);
            if (responseBody == null) {
                return List.of();
            }

            return parseSearchResults(responseBody);
        } catch (InterruptedException e) {
            log.error("Wikidata search interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Search for information to verify a specific claim.
     * Delegates to regular search since Wikidata uses the same mechanism.
     *
     * @param claim The claim to verify
     * @return List of search results that may help verify the claim
     */
    public List<SearchResult> searchForVerification(String claim) {
        return search(claim);
    }

    /**
     * Get a summary for a topic.
     * Searches for the entity and returns its description.
     */
    @Override
    public SearchResult getTopicSummary(String topic) {
        if (!enabled || topic == null || topic.isBlank()) {
            return null;
        }

        // First search for the entity
        List<SearchResult> results = search(topic);
        if (results.isEmpty()) {
            return null;
        }

        // Return the best match (first result has label/description)
        SearchResult best = results.get(0);

        // Check if we got a close match
        if (isSimilarMatch(topic, best.title())) {
            log.debug("Found Wikidata match for '{}': {}", topic, best.title());
            return best;
        }

        // Return anyway with lower implicit confidence
        log.debug("Partial Wikidata match for '{}': {}", topic, best.title());
        return best;
    }

    /**
     * Get related topics by following Wikidata property relationships.
     */
    @Override
    public List<String> getRelatedTopics(String topic) {
        if (!enabled || topic == null || topic.isBlank()) {
            return List.of();
        }

        try {
            // First find the entity ID
            String entityId = findEntityId(topic);
            if (entityId == null) {
                return List.of();
            }

            // Get entity claims (properties)
            String url = WIKIDATA_API + "?action=wbgetentities" +
                    "&ids=" + entityId +
                    "&props=claims" +
                    "&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher)")
                    .GET()
                    .build();

            String responseBody = executeWithRetry(request);
            if (responseBody == null) {
                return List.of();
            }

            // Extract related entity IDs from claims
            Set<String> relatedIds = parseRelatedEntityIds(responseBody, entityId);
            if (relatedIds.isEmpty()) {
                return List.of();
            }

            // Fetch labels for related entities
            return fetchEntityLabels(relatedIds);

        } catch (InterruptedException e) {
            log.error("Wikidata related topics fetch interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    /**
     * Validate that a topic exists in Wikidata.
     * Returns confidence based on match quality.
     *
     * For educational/guide topics (multi-word), we're more lenient -
     * if significant concept words match Wikidata entities, we consider
     * the topic valid since it's about real concepts.
     */
    @Override
    public double validateTopic(String topic) {
        if (!enabled || topic == null || topic.isBlank()) {
            return 0.0;
        }

        List<SearchResult> results = search(topic);

        String normalizedTopic = topic.toLowerCase().trim();
        Set<String> topicWords = new HashSet<>(Arrays.asList(normalizedTopic.split("\\s+")));

        // Remove common filler words for better matching
        Set<String> fillerWords = Set.of("the", "a", "an", "for", "with", "in", "of", "to", "and", "or", "on", "by");
        Set<String> significantWords = new HashSet<>();
        for (String word : topicWords) {
            if (word.length() > 2 && !fillerWords.contains(word)) {
                significantWords.add(word);
            }
        }

        if (!results.isEmpty()) {
            SearchResult best = results.get(0);
            String normalizedLabel = best.title().toLowerCase().trim();

            // Exact match
            if (normalizedLabel.equals(normalizedTopic)) {
                return 1.0;
            }

            // Label contains query or vice versa
            if (normalizedLabel.contains(normalizedTopic) || normalizedTopic.contains(normalizedLabel)) {
                return 0.85;
            }

            // Check word overlap
            Set<String> labelWords = new HashSet<>(Arrays.asList(normalizedLabel.split("\\s+")));
            long commonWords = significantWords.stream().filter(labelWords::contains).count();

            if (commonWords > 0) {
                // For multi-word topics, finding ANY significant match is good
                // e.g., "Voice Assistant Development" matching "voice assistant"
                if (significantWords.size() > 2 && commonWords >= 1) {
                    return 0.6 + (0.2 * commonWords / significantWords.size());
                }
                double overlap = (double) commonWords / significantWords.size();
                return 0.5 + (overlap * 0.35); // 0.5 to 0.85 based on overlap
            }

            // Found something related (Wikidata returned a result for our query)
            // This means SOME concept was found, even if label doesn't match well
            return 0.45;
        }

        // No direct results - try searching for individual significant words
        // This helps validate composite topics like "LLM Integration with IoT Sensors"
        int foundCount = 0;
        for (String word : significantWords) {
            if (word.length() >= 4) { // Only check substantial words
                List<SearchResult> wordResults = search(word);
                if (!wordResults.isEmpty()) {
                    foundCount++;
                }
            }
        }

        if (foundCount > 0) {
            // Some component concepts exist in Wikidata
            double ratio = (double) foundCount / significantWords.size();
            return 0.35 + (ratio * 0.25); // 0.35 to 0.6 based on how many words found
        }

        // Nothing found
        return 0.0;
    }

    /**
     * Find the Wikidata entity ID for a topic.
     */
    private String findEntityId(String topic) throws InterruptedException {
        String encodedQuery = URLEncoder.encode(topic, StandardCharsets.UTF_8);
        String url = WIKIDATA_API + "?action=wbsearchentities" +
                "&search=" + encodedQuery +
                "&language=en" +
                "&limit=1" +
                "&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher)")
                .GET()
                .build();

        String responseBody = executeWithRetry(request);
        if (responseBody == null) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode searchResults = root.path("search");
            if (searchResults.isArray() && searchResults.size() > 0) {
                return searchResults.get(0).path("id").asText(null);
            }
        } catch (Exception e) {
            log.error("Failed to parse entity ID: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Parse related entity IDs from claims response.
     */
    private Set<String> parseRelatedEntityIds(String json, String entityId) {
        Set<String> relatedIds = new LinkedHashSet<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode claims = root.path("entities").path(entityId).path("claims");

            for (String property : RELATED_PROPERTIES) {
                JsonNode propertyClaims = claims.path(property);
                if (propertyClaims.isArray()) {
                    for (JsonNode claim : propertyClaims) {
                        JsonNode mainsnak = claim.path("mainsnak");
                        if ("wikibase-entityid".equals(mainsnak.path("datatype").asText())) {
                            String relatedId = mainsnak.path("datavalue").path("value").path("id").asText();
                            if (relatedId != null && !relatedId.isEmpty() && !relatedId.equals(entityId)) {
                                relatedIds.add(relatedId);
                                if (relatedIds.size() >= maxResults) {
                                    break;
                                }
                            }
                        }
                    }
                }
                if (relatedIds.size() >= maxResults) {
                    break;
                }
            }

            log.debug("Found {} related entities for {}", relatedIds.size(), entityId);
        } catch (Exception e) {
            log.error("Failed to parse related entities: {}", e.getMessage());
        }

        return relatedIds;
    }

    /**
     * Fetch English labels for a set of entity IDs.
     */
    private List<String> fetchEntityLabels(Set<String> entityIds) throws InterruptedException {
        if (entityIds.isEmpty()) {
            return List.of();
        }

        String ids = String.join("|", entityIds);
        String url = WIKIDATA_API + "?action=wbgetentities" +
                "&ids=" + ids +
                "&props=labels" +
                "&languages=en" +
                "&format=json";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_TIMEOUT)
                .header("User-Agent", "AIPublisher/1.0 (https://github.com/jakefear/aipublisher)")
                .GET()
                .build();

        String responseBody = executeWithRetry(request);
        if (responseBody == null) {
            return List.of();
        }

        List<String> labels = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode entities = root.path("entities");

            for (String entityIdItem : entityIds) {
                String label = entities.path(entityIdItem).path("labels").path("en").path("value").asText();
                if (label != null && !label.isEmpty()) {
                    labels.add(label);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse entity labels: {}", e.getMessage());
        }

        return labels;
    }

    /**
     * Parse search results from wbsearchentities response.
     */
    List<SearchResult> parseSearchResults(String json) {
        List<SearchResult> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode searchResults = root.path("search");

            if (searchResults.isArray()) {
                for (JsonNode result : searchResults) {
                    String label = result.path("label").asText("");
                    String description = result.path("description").asText("");
                    String entityId = result.path("id").asText("");

                    // Build URL - prefer Wikipedia link, fall back to Wikidata
                    String url = "https://www.wikidata.org/wiki/" + entityId;

                    if (!label.isEmpty()) {
                        results.add(new SearchResult(
                                label,
                                url,
                                description,
                                SourceReliability.REPUTABLE
                        ));
                    }
                }
            }

            log.debug("Parsed {} Wikidata search results", results.size());
        } catch (Exception e) {
            log.error("Failed to parse Wikidata search results: {}", e.getMessage());
        }

        return results;
    }

    /**
     * Check if the result label is a similar match to the query.
     */
    private boolean isSimilarMatch(String query, String label) {
        String normalizedQuery = query.toLowerCase().trim();
        String normalizedLabel = label.toLowerCase().trim();

        // Exact match
        if (normalizedQuery.equals(normalizedLabel)) {
            return true;
        }

        // One contains the other
        if (normalizedQuery.contains(normalizedLabel) || normalizedLabel.contains(normalizedQuery)) {
            return true;
        }

        // Significant word overlap
        Set<String> queryWords = new HashSet<>(Arrays.asList(normalizedQuery.split("\\s+")));
        Set<String> labelWords = new HashSet<>(Arrays.asList(normalizedLabel.split("\\s+")));
        long commonWords = queryWords.stream().filter(labelWords::contains).count();

        return commonWords >= Math.min(queryWords.size(), labelWords.size()) * 0.5;
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
                        log.info("Wikidata request returned status {}, retrying in {}ms (attempt {}/{})",
                                statusCode, backoffMs, attempt, MAX_RETRIES);
                        Thread.sleep(backoffMs);
                        backoffMs *= 2;
                    } else {
                        log.warn("Wikidata request failed with status {} after {} attempts", statusCode, MAX_RETRIES);
                        return null;
                    }
                } else {
                    log.warn("Wikidata request failed with non-retryable status {}", statusCode);
                    return null;
                }
            } catch (IOException e) {
                attempt++;
                if (attempt < MAX_RETRIES) {
                    log.info("Wikidata request failed with {}, retrying in {}ms (attempt {}/{})",
                            e.getMessage(), backoffMs, attempt, MAX_RETRIES);
                    Thread.sleep(backoffMs);
                    backoffMs *= 2;
                } else {
                    log.error("Wikidata request failed after {} attempts: {}", MAX_RETRIES, e.getMessage());
                    return null;
                }
            }
        }

        return null;
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode == 503 || statusCode == 504;
    }
}
