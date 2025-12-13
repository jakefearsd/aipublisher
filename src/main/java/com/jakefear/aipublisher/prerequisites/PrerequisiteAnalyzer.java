package com.jakefear.aipublisher.prerequisites;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.linking.WikiLinkContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Analyzes topics to determine their prerequisites.
 */
@Component
public class PrerequisiteAnalyzer {

    // Common prerequisite patterns for software development topics
    private static final Map<Pattern, PrerequisiteInfo> PREREQUISITE_PATTERNS = new LinkedHashMap<>();

    static {
        // Framework prerequisites
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bspring\\s*(boot|mvc|security|data)?\\b"),
                new PrerequisiteInfo("Java", "JavaBasics", "Spring is built on Java"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bdjango\\b"),
                new PrerequisiteInfo("Python", "PythonBasics", "Django is a Python framework"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\breact\\b"),
                new PrerequisiteInfo("JavaScript", "JavaScriptBasics", "React is a JavaScript library"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bangular\\b"),
                new PrerequisiteInfo("TypeScript", "TypeScriptBasics", "Angular uses TypeScript"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bvue\\b"),
                new PrerequisiteInfo("JavaScript", "JavaScriptBasics", "Vue is a JavaScript framework"));

        // Advanced topic prerequisites
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bmicroservices?\\b"),
                new PrerequisiteInfo("Distributed Systems", "DistributedSystems", "Microservices are distributed systems"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bkubernetes\\b|\\bk8s\\b"),
                new PrerequisiteInfo("Docker", "Docker", "Kubernetes orchestrates containers"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bdocker\\s*compose\\b"),
                new PrerequisiteInfo("Docker", "Docker", "Compose builds on Docker basics"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bgraphql\\b"),
                new PrerequisiteInfo("REST APIs", "RestApi", "GraphQL is an alternative to REST"));

        // Database prerequisites
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bjpa\\b|\\bhibernate\\b"),
                new PrerequisiteInfo("SQL", "SqlBasics", "ORM maps to relational databases"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bmongodb\\b"),
                new PrerequisiteInfo("NoSQL Concepts", "NoSqlDatabases", "MongoDB is a NoSQL database"));

        // Advanced patterns
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bevent\\s*sourc(e|ing)\\b"),
                new PrerequisiteInfo("Domain-Driven Design", "DomainDrivenDesign", "Event sourcing is a DDD pattern"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\bcqrs\\b"),
                new PrerequisiteInfo("Event Sourcing", "EventSourcing", "CQRS often paired with event sourcing"));
        PREREQUISITE_PATTERNS.put(
                Pattern.compile("(?i)\\basync\\b|\\basynchronous\\b"),
                new PrerequisiteInfo("Concurrency", "ConcurrencyBasics", "Async requires understanding concurrency"));
    }

    // Content type complexity influences prerequisite requirements
    private static final Map<ContentType, Integer> COMPLEXITY_LEVELS = Map.of(
            ContentType.OVERVIEW, 1,
            ContentType.CONCEPT, 2,
            ContentType.TUTORIAL, 3,
            ContentType.GUIDE, 3,
            ContentType.REFERENCE, 4,
            ContentType.COMPARISON, 4,
            ContentType.TROUBLESHOOTING, 4
    );

    /**
     * Analyze a topic to determine its prerequisites.
     *
     * @param topic The topic to analyze
     * @param contentType The type of content being written
     * @param wikiContext Optional wiki context for checking existing pages
     * @return Set of identified prerequisites
     */
    public PrerequisiteSet analyze(String topic, ContentType contentType, WikiLinkContext wikiContext) {
        if (topic == null || topic.isBlank()) {
            return PrerequisiteSet.empty("Unknown");
        }

        PrerequisiteSet.Builder builder = PrerequisiteSet.builder(topic);
        Set<String> addedTopics = new HashSet<>();

        // Check topic against known patterns
        for (Map.Entry<Pattern, PrerequisiteInfo> entry : PREREQUISITE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(topic).find()) {
                PrerequisiteInfo info = entry.getValue();
                if (!addedTopics.contains(info.topic())) {
                    addedTopics.add(info.topic());

                    // Check if wiki page exists
                    String wikiPage = null;
                    if (wikiContext != null && wikiContext.pageExists(info.wikiPage())) {
                        wikiPage = info.wikiPage();
                    }

                    // Determine prerequisite type based on content type complexity
                    PrerequisiteType type = determinePrerequisiteType(contentType, info);

                    if (type == PrerequisiteType.HARD) {
                        builder.addHard(info.topic(), wikiPage, info.reason());
                    } else if (type == PrerequisiteType.SOFT) {
                        builder.addSoft(info.topic(), wikiPage, info.reason());
                    }
                }
            }
        }

        // Add assumed knowledge based on content type
        addAssumedKnowledge(builder, contentType, addedTopics);

        return builder.build();
    }

    /**
     * Analyze with default wiki context.
     */
    public PrerequisiteSet analyze(String topic, ContentType contentType) {
        return analyze(topic, contentType, null);
    }

    /**
     * Determine prerequisite type based on content complexity.
     */
    private PrerequisiteType determinePrerequisiteType(ContentType contentType, PrerequisiteInfo info) {
        int complexity = COMPLEXITY_LEVELS.getOrDefault(contentType, 3);

        // Overview content has softer prerequisites
        if (complexity <= 1) {
            return PrerequisiteType.SOFT;
        }

        // Tutorial and guide content should have hard prerequisites
        if (contentType == ContentType.TUTORIAL || contentType == ContentType.GUIDE) {
            return PrerequisiteType.HARD;
        }

        // Reference and troubleshooting assume more knowledge
        if (contentType == ContentType.REFERENCE || contentType == ContentType.TROUBLESHOOTING) {
            return PrerequisiteType.SOFT;
        }

        return PrerequisiteType.HARD;
    }

    /**
     * Add assumed knowledge based on content type.
     */
    private void addAssumedKnowledge(PrerequisiteSet.Builder builder, ContentType contentType, Set<String> addedTopics) {
        // For technical content, assume basic programming knowledge
        if (contentType != ContentType.OVERVIEW) {
            if (!addedTopics.contains("Programming")) {
                builder.addAssumed("Basic programming concepts");
            }
        }

        // For tutorials, assume development environment setup
        if (contentType == ContentType.TUTORIAL) {
            builder.addAssumed("Development environment setup");
        }

        // For troubleshooting, assume familiarity with the technology
        if (contentType == ContentType.TROUBLESHOOTING) {
            builder.addAssumed("Basic familiarity with the technology");
        }
    }

    /**
     * Get suggested prerequisites for a topic based on common patterns.
     * Useful for interactive sessions.
     */
    public List<String> suggestPrerequisites(String topic) {
        List<String> suggestions = new ArrayList<>();

        for (Map.Entry<Pattern, PrerequisiteInfo> entry : PREREQUISITE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(topic).find()) {
                suggestions.add(entry.getValue().topic());
            }
        }

        return suggestions;
    }

    /**
     * Information about a prerequisite pattern match.
     */
    private record PrerequisiteInfo(String topic, String wikiPage, String reason) {}
}
