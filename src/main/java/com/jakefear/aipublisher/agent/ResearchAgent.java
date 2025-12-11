package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jakefear.aipublisher.agent.JsonParsingUtils.*;

/**
 * Research Agent: Gathers and synthesizes source material for article creation.
 *
 * Input: TopicBrief
 * Output: ResearchBrief
 */
@Component
public class ResearchAgent extends BaseAgent {

    public ResearchAgent(@Qualifier("researchChatModel") ChatLanguageModel model) {
        super(model, AgentPrompts.RESEARCH);
    }

    // Constructor for testing
    public ResearchAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.RESEARCHER;
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        TopicBrief brief = document.getTopicBrief();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please research the following topic:\n\n");
        prompt.append("TOPIC: ").append(brief.topic()).append("\n");

        if (brief.targetAudience() != null && !brief.targetAudience().isBlank()) {
            prompt.append("TARGET AUDIENCE: ").append(brief.targetAudience()).append("\n");
        }

        if (brief.targetWordCount() > 0) {
            prompt.append("TARGET ARTICLE LENGTH: approximately ").append(brief.targetWordCount()).append(" words\n");
        }

        if (!brief.requiredSections().isEmpty()) {
            prompt.append("REQUIRED SECTIONS: ").append(String.join(", ", brief.requiredSections())).append("\n");
        }

        if (!brief.relatedPages().isEmpty()) {
            prompt.append("RELATED WIKI PAGES (for linking): ").append(String.join(", ", brief.relatedPages())).append("\n");
        }

        if (!brief.sourceUrls().isEmpty()) {
            prompt.append("SOURCE URLS TO CONSIDER:\n");
            for (String url : brief.sourceUrls()) {
                prompt.append("  - ").append(url).append("\n");
            }
        }

        prompt.append("\nProduce a comprehensive research brief as JSON.");

        return prompt.toString();
    }

    @Override
    protected void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException {

        JsonNode root = parseJson(response);

        // Parse key facts
        List<KeyFact> keyFacts = parseKeyFacts(root);
        if (keyFacts.isEmpty()) {
            throw new JsonProcessingException("No key facts found in response") {};
        }

        // Parse sources
        List<SourceCitation> sources = parseSources(root);

        // Parse suggested outline
        List<String> suggestedOutline = parseStringArray(root, "suggestedOutline");
        if (suggestedOutline.isEmpty()) {
            throw new JsonProcessingException("No suggested outline found in response") {};
        }

        // Parse related pages
        List<String> relatedPages = parseStringArray(root, "relatedPages");

        // Parse glossary
        Map<String, String> glossary = parseStringMap(root, "glossary");

        // Parse uncertain areas
        List<String> uncertainAreas = parseStringArray(root, "uncertainAreas");

        // Create and set the research brief
        ResearchBrief researchBrief = new ResearchBrief(
                keyFacts,
                sources,
                suggestedOutline,
                relatedPages,
                glossary,
                uncertainAreas
        );

        document.setResearchBrief(researchBrief);
        log.info("Research brief created with {} facts, {} sources, {} outline sections",
                keyFacts.size(), sources.size(), suggestedOutline.size());
    }

    @Override
    public boolean validate(PublishingDocument document) {
        ResearchBrief brief = document.getResearchBrief();
        if (brief == null) {
            log.warn("Validation failed: no research brief");
            return false;
        }

        if (!brief.isValid()) {
            log.warn("Validation failed: research brief is incomplete");
            return false;
        }

        // Check minimum content
        if (brief.keyFacts().size() < 3) {
            log.warn("Validation failed: fewer than 3 key facts ({})", brief.keyFacts().size());
            return false;
        }

        if (brief.suggestedOutline().size() < 2) {
            log.warn("Validation failed: fewer than 2 outline sections ({})", brief.suggestedOutline().size());
            return false;
        }

        return true;
    }

    private List<KeyFact> parseKeyFacts(JsonNode root) {
        List<KeyFact> facts = new ArrayList<>();
        JsonNode factsNode = root.get("keyFacts");

        if (factsNode != null && factsNode.isArray()) {
            for (JsonNode factNode : factsNode) {
                String fact = factNode.asText();
                if (fact != null && !fact.isBlank()) {
                    facts.add(KeyFact.unsourced(fact));
                }
            }
        }

        return facts;
    }

    private List<SourceCitation> parseSources(JsonNode root) {
        List<SourceCitation> sources = new ArrayList<>();
        JsonNode sourcesNode = root.get("sources");

        if (sourcesNode != null && sourcesNode.isArray()) {
            for (JsonNode sourceNode : sourcesNode) {
                String description = getStringOrDefault(sourceNode, "description", "Unknown source");
                String reliabilityStr = getStringOrDefault(sourceNode, "reliability", "MEDIUM");
                ConfidenceLevel reliability = ConfidenceLevel.fromString(reliabilityStr);

                sources.add(new SourceCitation(description, reliability));
            }
        }

        return sources;
    }

}
