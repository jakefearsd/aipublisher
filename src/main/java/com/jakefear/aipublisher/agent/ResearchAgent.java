package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.search.SearchResult;
import com.jakefear.aipublisher.search.WikipediaSearchService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jakefear.aipublisher.util.JsonParsingUtils.*;

/**
 * Research Agent: Gathers and synthesizes source material for article creation.
 * Now enhanced with web search capabilities for current, verifiable information.
 *
 * Input: TopicBrief
 * Output: ResearchBrief
 */
@Component
public class ResearchAgent extends BaseAgent {

    private WikipediaSearchService wikipediaSearchService;

    /**
     * Default constructor for Spring - uses setter injection.
     */
    public ResearchAgent() {
        super(AgentPrompts.RESEARCH);
    }

    /**
     * Set the chat model (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setChatModel(@Qualifier("researchChatModel") ChatLanguageModel model) {
        this.model = model;
    }

    /**
     * Set the Wikipedia search service (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setWikipediaSearchService(WikipediaSearchService wikipediaSearchService) {
        this.wikipediaSearchService = wikipediaSearchService;
    }

    // Constructor for testing
    public ResearchAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    // Constructor for testing with Wikipedia search
    public ResearchAgent(ChatLanguageModel model, String systemPrompt, WikipediaSearchService wikipediaSearchService) {
        super(model, systemPrompt);
        this.wikipediaSearchService = wikipediaSearchService;
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.RESEARCHER;
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        TopicBrief brief = document.getTopicBrief();

        StringBuilder prompt = new StringBuilder();

        // Include rich domain context if available
        String formattedContext = brief.getFormattedContext();
        if (!formattedContext.isBlank()) {
            prompt.append(formattedContext);
            prompt.append("\n---\n\n");
        }

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

        // Add Wikipedia search results if available
        List<SearchResult> searchResults = performWikipediaSearch(brief.topic());
        if (!searchResults.isEmpty()) {
            prompt.append("\n--- WIKIPEDIA SEARCH RESULTS ---\n");
            prompt.append("The following are Wikipedia articles about this topic.\n");
            prompt.append("Use these as authoritative sources for encyclopedic information.\n");
            prompt.append("Cite Wikipedia URLs when using information from these sources.\n\n");
            for (SearchResult result : searchResults) {
                prompt.append(result.toPromptFormat());
            }
            prompt.append("\n");
        }

        prompt.append("\nProduce a comprehensive research brief as JSON.");
        prompt.append("\nInclude URLs from the search results in your sources where applicable.");

        return prompt.toString();
    }

    /**
     * Perform Wikipedia search for the topic.
     */
    private List<SearchResult> performWikipediaSearch(String topic) {
        if (wikipediaSearchService == null || !wikipediaSearchService.isEnabled()) {
            return List.of();
        }
        try {
            List<SearchResult> results = wikipediaSearchService.search(topic);
            log.debug("Wikipedia search for '{}' returned {} results", topic, results.size());
            return results;
        } catch (Exception e) {
            log.warn("Wikipedia search failed for topic '{}': {}", topic, e.getMessage());
            return List.of();
        }
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
