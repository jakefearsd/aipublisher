package com.jakefear.aipublisher.search;

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
@DisplayName("WebSearchService")
class WebSearchServiceTest {

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private HttpResponse<String> mockResponse;

    private WebSearchService service;

    @BeforeEach
    void setUp() {
        service = new WebSearchService(mockHttpClient, 5, true);
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
            WebSearchService disabled = new WebSearchService(mockHttpClient, 5, false);
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
            WebSearchService disabled = new WebSearchService(mockHttpClient, 5, false);
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
    }

    @Nested
    @DisplayName("HTML Parsing")
    class HtmlParsing {

        @Test
        @DisplayName("Parses DuckDuckGo result format")
        void parsesDuckDuckGoResults() {
            String html = """
                    <div class="result">
                        <a class="result__a" href="https://docs.example.com/guide">Example Documentation</a>
                        <a class="result__snippet">This is a helpful guide about the topic.</a>
                    </div>
                    <div class="result">
                        <a class="result__a" href="https://github.com/example/repo">Example Repository</a>
                        <a class="result__snippet">Source code for the example project.</a>
                    </div>
                    """;

            List<SearchResult> results = service.parseSearchResults(html);

            assertEquals(2, results.size());
            assertEquals("Example Documentation", results.get(0).title());
            assertEquals("https://docs.example.com/guide", results.get(0).url());
            assertEquals("This is a helpful guide about the topic.", results.get(0).snippet());
        }

        @Test
        @DisplayName("Skips DuckDuckGo internal links")
        void skipsDuckDuckGoInternalLinks() {
            String html = """
                    <div class="result">
                        <a class="result__a" href="//duckduckgo.com/something">DDG Internal</a>
                        <a class="result__snippet">Internal link</a>
                    </div>
                    <div class="result">
                        <a class="result__a" href="https://example.com/page">Real Result</a>
                        <a class="result__snippet">Actual content</a>
                    </div>
                    """;

            List<SearchResult> results = service.parseSearchResults(html);

            assertEquals(1, results.size());
            assertEquals("Real Result", results.get(0).title());
        }

        @Test
        @DisplayName("Limits results to maxResults")
        void limitsResultCount() {
            WebSearchService limited = new WebSearchService(mockHttpClient, 2, true);
            String html = """
                    <div class="result">
                        <a class="result__a" href="https://example1.com">Result 1</a>
                        <a class="result__snippet">Snippet 1</a>
                    </div>
                    <div class="result">
                        <a class="result__a" href="https://example2.com">Result 2</a>
                        <a class="result__snippet">Snippet 2</a>
                    </div>
                    <div class="result">
                        <a class="result__a" href="https://example3.com">Result 3</a>
                        <a class="result__snippet">Snippet 3</a>
                    </div>
                    """;

            List<SearchResult> results = limited.parseSearchResults(html);

            assertEquals(2, results.size());
        }

        @Test
        @DisplayName("Cleans HTML entities from results")
        void cleansHtmlEntities() {
            String html = """
                    <div class="result">
                        <a class="result__a" href="https://example.com">Title &amp; More</a>
                        <a class="result__snippet">Content &lt;with&gt; entities &quot;here&quot;</a>
                    </div>
                    """;

            List<SearchResult> results = service.parseSearchResults(html);

            assertEquals(1, results.size());
            assertEquals("Title & More", results.get(0).title());
            assertEquals("Content <with> entities \"here\"", results.get(0).snippet());
        }

        @Test
        @DisplayName("Returns empty list for empty HTML")
        void returnsEmptyForEmptyHtml() {
            List<SearchResult> results = service.parseSearchResults("");
            assertTrue(results.isEmpty());
        }
    }

    @Nested
    @DisplayName("Source Reliability Assessment")
    class ReliabilityAssessment {

