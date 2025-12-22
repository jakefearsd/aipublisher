package com.jakefear.aipublisher.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WikidataSearchService")
class WikidataSearchServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private ObjectMapper objectMapper;
    private WikidataSearchService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new WikidataSearchService(mockHttpClient, objectMapper, 5, true);
    }

    @Nested
    @DisplayName("Basic Properties")
    class BasicProperties {

        @Test
        @DisplayName("isEnabled returns true when enabled")
        void isEnabledWhenEnabled() {
            assertTrue(service.isEnabled());
        }

        @Test
        @DisplayName("isEnabled returns false when disabled")
        void isEnabledWhenDisabled() {
            WikidataSearchService disabled = new WikidataSearchService(mockHttpClient, objectMapper, 5, false);
            assertFalse(disabled.isEnabled());
        }

        @Test
        @DisplayName("getMaxResults returns configured value")
        void getMaxResults() {
            assertEquals(5, service.getMaxResults());
        }

        @Test
        @DisplayName("getProviderName returns wikidata")
        void getProviderName() {
            assertEquals("wikidata", service.getProviderName());
        }
    }

    @Nested
    @DisplayName("Search Behavior")
    class SearchBehavior {

        @Test
        @DisplayName("Returns empty list when disabled")
        void returnsEmptyWhenDisabled() {
            WikidataSearchService disabled = new WikidataSearchService(mockHttpClient, objectMapper, 5, false);
            List<SearchResult> results = disabled.search("test query");
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for null query")
        void returnsEmptyForNullQuery() {
            List<SearchResult> results = service.search(null);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for blank query")
        void returnsEmptyForBlankQuery() {
            List<SearchResult> results = service.search("   ");
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list on HTTP error")
        void returnsEmptyOnHttpError() throws Exception {
            when(mockResponse.statusCode()).thenReturn(500);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            List<SearchResult> results = service.search("test query");
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list on IOException")
        void returnsEmptyOnIOException() throws Exception {
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Network error"));

            List<SearchResult> results = service.search("test query");
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Calls Wikidata API endpoint")
        void callsWikidataApiEndpoint() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.search("compound interest");

            verify(mockHttpClient).send(argThat(request -> {
                String uri = request.uri().toString();
                return uri.contains("wikidata.org") && uri.contains("wbsearchentities");
            }), any(HttpResponse.BodyHandler.class));
        }
    }

    @Nested
    @DisplayName("JSON Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Parses Wikidata search results")
        void parsesWikidataSearchResults() {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q28787",
                          "label": "Compound interest",
                          "description": "interest calculated on principal and accumulated interest"
                        },
                        {
                          "id": "Q179179",
                          "label": "Interest rate",
                          "description": "percentage of amount lent charged by lender"
                        }
                      ]
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(2, results.size());
            assertEquals("Compound interest", results.get(0).title());
            assertTrue(results.get(0).url().contains("wikidata.org"));
            assertTrue(results.get(0).snippet().contains("interest calculated"));
            assertEquals(SourceReliability.REPUTABLE, results.get(0).reliability());
        }

        @Test
        @DisplayName("Returns empty list for empty search results")
        void returnsEmptyForEmptyResults() {
            String json = """
                    {
                      "search": []
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for invalid JSON")
        void returnsEmptyForInvalidJson() {
            String json = "not valid json";
            List<SearchResult> results = service.parseSearchResults(json);
            assertTrue(results.isEmpty());
        }

        @Test
        @DisplayName("Constructs proper Wikidata URLs")
        void constructsProperWikidataUrls() {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q12345",
                          "label": "Test Entity",
                          "description": "A test entity"
                        }
                      ]
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(1, results.size());
            assertEquals("https://www.wikidata.org/wiki/Q12345", results.get(0).url());
        }

        @Test
        @DisplayName("Handles missing description gracefully")
        void handlesMissingDescription() {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q12345",
                          "label": "Test Entity"
                        }
                      ]
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(1, results.size());
            assertEquals("Test Entity", results.get(0).title());
            assertEquals("", results.get(0).snippet());
        }
    }

    @Nested
    @DisplayName("Topic Summary")
    class TopicSummary {

        @Test
        @DisplayName("Returns first search result as summary")
        void returnsFirstResultAsSummary() throws Exception {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q28787",
                          "label": "Compound interest",
                          "description": "interest calculated on principal and accumulated interest"
                        }
                      ]
                    }
                    """;

            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(json);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            SearchResult result = service.getTopicSummary("compound interest");

            assertNotNull(result);
            assertEquals("Compound interest", result.title());
            assertTrue(result.snippet().contains("interest calculated"));
            assertEquals(SourceReliability.REPUTABLE, result.reliability());
        }

        @Test
        @DisplayName("Returns null for no results")
        void returnsNullForNoResults() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            SearchResult result = service.getTopicSummary("nonexistent topic xyz");
            assertNull(result);
        }

        @Test
        @DisplayName("Returns null when disabled")
        void returnsNullWhenDisabled() {
            WikidataSearchService disabled = new WikidataSearchService(mockHttpClient, objectMapper, 5, false);
            SearchResult result = disabled.getTopicSummary("Test");
            assertNull(result);
        }

        @Test
        @DisplayName("Returns null for null topic")
        void returnsNullForNullTopic() {
            SearchResult result = service.getTopicSummary(null);
            assertNull(result);
        }

        @Test
        @DisplayName("Returns null for blank topic")
        void returnsNullForBlankTopic() {
            SearchResult result = service.getTopicSummary("   ");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Related Topics")
    class RelatedTopics {

        @Test
        @DisplayName("Returns empty list when disabled")
        void returnsEmptyWhenDisabled() {
            WikidataSearchService disabled = new WikidataSearchService(mockHttpClient, objectMapper, 5, false);
            List<String> topics = disabled.getRelatedTopics("Test");
            assertTrue(topics.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for null topic")
        void returnsEmptyForNullTopic() {
            List<String> topics = service.getRelatedTopics(null);
            assertTrue(topics.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list for blank topic")
        void returnsEmptyForBlankTopic() {
            List<String> topics = service.getRelatedTopics("   ");
            assertTrue(topics.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when entity not found")
        void returnsEmptyWhenEntityNotFound() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            List<String> topics = service.getRelatedTopics("nonexistent");
            assertTrue(topics.isEmpty());
        }
    }

    @Nested
    @DisplayName("Validate Topic")
    class ValidateTopic {

        @Test
        @DisplayName("Returns 0.0 when disabled")
        void returnsZeroWhenDisabled() {
            WikidataSearchService disabled = new WikidataSearchService(mockHttpClient, objectMapper, 5, false);
            double confidence = disabled.validateTopic("Test");
            assertEquals(0.0, confidence);
        }

        @Test
        @DisplayName("Returns 0.0 for null topic")
        void returnsZeroForNullTopic() {
            double confidence = service.validateTopic(null);
            assertEquals(0.0, confidence);
        }

        @Test
        @DisplayName("Returns 0.0 for blank topic")
        void returnsZeroForBlankTopic() {
            double confidence = service.validateTopic("   ");
            assertEquals(0.0, confidence);
        }

        @Test
        @DisplayName("Returns 1.0 for exact match")
        void returnsOneForExactMatch() throws Exception {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q12345",
                          "label": "Machine Learning",
                          "description": "branch of artificial intelligence"
                        }
                      ]
                    }
                    """;

            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(json);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            double confidence = service.validateTopic("Machine Learning");
            assertEquals(1.0, confidence);
        }

        @Test
        @DisplayName("Returns high confidence for contained match")
        void returnsHighConfidenceForContainedMatch() throws Exception {
            String json = """
                    {
                      "search": [
                        {
                          "id": "Q12345",
                          "label": "Machine Learning",
                          "description": "branch of artificial intelligence"
                        }
                      ]
                    }
                    """;

            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn(json);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            double confidence = service.validateTopic("Machine Learning Basics");
            assertEquals(0.85, confidence);
        }

        @Test
        @DisplayName("Returns 0.0 for no results")
        void returnsZeroForNoResults() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            double confidence = service.validateTopic("xyznonexistent123");
            assertEquals(0.0, confidence);
        }
    }

    @Nested
    @DisplayName("Specialized Search Methods")
    class SpecializedSearchMethods {

        @Test
        @DisplayName("searchForVerification delegates to search")
        void searchForVerificationDelegatesToSearch() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.searchForVerification("test claim");

            verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }
    }

    @Nested
    @DisplayName("Retry Behavior")
    class RetryBehavior {

        @Test
        @DisplayName("Retries on 429 status")
        void retriesOn429Status() throws Exception {
            when(mockResponse.statusCode())
                    .thenReturn(429)
                    .thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.search("test");

            verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("Retries on 503 status")
        void retriesOn503Status() throws Exception {
            when(mockResponse.statusCode())
                    .thenReturn(503)
                    .thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.search("test");

            verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("Does not retry on 404 status")
        void doesNotRetryOn404Status() throws Exception {
            when(mockResponse.statusCode()).thenReturn(404);
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.search("test");

            verify(mockHttpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("Retries on IOException")
        void retriesOnIOException() throws Exception {
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection reset"))
                    .thenReturn(mockResponse);
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"search\":[]}");

            service.search("test");

            verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("Gives up after max retries")
        void givesUpAfterMaxRetries() throws Exception {
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenThrow(new IOException("Connection reset"));

            List<SearchResult> results = service.search("test");

            assertTrue(results.isEmpty());
            verify(mockHttpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }
    }
}
