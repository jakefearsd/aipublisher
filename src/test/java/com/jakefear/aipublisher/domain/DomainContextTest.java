package com.jakefear.aipublisher.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DomainContext")
class DomainContextTest {

    @Nested
    @DisplayName("Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("Creates empty context with defaults")
        void createsEmptyContext() {
            DomainContext context = DomainContext.empty();

            assertEquals("", context.domainSummary());
            assertTrue(context.keyThemes().isEmpty());
            assertTrue(context.glossary().isEmpty());
            assertTrue(context.authorityUrls().isEmpty());
            assertTrue(context.topicClusters().isEmpty());
            assertEquals("", context.writingGuidelines());
            assertNotNull(context.lastUpdated());
        }

        @Test
        @DisplayName("Builder creates context with values")
        void builderCreatesContext() {
            Instant now = Instant.now();
            DomainContext context = DomainContext.builder()
                    .domainSummary("A summary of investing")
                    .addKeyTheme("Risk management")
                    .addKeyTheme("Compound interest")
                    .addGlossaryTerm("ROI", "Return on Investment")
                    .addGlossaryTerm("ETF", "Exchange Traded Fund")
                    .addAuthorityUrl("https://investopedia.com")
                    .addTopicCluster("Basics", List.of("Stocks", "Bonds", "ETFs"))
                    .writingGuidelines("Use simple language")
                    .lastUpdated(now)
                    .build();

            assertEquals("A summary of investing", context.domainSummary());
            assertEquals(2, context.keyThemes().size());
            assertTrue(context.keyThemes().contains("Risk management"));
            assertEquals("Return on Investment", context.glossary().get("ROI"));
            assertEquals(1, context.authorityUrls().size());
            assertEquals(3, context.topicClusters().get("Basics").size());
            assertEquals("Use simple language", context.writingGuidelines());
            assertEquals(now, context.lastUpdated());
        }
    }

    @Nested
    @DisplayName("Normalization")
    class Normalization {

        @Test
        @DisplayName("Applies defaults for null fields")
        void appliesDefaultsForNullFields() {
            DomainContext context = new DomainContext(
                    null, null, null, null, null, null, null
            );

            assertEquals("", context.domainSummary());
            assertTrue(context.keyThemes().isEmpty());
            assertTrue(context.glossary().isEmpty());
            assertTrue(context.authorityUrls().isEmpty());
            assertTrue(context.topicClusters().isEmpty());
            assertEquals("", context.writingGuidelines());
            assertNotNull(context.lastUpdated());
        }

        @Test
        @DisplayName("Creates immutable copies of collections")
        void createsImmutableCopies() {
            DomainContext context = DomainContext.builder()
                    .keyThemes(List.of("Theme1"))
                    .glossary(Map.of("term", "def"))
                    .authorityUrls(List.of("url1"))
                    .topicClusters(Map.of("cluster", List.of("topic")))
                    .build();

            assertThrows(UnsupportedOperationException.class, () ->
                    context.keyThemes().add("Theme2"));
            assertThrows(UnsupportedOperationException.class, () ->
                    context.glossary().put("term2", "def2"));
        }
    }

    @Nested
    @DisplayName("Query Methods")
    class QueryMethods {

        @Test
        @DisplayName("Reports hasContent correctly")
        void reportsHasContentCorrectly() {
            DomainContext empty = DomainContext.empty();
            assertFalse(empty.hasContent());

            DomainContext withSummary = DomainContext.builder()
                    .domainSummary("Something")
                    .build();
            assertTrue(withSummary.hasContent());

            DomainContext withThemes = DomainContext.builder()
                    .addKeyTheme("Theme")
                    .build();
            assertTrue(withThemes.hasContent());

            DomainContext withGlossary = DomainContext.builder()
                    .addGlossaryTerm("term", "def")
                    .build();
            assertTrue(withGlossary.hasContent());
        }
    }

    @Nested
    @DisplayName("toPromptFormat")
    class ToPromptFormat {

        @Test
        @DisplayName("Formats empty context as empty string")
        void formatsEmptyContextAsEmpty() {
            DomainContext context = DomainContext.empty();
            assertEquals("", context.toPromptFormat());
        }

