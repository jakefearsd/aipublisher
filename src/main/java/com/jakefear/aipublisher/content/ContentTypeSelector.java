package com.jakefear.aipublisher.content;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Selects appropriate content type based on topic analysis.
 */
@Component
public class ContentTypeSelector {

    // Patterns for detecting content type from topic
    private static final List<Pattern> TUTORIAL_PATTERNS = List.of(
            Pattern.compile("(?i)\\bhow\\s+to\\b"),
            Pattern.compile("(?i)\\bgetting\\s+started\\b"),
            Pattern.compile("(?i)\\btutorial\\b"),
            Pattern.compile("(?i)\\bstep[- ]by[- ]step\\b"),
            Pattern.compile("(?i)\\bguide\\s+to\\s+(setting|creating|building|making)\\b"),
            Pattern.compile("(?i)\\byour\\s+first\\b"),
            Pattern.compile("(?i)\\bbeginners?\\s+guide\\b"),
            Pattern.compile("(?i)\\blearn(ing)?\\s+to\\b")
    );

    private static final List<Pattern> COMPARISON_PATTERNS = List.of(
            Pattern.compile("(?i)\\bvs\\.?\\b"),
            Pattern.compile("(?i)\\bversus\\b"),
            Pattern.compile("(?i)\\bcompare\\b"),
            Pattern.compile("(?i)\\bcomparison\\b"),
            Pattern.compile("(?i)\\bdifference(s)?\\s+between\\b"),
            Pattern.compile("(?i)\\bwhich\\s+(one|is\\s+better)\\b"),
            Pattern.compile("(?i)\\bor\\b.*\\bwhich\\b")
    );

    private static final List<Pattern> TROUBLESHOOTING_PATTERNS = List.of(
            Pattern.compile("(?i)\\btroubleshooting\\b"),
            Pattern.compile("(?i)\\bfix(ing)?\\b"),
            Pattern.compile("(?i)\\bsolv(e|ing)\\b"),
            Pattern.compile("(?i)\\berror(s)?\\b"),
            Pattern.compile("(?i)\\bproblem(s)?\\b"),
            Pattern.compile("(?i)\\bissue(s)?\\b"),
            Pattern.compile("(?i)\\bdoesn'?t\\s+work\\b"),
            Pattern.compile("(?i)\\bnot\\s+working\\b"),
            Pattern.compile("(?i)\\bfailed\\b"),
            Pattern.compile("(?i)\\bwhy\\s+(is|does|doesn'?t)\\b")
    );

    private static final List<Pattern> REFERENCE_PATTERNS = List.of(
            Pattern.compile("(?i)\\breference\\b"),
            Pattern.compile("(?i)\\bapi\\b"),
            Pattern.compile("(?i)\\bsyntax\\b"),
            Pattern.compile("(?i)\\bspecification\\b"),
            Pattern.compile("(?i)\\bcheat\\s*sheet\\b"),
            Pattern.compile("(?i)\\bquick\\s+reference\\b"),
            Pattern.compile("(?i)\\bcommand(s)?\\s+list\\b"),
            Pattern.compile("(?i)\\boptions\\s+list\\b")
    );

    private static final List<Pattern> GUIDE_PATTERNS = List.of(
            Pattern.compile("(?i)\\bbest\\s+practices?\\b"),
            Pattern.compile("(?i)\\bwhen\\s+to\\s+use\\b"),
            Pattern.compile("(?i)\\bchoosing\\b"),
            Pattern.compile("(?i)\\bselecting\\b"),
            Pattern.compile("(?i)\\bdecision\\b"),
            Pattern.compile("(?i)\\bstrategy\\b"),
            Pattern.compile("(?i)\\brecommendations?\\b"),
            Pattern.compile("(?i)\\bguide\\s+to\\s+(choosing|selecting|deciding)\\b")
    );

