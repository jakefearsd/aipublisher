package com.jakefear.aipublisher.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.domain.ComplexityLevel;
import com.jakefear.aipublisher.domain.DomainContext;
import com.jakefear.aipublisher.domain.ScopeConfiguration;
import com.jakefear.aipublisher.search.SearchProvider;
import com.jakefear.aipublisher.search.SearchProviderRegistry;
import com.jakefear.aipublisher.search.SearchResult;
import com.jakefear.aipublisher.util.JsonParsingUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI service that expands a seed topic into related topic suggestions.
 * Uses Claude to analyze a topic and suggest related concepts, subtopics,
 * prerequisites, and applications.
 */
@Component
public class TopicExpander {

    private static final Logger log = LoggerFactory.getLogger(TopicExpander.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;
    private final SearchProviderRegistry searchRegistry;

    private static final String SYSTEM_PROMPT = """
        You are an expert knowledge architect helping to design a comprehensive wiki about a technical domain.
        Your task is to analyze a topic and suggest related topics that would help create a complete, well-structured knowledge base.

        When suggesting topics, consider:
        1. PREREQUISITES - What must readers understand first?
        2. COMPONENTS - What are the key parts or subtopics?
        3. RELATED CONCEPTS - What parallel or complementary concepts exist?
        4. APPLICATIONS - How is this used in practice?
        5. ADVANCED TOPICS - What builds on this foundation?

        For each suggestion, provide:
        - A clear, specific topic name
        - A brief description (1-2 sentences)
        - The category (prerequisite/component/related/application/advanced)
        - Suggested content type (concept/tutorial/reference/how-to/comparison/troubleshooting)
        - Complexity level (beginner/intermediate/advanced)
        - A relevance score (0.0-1.0) indicating how important this topic is to the domain
        - A brief rationale explaining why this topic should be included

        Always respond with valid JSON in the specified format.
        """;

    public TopicExpander(
            @Qualifier("researchChatModel") ChatLanguageModel model,
            SearchProviderRegistry searchRegistry) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
        this.searchRegistry = searchRegistry;
    }

    /**
     * Expand a seed topic into related topic suggestions.
     *
     * @param seedTopic The topic to expand from
     * @param domainName The overall domain context
     * @param existingTopics Topics already in the universe (to avoid duplicates)
     * @param scope Optional scope configuration for guidance
     * @return List of topic suggestions
     */
    public List<TopicSuggestion> expandTopic(
            String seedTopic,
            String domainName,
            Set<String> existingTopics,
            ScopeConfiguration scope) {
        return expandTopic(seedTopic, domainName, existingTopics, scope, null);
    }