        @Test
        @DisplayName("Formats full context with all sections")
        void formatsFullContext() {
            DomainContext context = DomainContext.builder()
                    .domainSummary("Investing is about growing wealth")
                    .addKeyTheme("Compound interest")
                    .addKeyTheme("Risk management")
                    .addGlossaryTerm("ROI", "Return on Investment")
                    .writingGuidelines("Be clear and concise")
                    .build();

            String formatted = context.toPromptFormat();

            assertTrue(formatted.contains("=== DOMAIN BACKGROUND ==="));
            assertTrue(formatted.contains("Investing is about growing wealth"));
            assertTrue(formatted.contains("=== KEY THEMES (maintain throughout) ==="));
            assertTrue(formatted.contains("- Compound interest"));
            assertTrue(formatted.contains("=== DOMAIN TERMINOLOGY ==="));
            assertTrue(formatted.contains("- ROI: Return on Investment"));
            assertTrue(formatted.contains("=== WRITING GUIDELINES ==="));
            assertTrue(formatted.contains("Be clear and concise"));
        }

        @Test
        @DisplayName("Includes scope configuration when provided")
        void includesScopeConfiguration() {
            DomainContext context = DomainContext.builder()
                    .domainSummary("About investing")
                    .build();

            ScopeConfiguration scope = ScopeConfiguration.builder()
                    .audienceDescription("Beginner investors")
                    .domainDescription("Personal finance")
                    .build();

            String formatted = context.toPromptFormat(scope);

            assertTrue(formatted.contains("=== SCOPE & AUDIENCE ==="));
            assertTrue(formatted.contains("Beginner investors"));
            assertTrue(formatted.contains("Personal finance"));
        }
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTests {

        @Test
        @DisplayName("toBuilder preserves all fields")
        void toBuilderPreservesAllFields() {
            DomainContext original = DomainContext.builder()
                    .domainSummary("Summary")
                    .addKeyTheme("Theme1")
                    .addGlossaryTerm("T1", "D1")
                    .addAuthorityUrl("url1")
                    .addTopicCluster("C1", List.of("T1", "T2"))
                    .writingGuidelines("Guidelines")
                    .build();

            DomainContext copy = original.toBuilder().build();

            assertEquals(original.domainSummary(), copy.domainSummary());
            assertEquals(original.keyThemes(), copy.keyThemes());
            assertEquals(original.glossary(), copy.glossary());
            assertEquals(original.authorityUrls(), copy.authorityUrls());
            assertEquals(original.topicClusters(), copy.topicClusters());
            assertEquals(original.writingGuidelines(), copy.writingGuidelines());
        }

        @Test
        @DisplayName("toBuilder allows modification")
        void toBuilderAllowsModification() {
            DomainContext original = DomainContext.builder()
                    .domainSummary("Original")
                    .build();

            DomainContext modified = original.toBuilder()
                    .domainSummary("Modified")
                    .addKeyTheme("New theme")
                    .build();

            assertEquals("Modified", modified.domainSummary());
            assertEquals(1, modified.keyThemes().size());
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerialization {

        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
            objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
        }

        @Test
        @DisplayName("Serializes and deserializes correctly")
        void serializesAndDeserializes() throws Exception {
            DomainContext original = DomainContext.builder()
                    .domainSummary("Test summary")
                    .addKeyTheme("Theme1")
                    .addKeyTheme("Theme2")
                    .addGlossaryTerm("Term1", "Definition1")
                    .addAuthorityUrl("https://example.com")
                    .addTopicCluster("Cluster1", List.of("TopicA", "TopicB"))
                    .writingGuidelines("Write clearly")
                    .build();

            String json = objectMapper.writeValueAsString(original);
            DomainContext deserialized = objectMapper.readValue(json, DomainContext.class);

            assertEquals(original.domainSummary(), deserialized.domainSummary());
            assertEquals(original.keyThemes(), deserialized.keyThemes());
            assertEquals(original.glossary(), deserialized.glossary());
            assertEquals(original.authorityUrls(), deserialized.authorityUrls());
            assertEquals(original.topicClusters(), deserialized.topicClusters());
            assertEquals(original.writingGuidelines(), deserialized.writingGuidelines());
        }

        @Test
        @DisplayName("Handles empty context serialization")
        void handlesEmptyContextSerialization() throws Exception {
            DomainContext empty = DomainContext.empty();

            String json = objectMapper.writeValueAsString(empty);
            DomainContext deserialized = objectMapper.readValue(json, DomainContext.class);

            assertEquals("", deserialized.domainSummary());
            assertTrue(deserialized.keyThemes().isEmpty());
            assertTrue(deserialized.glossary().isEmpty());
        }
    }
}
