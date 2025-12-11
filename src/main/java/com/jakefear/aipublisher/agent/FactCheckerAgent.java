package com.jakefear.aipublisher.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.jakefear.aipublisher.document.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.jakefear.aipublisher.agent.JsonParsingUtils.*;

/**
 * Fact Checker Agent: Verifies claims and checks consistency of article content.
 *
 * Input: ArticleDraft (from WriterAgent) + ResearchBrief
 * Output: FactCheckReport
 */
@Component
@Lazy
public class FactCheckerAgent extends BaseAgent {

    public FactCheckerAgent(@Qualifier("factCheckerChatModel") ChatLanguageModel model) {
        super(model, AgentPrompts.FACT_CHECKER);
    }

    // Constructor for testing
    public FactCheckerAgent(ChatLanguageModel model, String systemPrompt) {
        super(model, systemPrompt);
    }

    @Override
    public AgentRole getRole() {
        return AgentRole.FACT_CHECKER;
    }

    @Override
    protected String buildUserPrompt(PublishingDocument document) {
        ArticleDraft draft = document.getDraft();
        ResearchBrief researchBrief = document.getResearchBrief();

        StringBuilder prompt = new StringBuilder();
        prompt.append("Please fact-check the following article against the research brief:\n\n");

        // Article draft
        prompt.append("--- ARTICLE DRAFT ---\n\n");
        prompt.append(draft.markdownContent());
        prompt.append("\n\n");

        // Research brief for verification
        prompt.append("--- RESEARCH BRIEF (for verification) ---\n\n");

        // Key facts
        prompt.append("KEY FACTS:\n");
        for (int i = 0; i < researchBrief.keyFacts().size(); i++) {
            KeyFact fact = researchBrief.keyFacts().get(i);
            prompt.append(i).append(". ").append(fact.fact()).append("\n");
        }

        // Sources with indices for reference
        if (!researchBrief.sources().isEmpty()) {
            prompt.append("\nSOURCES:\n");
            for (int i = 0; i < researchBrief.sources().size(); i++) {
                SourceCitation source = researchBrief.sources().get(i);
                prompt.append(i).append(". ").append(source.description())
                        .append(" (Reliability: ").append(source.reliability()).append(")\n");
            }
        }

        // Glossary for technical term verification
        if (!researchBrief.glossary().isEmpty()) {
            prompt.append("\nGLOSSARY:\n");
            researchBrief.glossary().forEach((term, definition) ->
                    prompt.append("- ").append(term).append(": ").append(definition).append("\n"));
        }

        // Uncertain areas to pay special attention to
        if (!researchBrief.uncertainAreas().isEmpty()) {
            prompt.append("\nAREAS OF UNCERTAINTY (verify carefully):\n");
            for (String area : researchBrief.uncertainAreas()) {
                prompt.append("- ").append(area).append("\n");
            }
        }

        prompt.append("\nAnalyze every factual claim and produce a fact-check report as JSON.");

        return prompt.toString();
    }

    @Override
    protected void parseAndApplyResponse(String response, PublishingDocument document)
            throws JsonProcessingException {

        JsonNode root = parseJson(response);

        // Parse verified claims
        List<VerifiedClaim> verifiedClaims = parseVerifiedClaims(root);

        // Parse questionable claims
        List<QuestionableClaim> questionableClaims = parseQuestionableClaims(root);

        // Parse consistency issues
        List<String> consistencyIssues = parseStringArray(root, "consistencyIssues");

        // Parse overall confidence
        String confidenceStr = getStringOrDefault(root, "overallConfidence", "MEDIUM");
        ConfidenceLevel overallConfidence = ConfidenceLevel.fromString(confidenceStr);

        // Parse recommended action
        String actionStr = getStringOrDefault(root, "recommendedAction", "APPROVE");
        RecommendedAction recommendedAction = RecommendedAction.fromString(actionStr);

        // Create and set the fact-check report
        // Use the original draft content as annotatedContent since we don't modify it
        FactCheckReport report = new FactCheckReport(
                document.getDraft().markdownContent(),
                verifiedClaims,
                questionableClaims,
                consistencyIssues,
                overallConfidence,
                recommendedAction
        );

        document.setFactCheckReport(report);
        log.info("Fact-check completed: {} verified, {} questionable, {} consistency issues. " +
                        "Confidence: {}, Recommendation: {}",
                verifiedClaims.size(), questionableClaims.size(), consistencyIssues.size(),
                overallConfidence, recommendedAction);
    }

    @Override
    public boolean validate(PublishingDocument document) {
        FactCheckReport report = document.getFactCheckReport();
        if (report == null) {
            log.warn("Validation failed: no fact-check report");
            return false;
        }

        // Check that fact-checking actually occurred (should have verified some claims)
        if (report.verifiedClaims().isEmpty() && report.questionableClaims().isEmpty()) {
            log.warn("Validation failed: no claims were checked");
            return false;
        }

        return true;
    }

    private List<VerifiedClaim> parseVerifiedClaims(JsonNode root) {
        List<VerifiedClaim> claims = new ArrayList<>();
        JsonNode claimsNode = root.get("verifiedClaims");

        if (claimsNode != null && claimsNode.isArray()) {
            for (JsonNode claimNode : claimsNode) {
                String claim = getStringOrDefault(claimNode, "claim", "");
                String status = getStringOrDefault(claimNode, "status", "VERIFIED");
                int sourceIndex = getIntOrDefault(claimNode, "sourceIndex", -1);

                if (!claim.isBlank()) {
                    claims.add(new VerifiedClaim(claim, status, sourceIndex));
                }
            }
        }

        return claims;
    }

    private List<QuestionableClaim> parseQuestionableClaims(JsonNode root) {
        List<QuestionableClaim> claims = new ArrayList<>();
        JsonNode claimsNode = root.get("questionableClaims");

        if (claimsNode != null && claimsNode.isArray()) {
            for (JsonNode claimNode : claimsNode) {
                String claim = getStringOrDefault(claimNode, "claim", "");
                String issue = getStringOrDefault(claimNode, "issue", "Unspecified issue");
                String suggestion = getStringOrDefault(claimNode, "suggestion", "");

                if (!claim.isBlank()) {
                    claims.add(new QuestionableClaim(claim, issue, suggestion));
                }
            }
        }

        return claims;
    }

}
