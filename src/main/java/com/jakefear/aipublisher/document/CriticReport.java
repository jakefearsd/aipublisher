package com.jakefear.aipublisher.document;

import java.util.List;

/**
 * Report from the Critic Agent reviewing article quality before publication.
 *
 * @param overallScore Quality score from 0.0 to 1.0
 * @param structureScore Score for article structure and organization
 * @param syntaxScore Score for JSPWiki syntax correctness
 * @param readabilityScore Score for readability and clarity
 * @param structureIssues Issues found with article structure
 * @param syntaxIssues Issues found with JSPWiki syntax (e.g., Markdown instead of JSPWiki)
 * @param styleIssues Issues found with writing style
 * @param suggestions Improvement suggestions
 * @param recommendedAction Recommended action: PUBLISH, REVISE, or REWORK
 */
public record CriticReport(
        double overallScore,
        double structureScore,
        double syntaxScore,
        double readabilityScore,
        List<String> structureIssues,
        List<String> syntaxIssues,
        List<String> styleIssues,
        List<String> suggestions,
        RecommendedAction recommendedAction
) {
    /**
     * Check if the article is approved for publication.
     */
    public boolean isApproved() {
        return recommendedAction == RecommendedAction.APPROVE;
    }

    /**
     * Check if the article needs revision (minor issues).
     */
    public boolean needsRevision() {
        return recommendedAction == RecommendedAction.REVISE;
    }

    /**
     * Check if the article needs major rework.
     */
    public boolean needsRework() {
        return recommendedAction == RecommendedAction.REJECT;
    }

    /**
     * Check if the article meets a minimum quality threshold.
     */
    public boolean meetsQualityThreshold(double threshold) {
        return overallScore >= threshold;
    }

    /**
     * Check if there are any critical syntax issues (Markdown detected).
     */
    public boolean hasSyntaxIssues() {
        return syntaxScore < 0.8 || !syntaxIssues.isEmpty();
    }

    /**
     * Check if the issues are primarily syntax-related (Markdown, formatting).
     * If true, these can likely be fixed automatically without LLM intervention.
     */
    public boolean hasPrimarilySyntaxIssues() {
        // Syntax issues exist and are the main problem
        if (syntaxIssues.isEmpty()) {
            return false;
        }

        // If there are significant structure or style issues, they need LLM attention
        boolean hasSignificantOtherIssues = structureIssues.size() > 1 || styleIssues.size() > 2;

        return !hasSignificantOtherIssues;
    }

    /**
     * Get a summary of all issues found.
     */
    public String getIssueSummary() {
        int totalIssues = structureIssues.size() + syntaxIssues.size() + styleIssues.size();
        return String.format("%d issues found (structure: %d, syntax: %d, style: %d)",
                totalIssues, structureIssues.size(), syntaxIssues.size(), styleIssues.size());
    }
}
