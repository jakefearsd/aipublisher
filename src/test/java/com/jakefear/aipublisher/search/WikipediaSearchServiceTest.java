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
@DisplayName("WikipediaSearchService")
class WikipediaSearchServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private ObjectMapper objectMapper;
    private WikipediaSearchService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new WikipediaSearchService(mockHttpClient, objectMapper, 5, true);
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
            WikipediaSearchService disabled = new WikipediaSearchService(mockHttpClient, objectMapper, 5, false);
            assertFalse(disabled.isEnabled());
        }

        @Test
        @DisplayName("getMaxResults returns configured value")
        void getMaxResults() {
            assertEquals(5, service.getMaxResults());
        }
    }

    @Nested
    @DisplayName("Search Behavior")
    class SearchBehavior {

        @Test
        @DisplayName("Returns empty list when disabled")
        void returnsEmptyWhenDisabled() {
            WikipediaSearchService disabled = new WikipediaSearchService(mockHttpClient, objectMapper, 5, false);
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
        @DisplayName("Calls Wikipedia API endpoint")
        void callsWikipediaApiEndpoint() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"query\":{\"search\":[]}}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.search("compound interest");

            verify(mockHttpClient).send(argThat(request -> {
                String uri = request.uri().toString();
                return uri.contains("en.wikipedia.org") && uri.contains("action=query");
            }), any(HttpResponse.BodyHandler.class));
        }
    }

    @Nested
    @DisplayName("JSON Parsing")
    class JsonParsing {

        @Test
        @DisplayName("Parses Wikipedia search results")
        void parsesWikipediaSearchResults() {
            String json = """
                    {
                      "query": {
                        "search": [
                          {
                            "title": "Compound interest",
                            "snippet": "Compound interest is the addition of interest to the principal sum..."
                          },
                          {
                            "title": "Interest rate",
                            "snippet": "An interest rate is the amount of interest due per period..."
                          }
                        ]
                      }
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(2, results.size());
            assertEquals("Compound interest", results.get(0).title());
            assertTrue(results.get(0).url().contains("wikipedia.org"));
            assertTrue(results.get(0).snippet().contains("addition of interest"));
            assertEquals(SourceReliability.REPUTABLE, results.get(0).reliability());
        }

        @Test
        @DisplayName("Returns empty list for empty search results")
        void returnsEmptyForEmptyResults() {
            String json = """
                    {
                      "query": {
                        "search": []
                      }
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
        @DisplayName("Cleans HTML from snippets")
        void cleansHtmlFromSnippets() {
            String json = """
                    {
                      "query": {
                        "search": [
                          {
                            "title": "Test Article",
                            "snippet": "This has <span class=\\"searchmatch\\">highlighted</span> text &amp; entities"
                          }
                        ]
                      }
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(1, results.size());
            assertEquals("This has highlighted text & entities", results.get(0).snippet());
        }

        @Test
        @DisplayName("Constructs proper Wikipedia URLs")
        void constructsProperWikipediaUrls() {
            String json = """
                    {
                      "query": {
                        "search": [
                          {
                            "title": "Test Article",
                            "snippet": "Some content"
                          }
                        ]
                      }
                    }
                    """;

            List<SearchResult> results = service.parseSearchResults(json);

            assertEquals(1, results.size());
            assertTrue(results.get(0).url().startsWith("https://en.wikipedia.org/wiki/"));
            assertTrue(results.get(0).url().contains("Test"));
        }
    }

    @Nested
    @DisplayName("Article Summary")
    class ArticleSummary {

        @Test
        @DisplayName("Parses article summary from REST API")
        void parsesArticleSummary() {
            String json = """
                    {
                      "title": "Compound interest",
                      "extract": "Compound interest is the addition of interest to the principal sum of a loan or deposit.",
                      "content_urls": {
                        "desktop": {
                          "page": "https://en.wikipedia.org/wiki/Compound_interest"
                        }
                      }
                    }
                    """;

            SearchResult result = service.parseArticleSummary(json);

            assertNotNull(result);
            assertEquals("Compound interest", result.title());
            assertEquals("https://en.wikipedia.org/wiki/Compound_interest", result.url());
            assertTrue(result.snippet().contains("addition of interest"));
            assertEquals(SourceReliability.REPUTABLE, result.reliability());
        }

        @Test
        @DisplayName("Returns null for missing title")
        void returnsNullForMissingTitle() {
            String json = """
                    {
                      "extract": "Some content",
                      "content_urls": {
                        "desktop": {
                          "page": "https://en.wikipedia.org/wiki/Test"
                        }
                      }
                    }
                    """;

            SearchResult result = service.parseArticleSummary(json);
            assertNull(result);
        }

        @Test
        @DisplayName("Returns null for invalid JSON")
        void returnsNullForInvalidJson() {
            SearchResult result = service.parseArticleSummary("not valid json");
            assertNull(result);
        }

        @Test
        @DisplayName("Returns null when disabled")
        void returnsNullWhenDisabled() {
            WikipediaSearchService disabled = new WikipediaSearchService(mockHttpClient, objectMapper, 5, false);
            SearchResult result = disabled.getArticleSummary("Test");
            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Related Pages")
    class RelatedPages {

        @Test
        @DisplayName("Parses related pages from links API")
        void parsesRelatedPages() {
            String json = """
                    {
                      "query": {
                        "pages": {
                          "12345": {
                            "links": [
                              {"title": "Interest rate"},
                              {"title": "Principal"},
                              {"title": "Loan"}
                            ]
                          }
                        }
                      }
                    }
                    """;

            List<String> pages = service.parseRelatedPages(json);

            assertEquals(3, pages.size());
            assertTrue(pages.contains("Interest rate"));
            assertTrue(pages.contains("Principal"));
            assertTrue(pages.contains("Loan"));
        }

        @Test
        @DisplayName("Filters out Wikipedia internal pages")
        void filtersInternalPages() {
            String json = """
                    {
                      "query": {
                        "pages": {
                          "12345": {
                            "links": [
                              {"title": "Interest rate"},
                              {"title": "Wikipedia:Guidelines"},
                              {"title": "Help:Contents"},
                              {"title": "Category:Economics"},
                              {"title": "Loan"}
                            ]
                          }
                        }
                      }
                    }
                    """;

            List<String> pages = service.parseRelatedPages(json);

            assertEquals(2, pages.size());
            assertTrue(pages.contains("Interest rate"));
            assertTrue(pages.contains("Loan"));
            assertFalse(pages.stream().anyMatch(p -> p.startsWith("Wikipedia:")));
            assertFalse(pages.stream().anyMatch(p -> p.startsWith("Help:")));
            assertFalse(pages.stream().anyMatch(p -> p.startsWith("Category:")));
        }

        @Test
        @DisplayName("Returns empty list for invalid JSON")
        void returnsEmptyForInvalidJson() {
            List<String> pages = service.parseRelatedPages("not valid json");
            assertTrue(pages.isEmpty());
        }

        @Test
        @DisplayName("Returns empty list when disabled")
        void returnsEmptyWhenDisabled() {
            WikipediaSearchService disabled = new WikipediaSearchService(mockHttpClient, objectMapper, 5, false);
            List<String> pages = disabled.getRelatedPages("Test");
            assertTrue(pages.isEmpty());
        }
    }

    @Nested
    @DisplayName("Specialized Search Methods")
    class SpecializedSearchMethods {

        @Test
        @DisplayName("searchForVerification delegates to search")
        void searchForVerificationDelegatesToSearch() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"query\":{\"search\":[]}}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.searchForVerification("test claim");

            verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("searchOfficialDocs delegates to search")
        void searchOfficialDocsDelegatesToSearch() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("{\"query\":{\"search\":[]}}");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.searchOfficialDocs("test topic");

            verify(mockHttpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
        }
    }
}
