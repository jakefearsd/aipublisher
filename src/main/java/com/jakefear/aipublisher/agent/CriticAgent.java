package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jakefear.aipublisher.agent.JsonParsingUtils.*;

/**
 * Critic Agent: Reviews article quality, structure, and JSPWiki syntax before publication.
 *
 * Input: FinalArticle (from EditorAgent)
 * Output: CriticReport
 */
@Component
public class CriticAgent extends BaseAgent {

    /**
     * Default constructor for Spring - uses setter injection.
     */
    public CriticAgent() {
        super(AgentPrompts.CRITIC);
    }

    /**
     * Set the chat model (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setChatModel(@Qualifier("criticChatModel") ChatLanguageModel model) {
        this.model = model;
    }

    // Constructor for testing
    public CriticAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.CRITIC;
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        FinalArticle article = document.getFinalArticle();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please review the following article for quality and JSPWiki syntax compliance:\n\n");

        // Article content
        prompt.append("--- ARTICLE CONTENT ---\n\n");
        prompt.append(article.markdownContent());
        prompt.append("\n\n");

        // Article metadata
        prompt.append("--- ARTICLE METADATA ---\n");
        prompt.append("Title: ").append(article.metadata().title()).append("\n");
        prompt.append("Word count: ").append(article.estimateWordCount()).append("\n");
        prompt.append("Editor quality score: ").append(String.format("%.2f", article.qualityScore())).append("\n");

        if (!article.addedLinks().isEmpty()) {
            prompt.append("Internal links: ").append(String.join(", ", article.addedLinks())).append("\n");
        }

        // Context from topic brief
        TopicBrief topicBrief = document.getTopicBrief();
        prompt.append("\n--- ORIGINAL REQUIREMENTS ---\n");
        prompt.append("Topic: ").append(topicBrief.topic()).append("\n");
        if (topicBrief.targetAudience() != null && !topicBrief.targetAudience().isBlank()) {
            prompt.append("Target audience: ").append(topicBrief.targetAudience()).append("\n");
        }
        if (topicBrief.targetWordCount() > 0) {
            prompt.append("Target word count: ").append(topicBrief.targetWordCount()).append("\n");
        }

        prompt.append("\nReview this article thoroughly and provide a detailed critique as JSON.");
        prompt.append("\nPay special attention to JSPWiki syntax - flag any Markdown syntax as critical issues.");

        return prompt.toString();
    }

    @Override
    protected void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException {

        JsonNode root = parseJson(response);

        // Parse scores
        double overallScore = getDoubleOrDefault(root, "overallScore", 0.0);
        double structureScore = getDoubleOrDefault(root, "structureScore", 0.0);
        double syntaxScore = getDoubleOrDefault(root, "syntaxScore", 0.0);
        double readabilityScore = getDoubleOrDefault(root, "readabilityScore", 0.0);

        // Parse issue lists
        List<String> structureIssues = parseStringArray(root, "structureIssues");
        List<String> syntaxIssues = parseStringArray(root, "syntaxIssues");
        List<String> styleIssues = parseStringArray(root, "styleIssues");
        List<String> suggestions = parseStringArray(root, "suggestions");

        // Parse recommended action
        String actionStr = getStringOrDefault(root, "recommendedAction", "REVISE");
        RecommendedAction recommendedAction = RecommendedAction.fromString(actionStr);

        // Create and set the critic report
        CriticReport report = new CriticReport(
                overallScore,
                structureScore,
                syntaxScore,
                readabilityScore,
                structureIssues,
                syntaxIssues,
                styleIssues,
                suggestions,
                recommendedAction
        );

        document.setCriticReport(report);
        log.info("Critic review complete: overall={:.2f}, syntax={:.2f}, recommendation={}",
                overallScore, syntaxScore, recommendedAction);
    }

    @Override
    public boolean validate(PublishingDocument document) {
        CriticReport report = document.getCriticReport();
        if (report == null) {
            log.warn("Validation failed: no critic report");
            return false;
        }

        // Check that we got meaningful scores
        if (report.overallScore() <= 0) {
            log.warn("Validation failed: invalid overall score");
            return false;
        }

        return true;
    }
}
