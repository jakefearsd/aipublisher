package com.jakefear.aipublisher.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.domain.RelationshipType;
import com.jakefear.aipublisher.domain.Topic;
import com.jakefear.aipublisher.util.JsonParsingUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI service that suggests relationships between topics in the universe.
 * Analyzes topic pairs to identify prerequisites, hierarchies, and semantic relationships.
 */
@Component
public class RelationshipSuggester {

    private static final Logger log = LoggerFactory.getLogger(RelationshipSuggester.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are an expert knowledge architect analyzing relationships between topics in a technical wiki.
        Your task is to identify meaningful relationships that will help readers navigate and understand the content.

        Relationship types to consider:
        1. PREREQUISITE_OF - Topic A must be understood before Topic B
        2. PART_OF - Topic A is a component or subtopic of Topic B
        3. EXAMPLE_OF - Topic A is a concrete example of Topic B
        4. RELATED_TO - Topics share common themes but neither is prerequisite
        5. CONTRASTS_WITH - Topics are alternatives or have important differences
        6. IMPLEMENTS - Topic A is an implementation/application of Topic B
        7. SUPERSEDES - Topic A replaces or is the modern version of Topic B
        8. PAIRS_WITH - Topics are commonly used together

        Guidelines:
        - Only suggest relationships that are meaningful and would help readers
        - PREREQUISITE_OF should be used when understanding A significantly aids understanding B
        - Provide confidence scores based on how certain the relationship is
        - Explain the rationale to help users validate suggestions

        Always respond with valid JSON in the specified format.
        """;

    public RelationshipSuggester(@Qualifier("researchChatModel") ChatLanguageModel model) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Suggest relationships for a new topic being added to the universe.
     *
     * @param newTopic The topic being added
     * @param existingTopics Topics already in the universe
     * @return List of relationship suggestions
     */
    public List<RelationshipSuggestion> suggestRelationshipsForTopic(
            Topic newTopic,
            List<Topic> existingTopics) {

        if (existingTopics.isEmpty()) {
            return List.of();
        }

        log.info("Suggesting relationships for topic: {} against {} existing topics",
                newTopic.name(), existingTopics.size());

        String prompt = buildSingleTopicPrompt(newTopic, existingTopics);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse relationship suggestions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Analyze all topics and suggest relationships between them.
     * Best used after initial topic expansion to map the domain structure.
     *
     * @param topics All topics to analyze
     * @return List of relationship suggestions
     */
    public List<RelationshipSuggestion> analyzeAllRelationships(List<Topic> topics) {
        if (topics.size() < 2) {
            return List.of();
        }

        log.info("Analyzing relationships among {} topics", topics.size());

        String prompt = buildFullAnalysisPrompt(topics);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse relationship analysis: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Identify prerequisite chains - ordered learning paths through topics.
     *
     * @param topics Topics to analyze
     * @return List of prerequisite relationships forming learning paths
     */
    public List<RelationshipSuggestion> identifyPrerequisiteChains(List<Topic> topics) {
        if (topics.size() < 2) {
            return List.of();
        }

        log.info("Identifying prerequisite chains among {} topics", topics.size());

        String prompt = buildPrerequisitePrompt(topics);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response).stream()
                    .filter(r -> r.suggestedType() == RelationshipType.PREREQUISITE_OF)
                    .toList();
        } catch (JsonProcessingException e) {
            log.error("Failed to parse prerequisite analysis: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildSingleTopicPrompt(Topic newTopic, List<Topic> existingTopics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## New Topic\n");
        prompt.append("Name: ").append(newTopic.name()).append("\n");
        prompt.append("Description: ").append(newTopic.description()).append("\n");
        prompt.append("Content Type: ").append(newTopic.contentType().name()).append("\n");
        prompt.append("Complexity: ").append(newTopic.complexity().name()).append("\n\n");

        prompt.append("## Existing Topics\n");
        for (Topic topic : existingTopics) {
            prompt.append("- **").append(topic.name()).append("**: ")
                    .append(topic.description()).append("\n");
        }
        prompt.append("\n");

        prompt.append("""
            ## Task
            Analyze how the new topic relates to each existing topic.
            Suggest meaningful relationships that would help wiki readers.
            Focus on the most important relationships - aim for quality over quantity.

            Respond with JSON:
            ```json
            {
              "relationships": [
                {
                  "source": "Source Topic Name",
                  "target": "Target Topic Name",
                  "type": "PREREQUISITE_OF|PART_OF|EXAMPLE_OF|RELATED_TO|CONTRASTS_WITH|IMPLEMENTS|SUPERSEDES|PAIRS_WITH",
                  "confidence": 0.85,
                  "rationale": "Why this relationship exists"
                }
              ]
            }
            ```
            """);

        return prompt.toString();
    }

    private String buildFullAnalysisPrompt(List<Topic> topics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## Topics to Analyze\n");
        for (Topic topic : topics) {
            prompt.append("- **").append(topic.name()).append("** (")
                    .append(topic.contentType().name()).append(", ")
                    .append(topic.complexity().name()).append("): ")
                    .append(topic.description()).append("\n");
        }
        prompt.append("\n");

        prompt.append("""
            ## Task
            Analyze all topics and identify meaningful relationships between them.
            Consider all relationship types but prioritize:
            1. Prerequisites (for learning path ordering)
            2. Part-of hierarchies (for navigation structure)
            3. Key related concepts (for cross-references)

            Suggest 10-20 most important relationships.
            Avoid suggesting relationships that are obvious or not useful for readers.

            Respond with JSON:
            ```json
            {
              "relationships": [
                {
                  "source": "Source Topic Name",
                  "target": "Target Topic Name",
                  "type": "PREREQUISITE_OF|PART_OF|EXAMPLE_OF|RELATED_TO|CONTRASTS_WITH|IMPLEMENTS|SUPERSEDES|PAIRS_WITH",
                  "confidence": 0.85,
                  "rationale": "Why this relationship exists"
                }
              ]
            }
            ```
            """);

        return prompt.toString();
    }

    private String buildPrerequisitePrompt(List<Topic> topics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## Topics\n");
        String topicList = topics.stream()
                .map(t -> String.format("- %s (%s): %s",
                        t.name(), t.complexity().name(), t.description()))
                .collect(Collectors.joining("\n"));
        prompt.append(topicList).append("\n\n");

        prompt.append("""
            ## Task
            Analyze these topics and identify prerequisite relationships.
            A prerequisite relationship means Topic A should be understood BEFORE Topic B.

            Consider:
            - Complexity levels (beginner topics often prerequisite to advanced)
            - Conceptual dependencies (fundamentals before applications)
            - Terminology dependencies (definitions before usage)

            Create a logical learning path through these topics.
            Only suggest prerequisites where the dependency is meaningful.

            Respond with JSON:
            ```json
            {
              "relationships": [
                {
                  "source": "Prerequisite Topic Name",
                  "target": "Dependent Topic Name",
                  "type": "PREREQUISITE_OF",
                  "confidence": 0.85,
                  "rationale": "Why source should be learned before target"
                }
              ]
            }
            ```
            """);

        return prompt.toString();
    }

    private List<RelationshipSuggestion> parseResponse(String response)
            throws JsonProcessingException {

        JsonNode root = JsonParsingUtils.parseJson(response, objectMapper);
        JsonNode relationshipsNode = root.get("relationships");

        if (relationshipsNode == null || !relationshipsNode.isArray()) {
            log.warn("No relationships array found in response");
            return List.of();
        }

        List<RelationshipSuggestion> suggestions = new ArrayList<>();

        for (JsonNode node : relationshipsNode) {
            try {
                RelationshipSuggestion suggestion = parseSuggestion(node);
                suggestions.add(suggestion);
            } catch (Exception e) {
                log.warn("Failed to parse relationship suggestion: {}", e.getMessage());
            }
        }

        log.info("Parsed {} relationship suggestions", suggestions.size());
        return suggestions;
    }

    private RelationshipSuggestion parseSuggestion(JsonNode node) {
        String source = JsonParsingUtils.getRequiredString(node, "source");
        String target = JsonParsingUtils.getRequiredString(node, "target");
        String typeStr = JsonParsingUtils.getStringOrDefault(node, "type", "RELATED_TO");
        double confidence = JsonParsingUtils.getDoubleOrDefault(node, "confidence", 0.5);
        String rationale = JsonParsingUtils.getStringOrDefault(node, "rationale", "");

        RelationshipType type;
        try {
            type = RelationshipType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            type = RelationshipType.RELATED_TO;
        }

        return RelationshipSuggestion.full(source, target, type, confidence, rationale);
    }
}
