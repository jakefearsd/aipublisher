package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.linking.LinkCandidate;
import com.jakefear.aipublisher.linking.LinkEvaluator;
import com.jakefear.aipublisher.linking.WikiLinkContext;
import com.jakefear.aipublisher.seealso.SeeAlsoGenerator;
import com.jakefear.aipublisher.seealso.SeeAlsoSection;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jakefear.aipublisher.util.JsonParsingUtils.*;

/**
 * Editor Agent: Polishes content and prepares final publication-ready article.
 *
 * Input: FactCheckReport + ArticleDraft + existing pages list
 * Output: FinalArticle
 */
@Component
public class EditorAgent extends BaseAgent {

    // List of existing wiki pages that can be linked to
    private List<String> existingPages = List.of();

    // Link evaluator for intelligent link suggestions
    private LinkEvaluator linkEvaluator;

    // Wiki link context for page relationships
    private WikiLinkContext wikiLinkContext;

    // See Also generator for related topic suggestions
    private SeeAlsoGenerator seeAlsoGenerator;

    /**
     * Default constructor for Spring - uses setter injection.
     */
    public EditorAgent() {
        super(AgentPrompts.EDITOR);
    }

    /**
     * Set the chat model (called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setChatModel(@Qualifier("editorChatModel") ChatLanguageModel model) {
        this.model = model;
    }

    /**
     * Set the link evaluator (optional, called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setLinkEvaluator(LinkEvaluator linkEvaluator) {
        this.linkEvaluator = linkEvaluator;
    }

    /**
     * Set the See Also generator (optional, called by Spring via @Autowired).
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setSeeAlsoGenerator(SeeAlsoGenerator seeAlsoGenerator) {
        this.seeAlsoGenerator = seeAlsoGenerator;
    }

    // Constructor for testing
    public EditorAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    // Constructor for testing with LinkEvaluator
    public EditorAgent(ChatLanguageModel model, String systemPrompt, LinkEvaluator linkEvaluator) {
        super(model, systemPrompt);
        this.linkEvaluator = linkEvaluator;
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.EDITOR;
    }

    /**
     * Set the list of existing wiki pages for link integration.
     */
    public void setExistingPages(List<String> existingPages) {
        this.existingPages = existingPages == null ? List.of() : List.copyOf(existingPages);
        // Build wiki context from existing pages
        this.wikiLinkContext = new WikiLinkContext();
        for (String page : this.existingPages) {
            wikiLinkContext.registerPage(page);
        }
    }

    /**
     * Set the wiki link context with full page relationship data.
     */
    public void setWikiLinkContext(WikiLinkContext context) {
        this.wikiLinkContext = context;
        // Also update existingPages list from context
        if (context != null) {
            this.existingPages = List.copyOf(context.getAllPages());
        }
    }

    /**
     * Get the list of existing wiki pages.
     */
    public List<String> getExistingPages() {
        return existingPages;
    }