    private static final List<Pattern> OVERVIEW_PATTERNS = List.of(
            Pattern.compile("(?i)\\boverview\\b"),
            Pattern.compile("(?i)\\bintroduction\\s+to\\b"),
            Pattern.compile("(?i)\\bintro\\s+to\\b"),
            Pattern.compile("(?i)\\bwhat\\s+is\\b"),
            Pattern.compile("(?i)\\bunderstanding\\b"),
            Pattern.compile("(?i)\\bfundamentals\\b"),
            Pattern.compile("(?i)\\bbasics\\s+of\\b")
    );

    /**
     * Detect the most appropriate content type from the topic string.
     *
     * @param topic The topic to analyze
     * @return The detected content type, or CONCEPT as default
     */
    public ContentType detectFromTopic(String topic) {
        if (topic == null || topic.isBlank()) {
            return ContentType.CONCEPT;
        }

        // Check patterns in order of specificity
        if (matchesAny(topic, TUTORIAL_PATTERNS)) {
            return ContentType.TUTORIAL;
        }
        if (matchesAny(topic, COMPARISON_PATTERNS)) {
            return ContentType.COMPARISON;
        }
        if (matchesAny(topic, TROUBLESHOOTING_PATTERNS)) {
            return ContentType.TROUBLESHOOTING;
        }
        if (matchesAny(topic, REFERENCE_PATTERNS)) {
            return ContentType.REFERENCE;
        }
        if (matchesAny(topic, GUIDE_PATTERNS)) {
            return ContentType.GUIDE;
        }
        if (matchesAny(topic, OVERVIEW_PATTERNS)) {
            return ContentType.OVERVIEW;
        }

        // Default to CONCEPT
        return ContentType.CONCEPT;
    }

    /**
     * Recommend a content type with confidence score.
     */
    public ContentTypeRecommendation recommend(String topic) {
        ContentType detected = detectFromTopic(topic);
        double confidence = calculateConfidence(topic, detected);
        String rationale = generateRationale(topic, detected);

        return new ContentTypeRecommendation(detected, confidence, rationale);
    }

    /**
     * Result of content type recommendation.
     */
    public record ContentTypeRecommendation(
            ContentType contentType,
            double confidence,
            String rationale
    ) {}

    private boolean matchesAny(String text, List<Pattern> patterns) {
        return patterns.stream().anyMatch(p -> p.matcher(text).find());
    }

    private double calculateConfidence(String topic, ContentType detected) {
        // Count how many patterns match
        List<Pattern> patterns = getPatternsForType(detected);
        if (patterns.isEmpty()) {
            return 0.5; // Default confidence for CONCEPT
        }

        long matches = patterns.stream()
                .filter(p -> p.matcher(topic).find())
                .count();

        // More matches = higher confidence
        return Math.min(0.5 + (matches * 0.15), 0.95);
    }

    private List<Pattern> getPatternsForType(ContentType type) {
        return switch (type) {
            case TUTORIAL -> TUTORIAL_PATTERNS;
            case COMPARISON -> COMPARISON_PATTERNS;
            case TROUBLESHOOTING -> TROUBLESHOOTING_PATTERNS;
            case REFERENCE -> REFERENCE_PATTERNS;
            case GUIDE -> GUIDE_PATTERNS;
            case OVERVIEW -> OVERVIEW_PATTERNS;
            case CONCEPT, DEFINITION -> List.of();
        };
    }

    private String generateRationale(String topic, ContentType detected) {
        return switch (detected) {
            case TUTORIAL -> "Topic suggests a step-by-step instructional format";
            case COMPARISON -> "Topic involves comparing multiple options or alternatives";
            case TROUBLESHOOTING -> "Topic relates to solving problems or fixing issues";
            case REFERENCE -> "Topic is best suited for quick-lookup reference format";
            case GUIDE -> "Topic involves decision-making or best practices";
            case OVERVIEW -> "Topic is a high-level introduction to a subject area";
            case CONCEPT -> "Topic is best explained as a concept with definition and examples";
            case DEFINITION -> "Brief definition page for a term or concept";
        };
    }
}
