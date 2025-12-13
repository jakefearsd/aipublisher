package com.jakefear.aipublisher.linking;

/**
 * Configuration for link generation strategy.
 */
public class LinkingStrategy {

    /**
     * Target link density as percentage of words.
     * Links typically 3-8% of words for good readability.
     */
    private double minLinkDensity = 0.03;
    private double maxLinkDensity = 0.08;

    /**
     * Maximum number of links per article.
     */
    private int maxLinksPerArticle = 20;

    /**
     * Minimum words between links to avoid over-linking.
     */
    private int minWordsBetweenLinks = 50;

    /**
     * Whether to only link first mentions of terms.
     */
    private boolean firstMentionOnly = true;

    /**
     * Minimum relevance score to include a link.
     */
    private double minRelevanceScore = 0.5;

    /**
     * Whether to prefer linking to popular pages (many inbound links).
     */
    private boolean preferPopularPages = true;

    /**
     * Weight given to first-mention preference in scoring.
     */
    private double firstMentionWeight = 0.4;

    /**
     * Weight given to contextual relevance in scoring.
     */
    private double relevanceWeight = 0.4;

    /**
     * Weight given to page popularity in scoring.
     */
    private double popularityWeight = 0.2;

    // Getters and setters

    public double getMinLinkDensity() {
        return minLinkDensity;
    }

    public void setMinLinkDensity(double minLinkDensity) {
        this.minLinkDensity = minLinkDensity;
    }

    public double getMaxLinkDensity() {
        return maxLinkDensity;
    }

    public void setMaxLinkDensity(double maxLinkDensity) {
        this.maxLinkDensity = maxLinkDensity;
    }

    public int getMaxLinksPerArticle() {
        return maxLinksPerArticle;
    }

    public void setMaxLinksPerArticle(int maxLinksPerArticle) {
        this.maxLinksPerArticle = maxLinksPerArticle;
    }

    public int getMinWordsBetweenLinks() {
        return minWordsBetweenLinks;
    }

    public void setMinWordsBetweenLinks(int minWordsBetweenLinks) {
        this.minWordsBetweenLinks = minWordsBetweenLinks;
    }

    public boolean isFirstMentionOnly() {
        return firstMentionOnly;
    }

    public void setFirstMentionOnly(boolean firstMentionOnly) {
        this.firstMentionOnly = firstMentionOnly;
    }

    public double getMinRelevanceScore() {
        return minRelevanceScore;
    }

    public void setMinRelevanceScore(double minRelevanceScore) {
        this.minRelevanceScore = minRelevanceScore;
    }

    public boolean isPreferPopularPages() {
        return preferPopularPages;
    }

    public void setPreferPopularPages(boolean preferPopularPages) {
        this.preferPopularPages = preferPopularPages;
    }

    public double getFirstMentionWeight() {
        return firstMentionWeight;
    }

    public void setFirstMentionWeight(double firstMentionWeight) {
        this.firstMentionWeight = firstMentionWeight;
    }

    public double getRelevanceWeight() {
        return relevanceWeight;
    }

    public void setRelevanceWeight(double relevanceWeight) {
        this.relevanceWeight = relevanceWeight;
    }

    public double getPopularityWeight() {
        return popularityWeight;
    }

    public void setPopularityWeight(double popularityWeight) {
        this.popularityWeight = popularityWeight;
    }

    /**
     * Calculate target number of links for a given word count.
     */
    public int calculateTargetLinkCount(int wordCount) {
        double targetDensity = (minLinkDensity + maxLinkDensity) / 2;
        int target = (int) Math.round(wordCount * targetDensity);
        return Math.min(target, maxLinksPerArticle);
    }

    /**
     * Check if a link density is within acceptable range.
     */
    public boolean isLinkDensityAcceptable(int linkCount, int wordCount) {
        if (wordCount <= 0) return linkCount == 0;
        double density = (double) linkCount / wordCount;
        return density >= minLinkDensity && density <= maxLinkDensity;
    }

    /**
     * Create a default strategy.
     */
    public static LinkingStrategy defaults() {
        return new LinkingStrategy();
    }

    /**
     * Create a minimal linking strategy (fewer links).
     */
    public static LinkingStrategy minimal() {
        LinkingStrategy strategy = new LinkingStrategy();
        strategy.setMinLinkDensity(0.01);
        strategy.setMaxLinkDensity(0.03);
        strategy.setMaxLinksPerArticle(10);
        strategy.setMinRelevanceScore(0.7);
        return strategy;
    }

    /**
     * Create an aggressive linking strategy (more links).
     */
    public static LinkingStrategy aggressive() {
        LinkingStrategy strategy = new LinkingStrategy();
        strategy.setMinLinkDensity(0.05);
        strategy.setMaxLinkDensity(0.12);
        strategy.setMaxLinksPerArticle(30);
        strategy.setMinRelevanceScore(0.3);
        strategy.setMinWordsBetweenLinks(30);
        return strategy;
    }
}