        @Test
        @DisplayName("Classifies official documentation sites")
        void classifiesOfficialDocs() {
            assertEquals(SourceReliability.OFFICIAL, service.assessReliability("https://docs.oracle.com/java"));
            assertEquals(SourceReliability.OFFICIAL, service.assessReliability("https://developer.mozilla.org"));
            assertEquals(SourceReliability.OFFICIAL, service.assessReliability("https://kafka.apache.org/documentation"));
        }

        @Test
        @DisplayName("Classifies academic sources")
        void classifiesAcademicSources() {
            assertEquals(SourceReliability.ACADEMIC, service.assessReliability("https://arxiv.org/paper"));
            assertEquals(SourceReliability.ACADEMIC, service.assessReliability("https://dl.acm.org/journal"));
            assertEquals(SourceReliability.ACADEMIC, service.assessReliability("https://ieee.org/standard"));
            assertEquals(SourceReliability.ACADEMIC, service.assessReliability("https://stanford.edu/research"));
        }

        @Test
        @DisplayName("Classifies authoritative sources")
        void classifiesAuthoritativeSources() {
            assertEquals(SourceReliability.AUTHORITATIVE, service.assessReliability("https://oreilly.com/book"));
            assertEquals(SourceReliability.AUTHORITATIVE, service.assessReliability("https://manning.com/book"));
            assertEquals(SourceReliability.AUTHORITATIVE, service.assessReliability("https://martinfowler.com/articles"));
            assertEquals(SourceReliability.AUTHORITATIVE, service.assessReliability("https://infoq.com/articles"));
        }

        @Test
        @DisplayName("Classifies reputable community sources")
        void classifiesReputableSources() {
            assertEquals(SourceReliability.REPUTABLE, service.assessReliability("https://github.com/project"));
            assertEquals(SourceReliability.REPUTABLE, service.assessReliability("https://stackoverflow.com/questions"));
            assertEquals(SourceReliability.REPUTABLE, service.assessReliability("https://medium.com/article"));
            assertEquals(SourceReliability.REPUTABLE, service.assessReliability("https://dev.to/post"));
            assertEquals(SourceReliability.REPUTABLE, service.assessReliability("https://en.wikipedia.org/wiki"));
        }

        @Test
        @DisplayName("Classifies community sources")
        void classifiesCommunitySources() {
            assertEquals(SourceReliability.COMMUNITY, service.assessReliability("https://reddit.com/r/programming"));
            assertEquals(SourceReliability.COMMUNITY, service.assessReliability("https://quora.com/question"));
            assertEquals(SourceReliability.COMMUNITY, service.assessReliability("https://example.com/forum"));
            assertEquals(SourceReliability.COMMUNITY, service.assessReliability("https://community.example.com"));
        }

        @Test
        @DisplayName("Returns UNCERTAIN for unknown sources")
        void returnsUncertainForUnknown() {
            assertEquals(SourceReliability.UNCERTAIN, service.assessReliability("https://random-blog.xyz/post"));
            assertEquals(SourceReliability.UNCERTAIN, service.assessReliability("https://unknown-site.com"));
        }
    }

    @Nested
    @DisplayName("Specialized Search Methods")
    class SpecializedSearchMethods {

        @Test
        @DisplayName("searchForVerification adds verification keywords")
        void searchForVerificationAddsKeywords() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("<html></html>");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.searchForVerification("Climate change is real");

            verify(mockHttpClient).send(argThat(request -> {
                String uri = request.uri().toString();
                return uri.contains("fact") && uri.contains("check") && uri.contains("verify");
            }), any(HttpResponse.BodyHandler.class));
        }

        @Test
        @DisplayName("searchOfficialDocs adds documentation keywords")
        void searchOfficialDocsAddsKeywords() throws Exception {
            when(mockResponse.statusCode()).thenReturn(200);
            when(mockResponse.body()).thenReturn("<html></html>");
            when(mockHttpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenReturn(mockResponse);

            service.searchOfficialDocs("Apache Kafka");

            verify(mockHttpClient).send(argThat(request -> {
                String uri = request.uri().toString();
                return uri.contains("official") && uri.contains("documentation");
            }), any(HttpResponse.BodyHandler.class));
        }
    }
}