    /**
     * Expand a seed topic into related topic suggestions with cost profile control.
     *
     * @param seedTopic The topic to expand from
     * @param domainName The overall domain context
     * @param existingTopics Topics already in the universe (to avoid duplicates)
     * @param scope Optional scope configuration for guidance
     * @param costProfile Optional cost profile for controlling suggestion count
     * @return List of topic suggestions
     */
    public List<TopicSuggestion> expandTopic(
            String seedTopic,
            String domainName,
            Set<String> existingTopics,
            ScopeConfiguration scope,
            CostProfile costProfile) {

        log.info("Expanding topic: {} in domain: {}", seedTopic, domainName);

        String suggestionsRange = costProfile != null ? costProfile.getSuggestionsRange() : "5-10";
        String prompt = buildPrompt(seedTopic, domainName, existingTopics, scope, suggestionsRange);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response, seedTopic);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse topic expansion response: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Generate initial topic suggestions from a domain description.
     *
     * @param domainName The domain name
     * @param domainDescription Description of what the wiki should cover
     * @param scope Optional scope configuration
     * @return Initial set of topic suggestions
     */
    public List<TopicSuggestion> generateInitialTopics(
            String domainName,
            String domainDescription,
            ScopeConfiguration scope) {

        log.info("Generating initial topics for domain: {}", domainName);

        String prompt = buildInitialTopicsPrompt(domainName, domainDescription, scope);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response, "initial domain analysis");
        } catch (JsonProcessingException e) {
            log.error("Failed to parse initial topics response: {}", e.getMessage());
            return List.of();
        }
    }

    // ==================== Search-Grounded Methods ====================

    /**
     * Build domain context by gathering information from search.
     * This context can be used throughout article generation for consistency.
     *
     * @param domainName The domain name to research
     * @param domainDescription Optional user-provided description
     * @return DomainContext with search-derived information
     */
    public DomainContext buildDomainContext(String domainName, String domainDescription) {
        log.info("Building domain context for: {}", domainName);

        SearchProvider searchProvider = searchRegistry.getDefault();
        if (!searchProvider.isEnabled()) {
            log.warn("Search provider is disabled, returning empty domain context");
            return DomainContext.empty();
        }

        DomainContext.Builder builder = DomainContext.builder();

        // 1. Get domain summary from search
        SearchResult domainInfo = searchProvider.getTopicSummary(domainName);
        if (domainInfo != null) {
            builder.domainSummary(domainInfo.snippet());
            builder.addAuthorityUrl(domainInfo.url());
            log.debug("Found domain summary: {} chars", domainInfo.snippet().length());
        }

        // 2. Get related topics from search
        List<String> relatedTopics = searchProvider.getRelatedTopics(domainName);
        log.debug("Found {} related topics from search", relatedTopics.size());

        // 3. Use LLM to synthesize themes and guidelines from search results
        if (domainInfo != null || !relatedTopics.isEmpty()) {
            synthesizeContext(builder, domainName, domainDescription, domainInfo, relatedTopics);
        }

        builder.lastUpdated(Instant.now());
        DomainContext context = builder.build();
        log.info("Built domain context with {} themes, {} glossary terms",
                context.keyThemes().size(), context.glossary().size());

        return context;
    }

    /**
     * Expand a topic with search grounding.
     * First gathers search context, then asks LLM for suggestions, finally validates against search.
     *
     * @param seedTopic The topic to expand from
     * @param domainName The overall domain context
     * @param existingTopics Topics already in the universe
     * @param scope Optional scope configuration
     * @param costProfile Optional cost profile for controlling suggestion count
     * @return List of validated topic suggestions with search confidence scores
     */
    public List<TopicSuggestion> expandTopicWithSearch(
            String seedTopic,
            String domainName,
            Set<String> existingTopics,
            ScopeConfiguration scope,
            CostProfile costProfile) {

        log.info("Expanding topic with search grounding: {} in domain: {}", seedTopic, domainName);

        SearchProvider searchProvider = searchRegistry.getDefault();
        if (!searchProvider.isEnabled()) {
            log.warn("Search provider disabled, falling back to non-search expansion");
            return expandTopic(seedTopic, domainName, existingTopics, scope, costProfile);
        }

        // 1. Get search context for this topic
        SearchResult topicInfo = searchProvider.getTopicSummary(seedTopic);
        List<String> searchRelated = searchProvider.getRelatedTopics(seedTopic);
        log.debug("Search found {} related topics for '{}'", searchRelated.size(), seedTopic);

        // 2. Build search-grounded prompt
        String suggestionsRange = costProfile != null ? costProfile.getSuggestionsRange() : "5-10";
        String prompt = buildSearchGroundedPrompt(
                seedTopic, domainName, existingTopics, scope,
                topicInfo, searchRelated, suggestionsRange);

        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        // 3. Parse suggestions
        List<TopicSuggestion> suggestions;
        try {
            suggestions = parseResponse(response, seedTopic);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse topic expansion response: {}", e.getMessage());
            return List.of();
        }

        // 4. Validate suggestions against search
        return validateSuggestionsWithSearch(suggestions, searchProvider);
    }

    /**
     * Validate that suggested topics exist in search results.
     * Adds confidence scores based on how well each topic validates.
     */
    private List<TopicSuggestion> validateSuggestionsWithSearch(
            List<TopicSuggestion> suggestions,
            SearchProvider searchProvider) {

        List<TopicSuggestion> validated = new ArrayList<>();

        for (TopicSuggestion suggestion : suggestions) {
            double confidence = searchProvider.validateTopic(suggestion.name());

            if (confidence < 0.3) {
                log.warn("Low confidence topic '{}' ({:.2f}) - may be hallucinated",
                        suggestion.name(), confidence);
            } else {
                log.debug("Topic '{}' validated with confidence {:.2f}",
                        suggestion.name(), confidence);
            }

            validated.add(suggestion.withSearchConfidence(confidence));
        }

        return validated;
    }

    /**
     * Use LLM to synthesize key themes and writing guidelines from search context.
     */
    private void synthesizeContext(
            DomainContext.Builder builder,
            String domainName,
            String domainDescription,
            SearchResult domainInfo,
            List<String> relatedTopics) {

        StringBuilder synthesisPrompt = new StringBuilder();
        synthesisPrompt.append("Analyze this domain and extract key themes and terminology.\n\n");
        synthesisPrompt.append("Domain: ").append(domainName).append("\n");

        if (domainDescription != null && !domainDescription.isBlank()) {
            synthesisPrompt.append("User description: ").append(domainDescription).append("\n");
        }

        if (domainInfo != null) {
            synthesisPrompt.append("Summary from search: ").append(domainInfo.snippet()).append("\n");
        }

        if (!relatedTopics.isEmpty()) {
            synthesisPrompt.append("Related topics from search: ");
            synthesisPrompt.append(String.join(", ", relatedTopics.subList(0, Math.min(20, relatedTopics.size()))));
            synthesisPrompt.append("\n");
        }

        synthesisPrompt.append("""

            Based on the above, provide:
            1. 3-5 key themes that articles should maintain for consistency
            2. 5-10 important domain terms with brief definitions
            3. Writing guidelines for this domain (2-3 sentences)

            Respond with JSON:
            ```json
            {
              "keyThemes": ["theme1", "theme2"],
              "glossary": {"term": "definition"},
              "writingGuidelines": "Guidelines here"
            }
            ```
            """);

        try {
            String response = model.generate(synthesisPrompt.toString());
            JsonNode root = JsonParsingUtils.parseJson(response, objectMapper);

            // Extract key themes
            JsonNode themesNode = root.get("keyThemes");
            if (themesNode != null && themesNode.isArray()) {
                List<String> themes = new ArrayList<>();
                for (JsonNode theme : themesNode) {
                    themes.add(theme.asText());
                }
                builder.keyThemes(themes);
            }

            // Extract glossary
            JsonNode glossaryNode = root.get("glossary");
            if (glossaryNode != null && glossaryNode.isObject()) {
                Map<String, String> glossary = new LinkedHashMap<>();
                glossaryNode.fields().forEachRemaining(entry ->
                        glossary.put(entry.getKey(), entry.getValue().asText()));
                builder.glossary(glossary);
            }

            // Extract writing guidelines
            JsonNode guidelinesNode = root.get("writingGuidelines");
            if (guidelinesNode != null) {
                builder.writingGuidelines(guidelinesNode.asText());
            }

        } catch (Exception e) {
            log.warn("Failed to synthesize domain context: {}", e.getMessage());
        }
    }

    /**
     * Build a prompt that includes search context for grounding.
     */
    private String buildSearchGroundedPrompt(
            String seedTopic,
            String domainName,
            Set<String> existingTopics,
            ScopeConfiguration scope,
            SearchResult topicInfo,
            List<String> searchRelated,
            String suggestionsRange) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("## Domain Context\n");
        prompt.append("Domain: ").append(domainName).append("\n\n");

        prompt.append("## Seed Topic to Expand\n");
        prompt.append("Topic: ").append(seedTopic).append("\n");

        // Include search context
        if (topicInfo != null) {
            prompt.append("Summary from search: ").append(topicInfo.snippet()).append("\n");
        }

        if (!searchRelated.isEmpty()) {
            prompt.append("Related topics from search: ");
            prompt.append(String.join(", ", searchRelated.subList(0, Math.min(15, searchRelated.size()))));
            prompt.append("\n");
        }
        prompt.append("\n");

        if (!existingTopics.isEmpty()) {
            prompt.append("## Existing Topics (do not suggest duplicates)\n");
            prompt.append(existingTopics.stream()
                    .sorted()
                    .collect(Collectors.joining(", ")));
            prompt.append("\n\n");
        }

        if (scope != null) {
            appendScopeGuidance(prompt, scope);
        }

        prompt.append(String.format("""
            ## Task
            Analyze the seed topic and suggest %s related topics that would help create a comprehensive wiki.
            Focus on topics that directly support understanding or applying the seed topic.

            IMPORTANT: Prefer suggesting topics that appear in the "Related topics from search" list or are
            closely related to them. This helps ensure topic suggestions are grounded in real knowledge.

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "prerequisite|component|related|application|advanced",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is important for the wiki"
                }
              ]
            }
            ```
            """, suggestionsRange));

        return prompt.toString();
    }

    private String buildPrompt(
            String seedTopic,
            String domainName,
            Set<String> existingTopics,
            ScopeConfiguration scope,
            String suggestionsRange) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("## Domain Context\n");
        prompt.append("Domain: ").append(domainName).append("\n\n");

        prompt.append("## Seed Topic to Expand\n");
        prompt.append("Topic: ").append(seedTopic).append("\n\n");

        if (!existingTopics.isEmpty()) {
            prompt.append("## Existing Topics (do not suggest duplicates)\n");
            prompt.append(existingTopics.stream()
                    .sorted()
                    .collect(Collectors.joining(", ")));
            prompt.append("\n\n");
        }

        if (scope != null) {
            appendScopeGuidance(prompt, scope);
        }

        prompt.append(String.format("""
            ## Task
            Analyze the seed topic and suggest %s related topics that would help create a comprehensive wiki.
            Focus on topics that directly support understanding or applying the seed topic.

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "prerequisite|component|related|application|advanced",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is important for the wiki"
                }
              ]
            }
            ```
            """, suggestionsRange));

        return prompt.toString();
    }

    private String buildInitialTopicsPrompt(
            String domainName,
            String domainDescription,
            ScopeConfiguration scope) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("## Domain\n");
        prompt.append("Name: ").append(domainName).append("\n");
        if (domainDescription != null && !domainDescription.isBlank()) {
            prompt.append("Description: ").append(domainDescription).append("\n");
        }
        prompt.append("\n");

        if (scope != null) {
            appendScopeGuidance(prompt, scope);
        }

        prompt.append("""
            ## Task
            Analyze this domain and suggest 10-15 foundational topics that would form the core of a comprehensive wiki.
            Include a mix of:
            - Core concepts that define the domain
            - Practical tutorials for hands-on learning
            - Reference material for ongoing use
            - Comparisons with alternatives where relevant

            Respond with JSON in this format:
            ```json
            {
              "suggestions": [
                {
                  "name": "Topic Name",
                  "description": "Brief description of what this topic covers",
                  "category": "core|foundation|practical|reference|comparison",
                  "contentType": "CONCEPT|TUTORIAL|REFERENCE|HOW_TO|COMPARISON|TROUBLESHOOTING",
                  "complexity": "BEGINNER|INTERMEDIATE|ADVANCED",
                  "relevance": 0.85,
                  "rationale": "Why this topic is essential for the wiki"
                }
              ]
            }
            ```
            """);

        return prompt.toString();
    }

    private void appendScopeGuidance(StringBuilder prompt, ScopeConfiguration scope) {
        prompt.append("## Scope Guidance\n");

        if (!scope.assumedKnowledge().isEmpty()) {
            prompt.append("Assumed reader knowledge (do not cover): ");
            prompt.append(String.join(", ", scope.assumedKnowledge()));
            prompt.append("\n");
        }

        if (!scope.outOfScope().isEmpty()) {
            prompt.append("Out of scope (exclude): ");
            prompt.append(String.join(", ", scope.outOfScope()));
            prompt.append("\n");
        }

        if (!scope.focusAreas().isEmpty()) {
            prompt.append("Focus areas (prioritize): ");
            prompt.append(String.join(", ", scope.focusAreas()));
            prompt.append("\n");
        }

        if (scope.audienceDescription() != null && !scope.audienceDescription().isBlank()) {
            prompt.append("Target audience: ").append(scope.audienceDescription());
            prompt.append("\n");
        }

        prompt.append("\n");
    }

    private List<TopicSuggestion> parseResponse(String response, String sourceContext)
            throws JsonProcessingException {

        JsonNode root = JsonParsingUtils.parseJson(response, objectMapper);
        JsonNode suggestionsNode = root.get("suggestions");

        if (suggestionsNode == null || !suggestionsNode.isArray()) {
            log.warn("No suggestions array found in response");
            return List.of();
        }

        List<TopicSuggestion> suggestions = new ArrayList<>();

        for (JsonNode node : suggestionsNode) {
            try {
                TopicSuggestion suggestion = parseSuggestion(node, sourceContext);
                suggestions.add(suggestion);
            } catch (Exception e) {
                log.warn("Failed to parse suggestion: {}", e.getMessage());
            }
        }

        log.info("Parsed {} topic suggestions", suggestions.size());
        return suggestions;
    }

    private TopicSuggestion parseSuggestion(JsonNode node, String sourceContext) {
        String name = JsonParsingUtils.getRequiredString(node, "name");
        String description = JsonParsingUtils.getStringOrDefault(node, "description", "");
        String category = JsonParsingUtils.getStringOrDefault(node, "category", "related");

        ContentType contentType = parseContentType(
                JsonParsingUtils.getStringOrDefault(node, "contentType", "CONCEPT"));
        ComplexityLevel complexity = parseComplexity(
                JsonParsingUtils.getStringOrDefault(node, "complexity", "INTERMEDIATE"));

        double relevance = JsonParsingUtils.getDoubleOrDefault(node, "relevance", 0.5);
        String rationale = JsonParsingUtils.getStringOrDefault(node, "rationale", "");

        return TopicSuggestion.builder(name)
                .description(description)
                .category(category)
                .contentType(contentType)
                .complexity(complexity)
                .relevance(relevance)
                .rationale(rationale)
                .sourceContext(sourceContext)
                .build();
    }

    private ContentType parseContentType(String value) {
        try {
            return ContentType.valueOf(value.toUpperCase().replace("-", "_"));
        } catch (IllegalArgumentException e) {
            return ContentType.CONCEPT;
        }
    }

    private ComplexityLevel parseComplexity(String value) {
        try {
            return ComplexityLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ComplexityLevel.INTERMEDIATE;
        }
    }

}
