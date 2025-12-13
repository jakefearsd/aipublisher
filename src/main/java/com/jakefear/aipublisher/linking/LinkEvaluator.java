package com.jakefear.aipublisher.linking;

import com.jakefear.aipublisher.glossary.GlossaryEntry;
import com.jakefear.aipublisher.glossary.GlossaryService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Evaluates and scores potential links in article content.
 */
@Component
public class LinkEvaluator {

    private final GlossaryService glossaryService;
    private final LinkingStrategy strategy;

    public LinkEvaluator(GlossaryService glossaryService) {
        this.glossaryService = glossaryService;
        this.strategy = LinkingStrategy.defaults();
    }

    public LinkEvaluator(GlossaryService glossaryService, LinkingStrategy strategy) {
        this.glossaryService = glossaryService;
        this.strategy = strategy;
    }

    /**
     * Find all link candidates in the given content.
     *
     * @param content The article content
     * @param wikiContext Context about existing wiki pages
     * @return List of link candidates, sorted by position
     */
    public List<LinkCandidate> findCandidates(String content, WikiLinkContext wikiContext) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<LinkCandidate> candidates = new ArrayList<>();
        Set<String> seenTerms = new HashSet<>();

        // Find mentions of existing wiki pages
        for (String pageName : wikiContext.getAllPages()) {
            List<LinkCandidate> pageCandidates = findPageMentions(content, pageName, seenTerms, wikiContext);
            candidates.addAll(pageCandidates);
        }

        // Find mentions of glossary terms that map to pages
        List<GlossaryEntry> glossaryEntries = glossaryService.findTermsInText(content);
        for (GlossaryEntry entry : glossaryEntries) {
            String targetPage = entry.getWikiLink();
            if (wikiContext.pageExists(targetPage) && !seenTerms.contains(targetPage.toLowerCase())) {
                List<LinkCandidate> termCandidates = findTermMentions(content, entry, targetPage, seenTerms, wikiContext);
                candidates.addAll(termCandidates);
            }
        }

        // Sort by position
        candidates.sort(Comparator.comparingInt(LinkCandidate::position));

