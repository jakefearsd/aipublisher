package com.jakefear.aipublisher.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TopicUniverseRepository focusing on proper JSON serialization.
 *
 * These tests ensure that:
 * 1. Universe files can be saved and loaded correctly
 * 2. Computed properties are NOT serialized to JSON (via @JsonIgnore)
 * 3. The JSON structure matches expected format
 * 4. Round-trip serialization preserves all data
 */
@DisplayName("TopicUniverseRepository")
class TopicUniverseRepositoryTest {

    @TempDir
    Path tempDir;

    private TopicUniverseRepository repository;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = new TopicUniverseRepository();
        repository.setStorageDirectory(tempDir);

        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Creates a realistic test universe similar to what the discovery session produces.
     */
    private TopicUniverse createRealisticUniverse() {
        // Landing page topic
        Topic landingPage = Topic.builder("What is Investing")
                .id("WhatisInvesting")
                .description("Introduction to investing fundamentals and how it builds wealth over time.")
                .status(TopicStatus.ACCEPTED)
                .contentType(ContentType.CONCEPT)
                .complexity(ComplexityLevel.BEGINNER)
                .priority(Priority.MUST_HAVE)
                .estimatedWords(1000)
                .isLandingPage(true)
                .addedReason("Landing page for domain")
                .build();

        // User-provided seed topics
        Topic compounding = Topic.builder("Compound Interest")
                .id("CompoundInterest")
                .description("How money grows exponentially through compound returns.")
                .status(TopicStatus.ACCEPTED)
                .contentType(ContentType.CONCEPT)
                .complexity(ComplexityLevel.INTERMEDIATE)
                .priority(Priority.MUST_HAVE)
                .estimatedWords(1200)
                .addedReason("User-provided seed topic")
                .build();

        Topic accountTypes = Topic.builder("Investment Account Types")
                .id("InvestmentAccountTypes")
                .description("Overview of IRA, 401k, Roth, and taxable brokerage accounts.")
                .status(TopicStatus.ACCEPTED)
                .contentType(ContentType.REFERENCE)
                .complexity(ComplexityLevel.INTERMEDIATE)
                .priority(Priority.MUST_HAVE)
                .estimatedWords(1500)
                .emphasize(Set.of("tax advantages", "contribution limits"))
                .addedReason("User-provided seed topic")
                .build();

        // AI-suggested topics
        Topic riskReturn = Topic.builder("Risk vs Return")
                .id("RiskvsReturn")
                .description("The fundamental relationship between investment risk and potential returns.")
                .status(TopicStatus.ACCEPTED)
                .contentType(ContentType.CONCEPT)
                .complexity(ComplexityLevel.BEGINNER)
                .priority(Priority.SHOULD_HAVE)
                .estimatedWords(800)
                .category("component")
                .addedReason("AI suggested: Essential concept for understanding investment decisions")
                .build();

        Topic assetAllocation = Topic.builder("Asset Allocation")
                .id("AssetAllocation")
                .description("How to distribute investments across different asset classes.")
                .status(TopicStatus.ACCEPTED)
                .contentType(ContentType.CONCEPT)
                .complexity(ComplexityLevel.INTERMEDIATE)
                .priority(Priority.SHOULD_HAVE)
                .estimatedWords(1000)
                .category("application")
                .addedReason("AI suggested: Practical application of diversification principles")
                .build();

        // A proposed topic (not yet accepted)
        Topic taxLossHarvesting = Topic.builder("Tax Loss Harvesting")
                .id("TaxLossHarvesting")
                .description("Strategy for reducing taxes by selling losing investments.")
                .status(TopicStatus.PROPOSED)
                .contentType(ContentType.TUTORIAL)
                .complexity(ComplexityLevel.ADVANCED)
                .priority(Priority.NICE_TO_HAVE)
                .estimatedWords(1200)
                .category("advanced")
                .addedReason("AI suggested: Advanced tax optimization technique")
                .build();

        // A deferred topic
        Topic optionsTrading = Topic.builder("Options Trading Basics")
                .id("OptionsTradingBasics")
                .description("Introduction to call and put options.")
                .status(TopicStatus.DEFERRED)
                .contentType(ContentType.TUTORIAL)
                .complexity(ComplexityLevel.ADVANCED)
                .priority(Priority.BACKLOG)
                .estimatedWords(2000)
                .addedReason("User deferred: Too advanced for initial wiki")
                .build();

        // Relationships
        TopicRelationship prereq1 = TopicRelationship.confirmed(
                "WhatisInvesting", "CompoundInterest", RelationshipType.PREREQUISITE_OF);

        TopicRelationship prereq2 = TopicRelationship.confirmed(
                "WhatisInvesting", "InvestmentAccountTypes", RelationshipType.PREREQUISITE_OF);

        TopicRelationship prereq3 = TopicRelationship.confirmed(
                "WhatisInvesting", "RiskvsReturn", RelationshipType.PREREQUISITE_OF);

        TopicRelationship related1 = TopicRelationship.confirmed(
                "RiskvsReturn", "AssetAllocation", RelationshipType.RELATED_TO);

        TopicRelationship related2 = TopicRelationship.confirmed(
                "CompoundInterest", "InvestmentAccountTypes", RelationshipType.RELATED_TO);

        // Scope configuration
        ScopeConfiguration scope = ScopeConfiguration.builder()
                .addAssumedKnowledge("Basic math")
                .addAssumedKnowledge("Understanding of percentages")
                .addFocusArea("Long-term investing")
                .addFocusArea("Tax-advantaged accounts")
                .addOutOfScope("Day trading")
                .addOutOfScope("Cryptocurrency speculation")
                .audienceDescription("Beginner investors aged 25-45")
                .domainDescription("Personal finance and wealth building")
                .build();

        return TopicUniverse.builder("Investing Basics")
                .id("investing-basics")
                .description("A comprehensive guide to getting started with investing for long-term wealth.")
                .addTopic(landingPage)
                .addTopic(compounding)
                .addTopic(accountTypes)
                .addTopic(riskReturn)
                .addTopic(assetAllocation)
                .addTopic(taxLossHarvesting)
                .addTopic(optionsTrading)
                .addRelationship(prereq1)
                .addRelationship(prereq2)
                .addRelationship(prereq3)
                .addRelationship(related1)
                .addRelationship(related2)
                .scope(scope)
                .addToBacklog("Dollar cost averaging")
                .addToBacklog("Emergency fund basics")
                .build();
    }

