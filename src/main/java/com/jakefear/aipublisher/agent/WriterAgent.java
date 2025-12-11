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
 * Writer Agent: Transforms research into well-structured wiki articles.
 *
 * Input: ResearchBrief (from ResearchAgent)
 * Output: ArticleDraft
 */
@Component
public class WriterAgent extends BaseAgent {

    public WriterAgent(@Qualifier("writerChatModel") ChatLanguageModel model) {
        super(model, AgentPrompts.WRITER);
    }

    // Constructor for testing
    public WriterAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.WRITER;
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        TopicBrief topicBrief = document.getTopicBrief();
        ResearchBrief researchBrief = document.getResearchBrief();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please write a wiki article based on the following research:\n\n");

        // Topic information
        prompt.append("TOPIC: ").append(topicBrief.topic()).append("\n");
        prompt.append("PAGE NAME: ").append(document.getPageName()).append("\n");

        if (topicBrief.targetAudience() != null && !topicBrief.targetAudience().isBlank()) {
            prompt.append("TARGET AUDIENCE: ").append(topicBrief.targetAudience()).append("\n");
        }

        if (topicBrief.targetWordCount() > 0) {
            prompt.append("TARGET LENGTH: approximately ").append(topicBrief.targetWordCount()).append(" words\n");
        }

        // Research brief content
        prompt.append("\n--- RESEARCH BRIEF ---\n\n");

        // Key facts
        prompt.append("KEY FACTS:\n");
        for (KeyFact fact : researchBrief.keyFacts()) {
            prompt.append("- ").append(fact.fact()).append("\n");
        }

        // Suggested outline
        if (!researchBrief.suggestedOutline().isEmpty()) {
            prompt.append("\nSUGGESTED OUTLINE:\n");
            for (int i = 0; i < researchBrief.suggestedOutline().size(); i++) {
                prompt.append(i + 1).append(". ").append(researchBrief.suggestedOutline().get(i)).append("\n");
            }
        }

        // Related pages for linking
        List<String> relatedPages = new ArrayList<>();
        relatedPages.addAll(topicBrief.relatedPages());
        relatedPages.addAll(researchBrief.relatedPageSuggestions());
        if (!relatedPages.isEmpty()) {
            prompt.append("\nRELATED PAGES (for internal links):\n");
            for (String page : relatedPages) {
                prompt.append("- ").append(page).append("\n");
            }
        }

        // Glossary
        if (!researchBrief.glossary().isEmpty()) {
            prompt.append("\nGLOSSARY:\n");
            for (Map.Entry<String, String> entry : researchBrief.glossary().entrySet()) {
                prompt.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        // Sources for attribution
        if (!researchBrief.sources().isEmpty()) {
            prompt.append("\nSOURCES:\n");
            for (int i = 0; i < researchBrief.sources().size(); i++) {
                SourceCitation source = researchBrief.sources().get(i);
                prompt.append(i).append(". ").append(source.description())
                        .append(" (").append(source.reliability()).append(")\n");
            }
        }

        // Required sections
        if (!topicBrief.requiredSections().isEmpty()) {
            prompt.append("\nREQUIRED SECTIONS:\n");
            for (String section : topicBrief.requiredSections()) {
                prompt.append("- ").append(section).append("\n");
            }
        }

        // Uncertain areas to be careful about
        if (!researchBrief.uncertainAreas().isEmpty()) {
            prompt.append("\nAREAS OF UNCERTAINTY (be careful with claims here):\n");
            for (String area : researchBrief.uncertainAreas()) {
                prompt.append("- ").append(area).append("\n");
            }
        }

        prompt.append("\nProduce the article as JSON with markdownContent, summary, internalLinks, and categories.");

        return prompt.toString();
    }

    @Override
    protected void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException {

        JsonNode root = parseJson(response);

        // Parse markdown content (required)
        String markdownContent = getStringOrDefault(root, "markdownContent", "");
        if (markdownContent.isBlank()) {
            throw new JsonProcessingException("No markdownContent found in response") {};
        }

        // Parse summary (required)
        String summary = getStringOrDefault(root, "summary", "");
        if (summary.isBlank()) {
            throw new JsonProcessingException("No summary found in response") {};
        }

        // Parse internal links
        List<String> internalLinks = parseStringArray(root, "internalLinks");

        // Parse categories
        List<String> categories = parseStringArray(root, "categories");

        // Parse additional metadata if present
        Map<String, String> metadata = parseStringMap(root, "metadata");

        // Create and set the article draft
        ArticleDraft draft = new ArticleDraft(
                markdownContent,
                summary,
                internalLinks,
                categories,
                metadata
        );

        document.setDraft(draft);
        log.info("Article draft created: {} words, {} internal links, {} categories",
                draft.estimateWordCount(), internalLinks.size(), categories.size());
    }

    @Override
    public boolean validate(PublishingDocument document) {
        ArticleDraft draft = document.getDraft();
        if (draft == null) {
            log.warn("Validation failed: no article draft");
            return false;
        }

        if (!draft.isValid()) {
            log.warn("Validation failed: draft is invalid");
            return false;
        }

        // Check minimum content
        int wordCount = draft.estimateWordCount();
        int targetWordCount = document.getTopicBrief().targetWordCount();

        // Allow some flexibility - at least 50% of target
        if (targetWordCount > 0 && wordCount < targetWordCount * 0.5) {
            log.warn("Validation failed: word count {} is less than 50% of target {}",
                    wordCount, targetWordCount);
            return false;
        }

        return true;
    }

}
