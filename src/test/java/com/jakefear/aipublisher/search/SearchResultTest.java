package com.jakefear.aipublisher.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SearchResult")
class SearchResultTest {

    @Nested
    @DisplayName("toPromptFormat")
    class ToPromptFormat {

        @Test
        @DisplayName("Formats result with all fields")
        void formatsWithAllFields() {
            SearchResult result = new SearchResult(
                    "Example Title",
                    "https://example.com/page",
                    "This is the snippet text",
                    SourceReliability.OFFICIAL
            );

            String formatted = result.toPromptFormat();

            assertTrue(formatted.contains("**Example Title**"));
            assertTrue(formatted.contains("URL: https://example.com/page"));
            assertTrue(formatted.contains("Reliability: OFFICIAL"));
            assertTrue(formatted.contains("Excerpt: This is the snippet text"));
        }

        @Test
        @DisplayName("Omits excerpt when snippet is null")
        void omitsExcerptWhenNull() {
            SearchResult result = new SearchResult(
                    "Title",
                    "https://example.com",
                    null,
                    SourceReliability.REPUTABLE
            );

            String formatted = result.toPromptFormat();

            assertFalse(formatted.contains("Excerpt:"));
        }

        @Test
        @DisplayName("Omits excerpt when snippet is blank")
        void omitsExcerptWhenBlank() {
            SearchResult result = new SearchResult(
                    "Title",
                    "https://example.com",
                    "   ",
                    SourceReliability.REPUTABLE
            );

            String formatted = result.toPromptFormat();

            assertFalse(formatted.contains("Excerpt:"));
        }
    }

    @Nested
    @DisplayName("toCitation")
    class ToCitation {

        @Test
        @DisplayName("Formats citation with link and reliability indicator")
        void formatsCitationWithLinkAndReliability() {
            SearchResult result = new SearchResult(
                    "Apache Kafka Docs",
                    "https://kafka.apache.org/documentation",
                    "Documentation",
                    SourceReliability.OFFICIAL
            );

            String citation = result.toCitation();

            assertEquals("[Apache Kafka Docs](https://kafka.apache.org/documentation) (OFFICIAL)", citation);
        }
    }
}
