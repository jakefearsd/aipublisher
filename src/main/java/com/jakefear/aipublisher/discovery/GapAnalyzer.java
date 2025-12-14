package com.jakefear.aipublisher.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jakefear.aipublisher.domain.ScopeConfiguration;
import com.jakefear.aipublisher.domain.Topic;
import com.jakefear.aipublisher.domain.TopicRelationship;
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
 * AI service that analyzes the topic universe to identify gaps in coverage.
 * Detects missing prerequisites, orphaned topics, and unexplored areas.
 */
@Component
public class GapAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(GapAnalyzer.class);

    private final ChatLanguageModel model;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
        You are an expert knowledge architect analyzing a wiki's topic coverage.
        Your task is to identify gaps - missing topics, concepts, or connections that would improve the wiki.

        Types of gaps to look for:
        1. MISSING_PREREQUISITE - A topic references concepts not covered elsewhere
        2. COVERAGE_GAP - An important area of the domain lacks sufficient topics
        3. ORPHAN_TOPIC - A topic has no connections to others
        4. DEPTH_IMBALANCE - Some areas have too much/little detail compared to others
        5. MISSING_PRACTICAL - Theory exists but no hands-on content
        6. TERMINOLOGY_GAP - Terms are used but not defined

        For each gap, provide:
        - A clear description of what's missing
        - The severity (critical/moderate/minor)
        - Suggested resolution (new topic, glossary entry, or merge with existing)

        Always respond with valid JSON in the specified format.
        """;

    public GapAnalyzer(@Qualifier("researchChatModel") ChatLanguageModel model) {
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyze the topic universe for coverage gaps.
     *
     * @param topics All topics in the universe
     * @param relationships Existing relationships
     * @param scope Scope configuration for context
     * @return Analysis result with identified gaps
     */
    public GapAnalysisResult analyzeGaps(
            List<Topic> topics,
            List<TopicRelationship> relationships,
            ScopeConfiguration scope) {

        log.info("Analyzing {} topics for coverage gaps", topics.size());

        String prompt = buildAnalysisPrompt(topics, relationships, scope);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseResponse(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse gap analysis: {}", e.getMessage());
            return GapAnalysisResult.empty();
        }
    }

    /**
     * Quick check for obvious gaps after adding a new topic.
     *
     * @param newTopic The topic just added
     * @param existingTopics Other topics in the universe
     * @return List of gap descriptions
     */
    public List<String> quickGapCheck(Topic newTopic, List<Topic> existingTopics) {
        log.info("Quick gap check for topic: {}", newTopic.name());

        String prompt = buildQuickCheckPrompt(newTopic, existingTopics);
        String response = model.generate(SYSTEM_PROMPT + "\n\n---\n\n" + prompt);

        try {
            return parseQuickCheckResponse(response);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse quick gap check: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildAnalysisPrompt(
            List<Topic> topics,
            List<TopicRelationship> relationships,
            ScopeConfiguration scope) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("## Domain Context\n");
        if (scope != null && scope.domainDescription() != null) {
            prompt.append(scope.domainDescription()).append("\n\n");
        }

        prompt.append("## Current Topics (").append(topics.size()).append(" total)\n");
        for (Topic topic : topics) {
            prompt.append("- **").append(topic.name()).append("** (")
                    .append(topic.complexity().name()).append("): ")
                    .append(topic.description()).append("\n");
        }
        prompt.append("\n");

        if (!relationships.isEmpty()) {
            prompt.append("## Existing Relationships (").append(relationships.size()).append(" total)\n");
            for (TopicRelationship rel : relationships) {
                prompt.append("- ").append(rel.sourceTopicId())
                        .append(" --[").append(rel.type().name()).append("]--> ")
                        .append(rel.targetTopicId()).append("\n");
            }
            prompt.append("\n");
        }

        if (scope != null) {
            prompt.append("## Scope\n");
            if (!scope.focusAreas().isEmpty()) {
                prompt.append("Focus areas: ").append(String.join(", ", scope.focusAreas())).append("\n");
            }
            if (scope.audienceDescription() != null) {
                prompt.append("Audience: ").append(scope.audienceDescription()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("""
            ## Task
            Analyze this topic universe and identify gaps in coverage.
            Consider:
            - Are there missing foundational topics?
            - Are all topics well-connected?
            - Is there balance between theory and practice?
            - Are important concepts referenced but not explained?
            - Are there areas that need more depth?

            Respond with JSON:
            ```json
            {
              "gaps": [
                {
                  "type": "MISSING_PREREQUISITE|COVERAGE_GAP|ORPHAN_TOPIC|DEPTH_IMBALANCE|MISSING_PRACTICAL|TERMINOLOGY_GAP",
                  "description": "Clear description of what's missing",
                  "severity": "critical|moderate|minor",
                  "affectedTopics": ["Topic names that reveal this gap"],
                  "suggestedResolution": "How to address this gap",
                  "suggestedTopicName": "Optional: name of new topic to add"
                }
              ],
              "overallAssessment": {
                "coverageScore": 0.75,
                "balanceScore": 0.80,
                "connectednessScore": 0.70,
                "summary": "Brief overall assessment"
              }
            }
            ```
            """);

        return prompt.toString();
    }

    private String buildQuickCheckPrompt(Topic newTopic, List<Topic> existingTopics) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## New Topic Just Added\n");
        prompt.append("Name: ").append(newTopic.name()).append("\n");
        prompt.append("Description: ").append(newTopic.description()).append("\n");
        prompt.append("Complexity: ").append(newTopic.complexity().name()).append("\n\n");

        prompt.append("## Existing Topics\n");
        String topicList = existingTopics.stream()
                .map(t -> "- " + t.name())
                .collect(Collectors.joining("\n"));
        prompt.append(topicList).append("\n\n");

        prompt.append("""
            ## Task
            Quick check: Does adding this topic reveal any obvious gaps?
            Look for:
            - Prerequisites this topic needs that aren't covered
            - Related concepts that should also be included
            - Terms this topic uses that need definition

            Only report significant gaps, not every possible addition.

            Respond with JSON:
            ```json
            {
              "gaps": ["Gap description 1", "Gap description 2"]
            }
            ```
            """);

        return prompt.toString();
    }

    private GapAnalysisResult parseResponse(String response) throws JsonProcessingException {
        JsonNode root = JsonParsingUtils.parseJson(response, objectMapper);

        List<Gap> gaps = new ArrayList<>();
        JsonNode gapsNode = root.get("gaps");
        if (gapsNode != null && gapsNode.isArray()) {
            for (JsonNode node : gapsNode) {
                gaps.add(parseGap(node));
            }
        }

        OverallAssessment assessment = null;
        JsonNode assessmentNode = root.get("overallAssessment");
        if (assessmentNode != null) {
            assessment = new OverallAssessment(
                    JsonParsingUtils.getDoubleOrDefault(assessmentNode, "coverageScore", 0.5),
                    JsonParsingUtils.getDoubleOrDefault(assessmentNode, "balanceScore", 0.5),
                    JsonParsingUtils.getDoubleOrDefault(assessmentNode, "connectednessScore", 0.5),
                    JsonParsingUtils.getStringOrDefault(assessmentNode, "summary", "")
            );
        }

        log.info("Identified {} gaps in topic universe", gaps.size());
        return new GapAnalysisResult(gaps, assessment);
    }

    private Gap parseGap(JsonNode node) {
        GapType type;
        try {
            type = GapType.valueOf(JsonParsingUtils.getStringOrDefault(node, "type", "COVERAGE_GAP"));
        } catch (IllegalArgumentException e) {
            type = GapType.COVERAGE_GAP;
        }

        Severity severity;
        try {
            severity = Severity.valueOf(JsonParsingUtils.getStringOrDefault(node, "severity", "moderate").toUpperCase());
        } catch (IllegalArgumentException e) {
            severity = Severity.MODERATE;
        }

        List<String> affectedTopics = new ArrayList<>();
        JsonNode affected = node.get("affectedTopics");
        if (affected != null && affected.isArray()) {
            for (JsonNode t : affected) {
                affectedTopics.add(t.asText());
            }
        }

        return new Gap(
                type,
                JsonParsingUtils.getStringOrDefault(node, "description", ""),
                severity,
                affectedTopics,
                JsonParsingUtils.getStringOrDefault(node, "suggestedResolution", ""),
                JsonParsingUtils.getStringOrDefault(node, "suggestedTopicName", null)
        );
    }

    private List<String> parseQuickCheckResponse(String response) throws JsonProcessingException {
        JsonNode root = JsonParsingUtils.parseJson(response, objectMapper);

        List<String> gaps = new ArrayList<>();
        JsonNode gapsNode = root.get("gaps");
        if (gapsNode != null && gapsNode.isArray()) {
            for (JsonNode node : gapsNode) {
                gaps.add(node.asText());
            }
        }

        return gaps;
    }

    // Inner types

    public enum GapType {
        MISSING_PREREQUISITE,
        COVERAGE_GAP,
        ORPHAN_TOPIC,
        DEPTH_IMBALANCE,
        MISSING_PRACTICAL,
        TERMINOLOGY_GAP
    }

    public enum Severity {
        CRITICAL, MODERATE, MINOR
    }

    public record Gap(
            GapType type,
            String description,
            Severity severity,
            List<String> affectedTopics,
            String suggestedResolution,
            String suggestedTopicName
    ) {
        public boolean hasSuggestedTopic() {
            return suggestedTopicName != null && !suggestedTopicName.isBlank();
        }
    }

    public record OverallAssessment(
            double coverageScore,
            double balanceScore,
            double connectednessScore,
            String summary
    ) {}

    public record GapAnalysisResult(
            List<Gap> gaps,
            OverallAssessment assessment
    ) {
        public static GapAnalysisResult empty() {
            return new GapAnalysisResult(List.of(), null);
        }

        public boolean hasGaps() {
            return !gaps.isEmpty();
        }

        public List<Gap> getCriticalGaps() {
            return gaps.stream()
                    .filter(g -> g.severity() == Severity.CRITICAL)
                    .toList();
        }

        public List<Gap> getGapsWithSuggestedTopics() {
            return gaps.stream()
                    .filter(Gap::hasSuggestedTopic)
                    .toList();
        }
    }
}