    @Nested
    @DisplayName("JSON Serialization")
    class JsonSerialization {

        @Test
        @DisplayName("Should save universe to JSON file")
        void shouldSaveUniverseToJsonFile() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);

            assertTrue(Files.exists(savedPath));
            String content = Files.readString(savedPath);
            assertTrue(content.contains("\"id\" : \"investing-basics\""));
            assertTrue(content.contains("\"name\" : \"Investing Basics\""));
        }

        @Test
        @DisplayName("Should NOT serialize computed Topic properties")
        void shouldNotSerializeComputedTopicProperties() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);
            String json = Files.readString(savedPath);

            // These are computed properties that should NOT appear in JSON
            assertFalse(json.contains("\"wikiPageName\""),
                    "wikiPageName is computed and should not be serialized");
            assertFalse(json.contains("\"readyForGeneration\""),
                    "readyForGeneration is computed and should not be serialized");
            assertFalse(json.contains("\"userGuidance\""),
                    "hasUserGuidance is computed and should not be serialized");
        }

        @Test
        @DisplayName("Should NOT serialize computed TopicUniverse properties")
        void shouldNotSerializeComputedUniverseProperties() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);
            String json = Files.readString(savedPath);

            // These are computed properties that should NOT appear in JSON
            assertFalse(json.contains("\"acceptedTopics\""),
                    "acceptedTopics is computed and should not be serialized");
            assertFalse(json.contains("\"proposedTopics\""),
                    "proposedTopics is computed and should not be serialized");
            assertFalse(json.contains("\"generationOrder\""),
                    "generationOrder is computed and should not be serialized");
            assertFalse(json.contains("\"acceptedCount\""),
                    "acceptedCount is computed and should not be serialized");
            assertFalse(json.contains("\"estimatedWordCount\""),
                    "estimatedWordCount is computed and should not be serialized");
            assertFalse(json.contains("\"countByPriority\""),
                    "countByPriority is computed and should not be serialized");
            assertFalse(json.contains("\"countByStatus\""),
                    "countByStatus is computed and should not be serialized");
        }

        @Test
        @DisplayName("Should serialize only essential Topic fields")
        void shouldSerializeEssentialTopicFields() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);
            String json = Files.readString(savedPath);
            JsonNode root = objectMapper.readTree(json);
            JsonNode firstTopic = root.get("topics").get(0);

            // These fields SHOULD be present
            assertNotNull(firstTopic.get("id"), "id should be serialized");
            assertNotNull(firstTopic.get("name"), "name should be serialized");
            assertNotNull(firstTopic.get("description"), "description should be serialized");
            assertNotNull(firstTopic.get("status"), "status should be serialized");
            assertNotNull(firstTopic.get("contentType"), "contentType should be serialized");
            assertNotNull(firstTopic.get("complexity"), "complexity should be serialized");
            assertNotNull(firstTopic.get("priority"), "priority should be serialized");
            assertNotNull(firstTopic.get("estimatedWords"), "estimatedWords should be serialized");
            assertNotNull(firstTopic.get("isLandingPage"), "isLandingPage should be serialized");
            assertNotNull(firstTopic.get("createdAt"), "createdAt should be serialized");
            assertNotNull(firstTopic.get("addedReason"), "addedReason should be serialized");

            // These computed fields should NOT be present
            assertNull(firstTopic.get("wikiPageName"), "wikiPageName should not be serialized");
            assertNull(firstTopic.get("readyForGeneration"), "readyForGeneration should not be serialized");
        }

        @Test
        @DisplayName("Should serialize only essential TopicUniverse fields")
        void shouldSerializeEssentialUniverseFields() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);
            String json = Files.readString(savedPath);
            JsonNode root = objectMapper.readTree(json);

            // These fields SHOULD be present
            assertNotNull(root.get("id"), "id should be serialized");
            assertNotNull(root.get("name"), "name should be serialized");
            assertNotNull(root.get("description"), "description should be serialized");
            assertNotNull(root.get("topics"), "topics should be serialized");
            assertNotNull(root.get("relationships"), "relationships should be serialized");
            assertNotNull(root.get("scope"), "scope should be serialized");
            assertNotNull(root.get("backlog"), "backlog should be serialized");
            assertNotNull(root.get("createdAt"), "createdAt should be serialized");
            assertNotNull(root.get("modifiedAt"), "modifiedAt should be serialized");

            // These computed fields should NOT be present
            assertNull(root.get("acceptedTopics"), "acceptedTopics should not be serialized");
            assertNull(root.get("proposedTopics"), "proposedTopics should not be serialized");
            assertNull(root.get("generationOrder"), "generationOrder should not be serialized");
            assertNull(root.get("acceptedCount"), "acceptedCount should not be serialized");
            assertNull(root.get("estimatedWordCount"), "estimatedWordCount should not be serialized");
            assertNull(root.get("countByPriority"), "countByPriority should not be serialized");
            assertNull(root.get("countByStatus"), "countByStatus should not be serialized");
        }

        @Test
        @DisplayName("Should properly serialize topics with emphasis and skip sets")
        void shouldSerializeTopicsWithEmphasisAndSkip() throws IOException {
            TopicUniverse universe = createRealisticUniverse();

            Path savedPath = repository.save(universe);
            String json = Files.readString(savedPath);
            JsonNode root = objectMapper.readTree(json);

            // Find the topic with emphasis set
            JsonNode topics = root.get("topics");
            JsonNode accountTypesTopic = null;
            for (JsonNode topic : topics) {
                if ("InvestmentAccountTypes".equals(topic.get("id").asText())) {
                    accountTypesTopic = topic;
                    break;
                }
            }

            assertNotNull(accountTypesTopic, "Should find InvestmentAccountTypes topic");
            assertTrue(accountTypesTopic.get("emphasize").isArray(), "emphasize should be an array");
            assertEquals(2, accountTypesTopic.get("emphasize").size(), "Should have 2 emphasis items");
        }
    }

    @Nested
    @DisplayName("Round-trip Serialization")
    class RoundTripSerialization {

        @Test
        @DisplayName("Should preserve all data through save/load cycle")
        void shouldPreserveAllDataThroughSaveLoadCycle() throws IOException {
            TopicUniverse original = createRealisticUniverse();

            // Save
            repository.save(original);

            // Load
            TopicUniverse loaded = repository.load("investing-basics").orElseThrow();

            // Verify core properties
            assertEquals(original.id(), loaded.id());
            assertEquals(original.name(), loaded.name());
            assertEquals(original.description(), loaded.description());

            // Verify topics
            assertEquals(original.topics().size(), loaded.topics().size());

            // Verify specific topic data
            Topic originalCompound = original.getTopicById("CompoundInterest").orElseThrow();
            Topic loadedCompound = loaded.getTopicById("CompoundInterest").orElseThrow();

            assertEquals(originalCompound.name(), loadedCompound.name());
            assertEquals(originalCompound.description(), loadedCompound.description());
            assertEquals(originalCompound.status(), loadedCompound.status());
            assertEquals(originalCompound.priority(), loadedCompound.priority());
            assertEquals(originalCompound.complexity(), loadedCompound.complexity());
            assertEquals(originalCompound.contentType(), loadedCompound.contentType());

            // Verify relationships
            assertEquals(original.relationships().size(), loaded.relationships().size());

            // Verify scope
            assertEquals(original.scope().assumedKnowledge(), loaded.scope().assumedKnowledge());
            assertEquals(original.scope().focusAreas(), loaded.scope().focusAreas());
            assertEquals(original.scope().outOfScope(), loaded.scope().outOfScope());

            // Verify backlog
            assertEquals(original.backlog(), loaded.backlog());
        }

        @Test
        @DisplayName("Should preserve topic emphasis and skip sets")
        void shouldPreserveTopicEmphasisAndSkipSets() throws IOException {
            TopicUniverse original = createRealisticUniverse();

            repository.save(original);
            TopicUniverse loaded = repository.load("investing-basics").orElseThrow();

            Topic originalAccounts = original.getTopicById("InvestmentAccountTypes").orElseThrow();
            Topic loadedAccounts = loaded.getTopicById("InvestmentAccountTypes").orElseThrow();

            assertEquals(originalAccounts.emphasize(), loadedAccounts.emphasize());
            assertEquals(originalAccounts.skip(), loadedAccounts.skip());
        }

        @Test
        @DisplayName("Should preserve all topic statuses")
        void shouldPreserveAllTopicStatuses() throws IOException {
            TopicUniverse original = createRealisticUniverse();

            repository.save(original);
            TopicUniverse loaded = repository.load("investing-basics").orElseThrow();

            // Check accepted topics
            assertEquals(5, loaded.getAcceptedTopics().size());

            // Check proposed topics
            assertEquals(1, loaded.getProposedTopics().size());
            assertEquals("Tax Loss Harvesting", loaded.getProposedTopics().get(0).name());

            // Check deferred topic
            Topic deferred = loaded.getTopicById("OptionsTradingBasics").orElseThrow();
            assertEquals(TopicStatus.DEFERRED, deferred.status());
        }

        @Test
        @DisplayName("Should preserve relationship data")
        void shouldPreserveRelationshipData() throws IOException {
            TopicUniverse original = createRealisticUniverse();

            repository.save(original);
            TopicUniverse loaded = repository.load("investing-basics").orElseThrow();

            // Check specific relationship - getPrerequisites returns List<Topic>
            List<Topic> prereqTopics = loaded.getPrerequisites("CompoundInterest");
            assertEquals(1, prereqTopics.size());
            assertEquals("What is Investing", prereqTopics.get(0).name());

            // Verify relationship details directly
            List<TopicRelationship> incomingRels = loaded.getIncomingRelationships("CompoundInterest");
            TopicRelationship prereq = incomingRels.stream()
                    .filter(r -> r.type() == RelationshipType.PREREQUISITE_OF)
                    .findFirst()
                    .orElseThrow();
            assertEquals("WhatisInvesting", prereq.sourceTopicId());
            assertEquals(TopicRelationship.RelationshipStatus.CONFIRMED, prereq.status());
        }

        @Test
        @DisplayName("Computed properties should work after loading")
        void computedPropertiesShouldWorkAfterLoading() throws IOException {
            TopicUniverse original = createRealisticUniverse();

            repository.save(original);
            TopicUniverse loaded = repository.load("investing-basics").orElseThrow();

            // These computed methods should work correctly on loaded data
            assertEquals(5, loaded.getAcceptedCount());
            assertTrue(loaded.getEstimatedWordCount() > 0);
            assertFalse(loaded.getGenerationOrder().isEmpty());
            assertNotNull(loaded.getCountByPriority());
            assertNotNull(loaded.getCountByStatus());

            // Topic computed methods
            Topic loadedTopic = loaded.getTopicById("WhatisInvesting").orElseThrow();
            assertEquals("WhatisInvesting", loadedTopic.getWikiPageName());
            assertTrue(loadedTopic.isReadyForGeneration());
        }
    }

    @Nested
    @DisplayName("File Operations")
    class FileOperations {

        @Test
        @DisplayName("Should list all saved universes")
        void shouldListAllSavedUniverses() throws IOException {
            TopicUniverse universe1 = TopicUniverse.create("Domain One", "First domain");
            TopicUniverse universe2 = TopicUniverse.create("Domain Two", "Second domain");

            repository.save(universe1);
            repository.save(universe2);

            List<String> ids = repository.listAll();

            assertEquals(2, ids.size());
            assertTrue(ids.contains("domain-one"));
            assertTrue(ids.contains("domain-two"));
        }

        @Test
        @DisplayName("Should check if universe exists")
        void shouldCheckIfUniverseExists() throws IOException {
            TopicUniverse universe = createRealisticUniverse();
            repository.save(universe);

            assertTrue(repository.exists("investing-basics"));
            assertFalse(repository.exists("nonexistent"));
        }

        @Test
        @DisplayName("Should delete universe")
        void shouldDeleteUniverse() throws IOException {
            TopicUniverse universe = createRealisticUniverse();
            repository.save(universe);

            assertTrue(repository.exists("investing-basics"));
            assertTrue(repository.delete("investing-basics"));
            assertFalse(repository.exists("investing-basics"));
        }

        @Test
        @DisplayName("Should load from specific path")
        void shouldLoadFromSpecificPath() throws IOException {
            TopicUniverse universe = createRealisticUniverse();
            Path customPath = tempDir.resolve("custom-location.json");
            repository.saveToPath(universe, customPath);

            TopicUniverse loaded = repository.loadFromPath(customPath).orElseThrow();

            assertEquals(universe.id(), loaded.id());
            assertEquals(universe.name(), loaded.name());
        }
    }

    @Nested
    @DisplayName("JSON String Conversion")
    class JsonStringConversion {

        @Test
        @DisplayName("toJson should not include computed properties")
        void toJsonShouldNotIncludeComputedProperties() {
            TopicUniverse universe = createRealisticUniverse();

            String json = repository.toJson(universe);

            assertFalse(json.contains("\"acceptedTopics\""));
            assertFalse(json.contains("\"proposedTopics\""));
            assertFalse(json.contains("\"generationOrder\""));
            assertFalse(json.contains("\"wikiPageName\""));
            assertFalse(json.contains("\"readyForGeneration\""));
        }

        @Test
        @DisplayName("fromJson should parse valid JSON")
        void fromJsonShouldParseValidJson() {
            TopicUniverse original = createRealisticUniverse();
            String json = repository.toJson(original);

            TopicUniverse parsed = repository.fromJson(json);

            assertEquals(original.id(), parsed.id());
            assertEquals(original.topics().size(), parsed.topics().size());
        }
    }
}