        return candidates;
    }

    /**
     * Select the best links from candidates based on strategy.
     *
     * @param candidates All link candidates
     * @param wordCount Total words in the article
     * @return Selected links to include
     */
    public List<LinkCandidate> selectBestLinks(List<LinkCandidate> candidates, int wordCount) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        int targetCount = strategy.calculateTargetLinkCount(wordCount);
        List<LinkCandidate> selected = new ArrayList<>();
        Set<String> linkedPages = new HashSet<>();
        int lastLinkPosition = -1000;

        // Score and sort candidates
        List<LinkCandidate> scored = candidates.stream()
                .filter(c -> c.relevanceScore() >= strategy.getMinRelevanceScore())
                .filter(c -> !strategy.isFirstMentionOnly() || c.firstMention())
                .sorted((a, b) -> Double.compare(b.relevanceScore(), a.relevanceScore()))
                .collect(Collectors.toList());

        // Select links ensuring spacing
        for (LinkCandidate candidate : scored) {
            if (selected.size() >= targetCount) break;
            if (linkedPages.contains(candidate.targetPage())) continue;

            // Estimate position in words (rough approximation)
            int estimatedWordPosition = estimateWordPosition(candidate.position(), wordCount);
            int wordsSinceLastLink = estimatedWordPosition - lastLinkPosition;

            if (wordsSinceLastLink >= strategy.getMinWordsBetweenLinks()) {
                selected.add(candidate);
                linkedPages.add(candidate.targetPage());
                lastLinkPosition = estimatedWordPosition;
            }
        }

        // Sort selected links by position for insertion
        selected.sort(Comparator.comparingInt(LinkCandidate::position));

        return selected;
    }

    /**
     * Calculate the link density for given links and word count.
     */
    public double calculateLinkDensity(int linkCount, int wordCount) {
        if (wordCount <= 0) return 0.0;
        return (double) linkCount / wordCount;
    }

    /**
     * Check if the current link density is acceptable.
     */
    public boolean isLinkDensityAcceptable(int linkCount, int wordCount) {
        return strategy.isLinkDensityAcceptable(linkCount, wordCount);
    }

    /**
     * Find mentions of a wiki page in content.
     */
    private List<LinkCandidate> findPageMentions(String content, String pageName,
                                                  Set<String> seenTerms, WikiLinkContext wikiContext) {
        List<LinkCandidate> candidates = new ArrayList<>();

        // Convert CamelCase to space-separated for matching
        String searchTerm = camelCaseToWords(pageName);
        String lowerContent = content.toLowerCase();
        String lowerSearch = searchTerm.toLowerCase();

        int pos = 0;
        boolean firstFound = false;
        while ((pos = lowerContent.indexOf(lowerSearch, pos)) != -1) {
            // Check if this is a word boundary match
            if (isWordBoundary(content, pos, searchTerm.length())) {
                boolean isFirst = !firstFound && !seenTerms.contains(pageName.toLowerCase());
                if (isFirst) {
                    firstFound = true;
                    seenTerms.add(pageName.toLowerCase());
                }

                double score = calculateScore(isFirst, pageName, wikiContext);
                String context = extractContext(content, pos, 100);

                candidates.add(new LinkCandidate(
                        pageName,
                        content.substring(pos, pos + searchTerm.length()),
                        pos,
                        isFirst,
                        context,
                        score
                ));
            }
            pos += searchTerm.length();
        }

        return candidates;
    }

    /**
     * Find mentions of a glossary term in content.
     */
    private List<LinkCandidate> findTermMentions(String content, GlossaryEntry entry, String targetPage,
                                                  Set<String> seenTerms, WikiLinkContext wikiContext) {
        List<LinkCandidate> candidates = new ArrayList<>();

        String term = entry.term();
        String lowerContent = content.toLowerCase();
        String lowerTerm = term.toLowerCase();

        int pos = 0;
        boolean firstFound = false;
        while ((pos = lowerContent.indexOf(lowerTerm, pos)) != -1) {
            if (isWordBoundary(content, pos, term.length())) {
                boolean isFirst = !firstFound && !seenTerms.contains(targetPage.toLowerCase());
                if (isFirst) {
                    firstFound = true;
                    seenTerms.add(targetPage.toLowerCase());
                }

                double score = calculateScore(isFirst, targetPage, wikiContext);
                String context = extractContext(content, pos, 100);

                candidates.add(new LinkCandidate(
                        targetPage,
                        content.substring(pos, pos + term.length()),
                        pos,
                        isFirst,
                        context,
                        score
                ));
            }
            pos += term.length();
        }

        return candidates;
    }

    /**
     * Calculate link score based on strategy weights.
     */
    private double calculateScore(boolean isFirst, String pageName, WikiLinkContext wikiContext) {
        double score = 0.0;

        // First mention component
        if (isFirst) {
            score += strategy.getFirstMentionWeight();
        }

        // Relevance component (base relevance)
        score += strategy.getRelevanceWeight() * 0.5;

        // Popularity component
        if (strategy.isPreferPopularPages()) {
            int inboundCount = wikiContext.getInboundLinkCount(pageName);
            double popularityScore = Math.min(1.0, inboundCount / 10.0);
            score += strategy.getPopularityWeight() * popularityScore;
        }

        return Math.min(1.0, score);
    }

    /**
     * Check if position is at a word boundary.
     */
    private boolean isWordBoundary(String content, int pos, int length) {
        boolean startOk = pos == 0 || !Character.isLetterOrDigit(content.charAt(pos - 1));
        int endPos = pos + length;
        boolean endOk = endPos >= content.length() || !Character.isLetterOrDigit(content.charAt(endPos));
        return startOk && endOk;
    }

    /**
     * Convert CamelCase to space-separated words.
     */
    private String camelCaseToWords(String camelCase) {
        return camelCase.replaceAll("([a-z])([A-Z])", "$1 $2")
                       .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
    }

    /**
     * Extract context around a position.
     */
    private String extractContext(String content, int pos, int radius) {
        int start = Math.max(0, pos - radius);
        int end = Math.min(content.length(), pos + radius);
        return content.substring(start, end);
    }

    /**
     * Estimate word position from character position.
     */
    private int estimateWordPosition(int charPos, int totalWords) {
        // Rough estimate: average word length is about 5 characters
        return charPos / 6;
    }
}