    /**
     * Get suggested links for content using the link evaluator.
     */
    public List<LinkCandidate> getSuggestedLinks(String content) {
        if (linkEvaluator == null || wikiLinkContext == null || content == null) {
            return List.of();
        }

        List<LinkCandidate> candidates = linkEvaluator.findCandidates(content, wikiLinkContext);
        int wordCount = content.split("\\s+").length;
        return linkEvaluator.selectBestLinks(candidates, wordCount);
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        ArticleDraft draft = document.getDraft();
        FactCheckReport factCheckReport = document.getFactCheckReport();
        TopicBrief topicBrief = document.getTopicBrief();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please edit and polish the following article for publication:\n\n");

        // Article draft
        prompt.append("--- ARTICLE DRAFT ---\n\n");
        prompt.append(draft.wikiContent());
        prompt.append("\n\n");

        // Fact-check feedback
        prompt.append("--- FACT-CHECK FEEDBACK ---\n\n");
        prompt.append("Overall Confidence: ").append(factCheckReport.overallConfidence()).append("\n");
        prompt.append("Recommendation: ").append(factCheckReport.recommendedAction()).append("\n");

        // Issues to address
        if (!factCheckReport.questionableClaims().isEmpty()) {
            prompt.append("\nISSUES TO ADDRESS:\n");
            for (QuestionableClaim claim : factCheckReport.questionableClaims()) {
                prompt.append("- Claim: \"").append(claim.claim()).append("\"\n");
                prompt.append("  Issue: ").append(claim.issue()).append("\n");
                if (claim.suggestion() != null && !claim.suggestion().isBlank()) {
                    prompt.append("  Suggestion: ").append(claim.suggestion()).append("\n");
                }
            }
        }

        if (!factCheckReport.consistencyIssues().isEmpty()) {
            prompt.append("\nCONSISTENCY ISSUES:\n");
            for (String issue : factCheckReport.consistencyIssues()) {
                prompt.append("- ").append(issue).append("\n");
            }
        }

        // Intelligent link suggestions from LinkEvaluator
        List<LinkCandidate> suggestedLinks = getSuggestedLinks(draft.wikiContent());
        if (!suggestedLinks.isEmpty()) {
            prompt.append("\n--- SUGGESTED LINKS (prioritized) ---\n");
            prompt.append("These links have been automatically identified as high-value. Consider adding them:\n");
            for (LinkCandidate link : suggestedLinks) {
                prompt.append("- \"").append(link.anchorText()).append("\" -> ")
                      .append(link.toWikiLink())
                      .append(" (score: ").append(String.format("%.2f", link.relevanceScore()));
                if (link.firstMention()) {
                    prompt.append(", first mention");
                }
                prompt.append(")\n");
            }
            prompt.append("\nLink density target: 3-8% of words. Prioritize first mentions.\n");
        } else if (!existingPages.isEmpty()) {
            // Fallback to simple page list if no evaluator
            prompt.append("\n--- EXISTING PAGES (for internal linking) ---\n");
            prompt.append("Add [PageName] links where content naturally references these topics:\n");
            for (String page : existingPages) {
                prompt.append("- ").append(page).append("\n");
            }
        }

        // See Also suggestions
        if (seeAlsoGenerator != null && topicBrief.contentType() != null) {
            SeeAlsoSection seeAlso = seeAlsoGenerator.generate(
                    topicBrief.topic(),
                    topicBrief.contentType(),
                    wikiLinkContext
            );
            if (!seeAlso.isEmpty()) {
                prompt.append("\n--- SEE ALSO SUGGESTIONS ---\n");
                prompt.append("Add a 'See Also' section at the end of the article with these related topics:\n");
                prompt.append(seeAlso.toPromptFormat());
                prompt.append("\nUse this JSPWiki format:\n");
                prompt.append(seeAlso.toWikiText());
            }
        }

        // Target metadata
        prompt.append("\n--- ARTICLE INFORMATION ---\n");
        prompt.append("Topic: ").append(topicBrief.topic()).append("\n");
        prompt.append("Target Audience: ").append(topicBrief.targetAudience()).append("\n");
        prompt.append("Page Name: ").append(document.getPageName()).append("\n");

        prompt.append("\nProduce the final polished article as JSON with wikiContent, metadata, " +
                "editSummary, qualityScore, and addedLinks.");

        return prompt.toString();
    }

    @Override
    protected void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException {

        JsonNode root = parseJson(response);

        // Parse wiki content (required)
        String wikiContent = getStringOrDefault(root, "wikiContent", "");
        if (wikiContent.isBlank()) {
            throw new JsonProcessingException("No wikiContent found in response") {};
        }

        // Parse metadata
        DocumentMetadata metadata = parseMetadata(root, document);

        // Parse edit summary
        String editSummary = getStringOrDefault(root, "editSummary", "Article edited for publication");

        // Parse quality score
        double qualityScore = getDoubleOrDefault(root, "qualityScore", 0.8);
        // Clamp to valid range
        qualityScore = Math.max(0.0, Math.min(1.0, qualityScore));

        // Parse added links
        List<String> addedLinks = parseStringArray(root, "addedLinks");

        // Create and set the final article
        FinalArticle finalArticle = new FinalArticle(
                wikiContent,
                metadata,
                editSummary,
                qualityScore,
                addedLinks
        );

        document.setFinalArticle(finalArticle);
        log.info("Final article prepared: {} words, quality score: {}, {} links added",
                finalArticle.estimateWordCount(), qualityScore, addedLinks.size());
    }

    @Override
    public boolean validate(PublishingDocument document) {
        FinalArticle finalArticle = document.getFinalArticle();
        if (finalArticle == null) {
            log.warn("Validation failed: no final article");
            return false;
        }

        // Check quality score meets minimum (default 0.7)
        if (finalArticle.qualityScore() < 0.7) {
            log.warn("Validation failed: quality score {} is below minimum 0.7",
                    finalArticle.qualityScore());
            return false;
        }

        // Check content exists
        if (finalArticle.wikiContent().isBlank()) {
            log.warn("Validation failed: final article content is empty");
            return false;
        }

        return true;
    }

    private DocumentMetadata parseMetadata(JsonNode root, PublishingDocument document) {
        JsonNode metadataNode = root.get("metadata");

        String title;
        String summary;
        String author;

        if (metadataNode != null && metadataNode.isObject()) {
            title = getStringOrDefault(metadataNode, "title", document.getTopicBrief().topic());
            summary = getStringOrDefault(metadataNode, "summary", "");
            author = getStringOrDefault(metadataNode, "author", "AI Publisher");
        } else {
            // Fallback to document information
            title = document.getTopicBrief().topic();
            summary = document.getDraft() != null ? document.getDraft().summary() : "";
            author = "AI Publisher";
        }

        return DocumentMetadata.create(title, summary);
    }

}
