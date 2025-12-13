package com.jakefear.aipublisher.seealso;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.linking.WikiLinkContext;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Generates "See Also" suggestions for wiki articles.
 */
@Component
public class SeeAlsoGenerator {

    // Common topic relationships based on technology hierarchies
    private static final Map<Pattern, TopicRelationship> TOPIC_RELATIONSHIPS = new LinkedHashMap<>();

    static {
        // Java ecosystem
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bspring\\s*(boot|mvc|security|data)?\\b"),
                new TopicRelationship("Java", "Java", SeeAlsoType.BROADER,
                        List.of("Spring Boot", "Spring MVC", "Spring Security")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bjava\\b(?!script)"),
                new TopicRelationship("Programming Languages", "ProgrammingLanguages", SeeAlsoType.BROADER,
                        List.of("Spring Framework", "Maven", "Gradle", "JUnit")));

        // JavaScript ecosystem
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\breact\\b"),
                new TopicRelationship("JavaScript", "JavaScript", SeeAlsoType.BROADER,
                        List.of("Redux", "React Router", "Next.js")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bvue\\b"),
                new TopicRelationship("JavaScript", "JavaScript", SeeAlsoType.BROADER,
                        List.of("Vuex", "Vue Router", "Nuxt.js")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bangular\\b"),
                new TopicRelationship("TypeScript", "TypeScript", SeeAlsoType.BROADER,
                        List.of("RxJS", "NgRx", "Angular CLI")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bjavascript\\b"),
                new TopicRelationship("Programming Languages", "ProgrammingLanguages", SeeAlsoType.BROADER,
                        List.of("TypeScript", "Node.js", "React", "Vue", "Angular")));

        // Python ecosystem
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bdjango\\b"),
                new TopicRelationship("Python", "Python", SeeAlsoType.BROADER,
                        List.of("Django REST Framework", "Celery", "Django ORM")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bflask\\b"),
                new TopicRelationship("Python", "Python", SeeAlsoType.BROADER,
                        List.of("SQLAlchemy", "Jinja2", "Flask-RESTful")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bpython\\b"),
                new TopicRelationship("Programming Languages", "ProgrammingLanguages", SeeAlsoType.BROADER,
                        List.of("Django", "Flask", "FastAPI", "NumPy", "Pandas")));

        // DevOps ecosystem
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bkubernetes\\b|\\bk8s\\b"),
                new TopicRelationship("Container Orchestration", "ContainerOrchestration", SeeAlsoType.BROADER,
                        List.of("Docker", "Helm", "Istio", "Prometheus")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bdocker\\b"),
                new TopicRelationship("Containerization", "Containerization", SeeAlsoType.BROADER,
                        List.of("Docker Compose", "Kubernetes", "Container Registry")));

        // Database ecosystem
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bpostgresql\\b|\\bpostgres\\b"),
                new TopicRelationship("Relational Databases", "RelationalDatabases", SeeAlsoType.BROADER,
                        List.of("SQL", "Database Indexing", "Database Replication")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bmongodb\\b"),
                new TopicRelationship("NoSQL Databases", "NoSqlDatabases", SeeAlsoType.BROADER,
                        List.of("Document Databases", "MongoDB Atlas", "Aggregation Pipeline")));

        // API patterns
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\brest\\s*api\\b"),
                new TopicRelationship("API Design", "ApiDesign", SeeAlsoType.BROADER,
                        List.of("GraphQL", "gRPC", "OpenAPI")));
        TOPIC_RELATIONSHIPS.put(
                Pattern.compile("(?i)\\bgraphql\\b"),
                new TopicRelationship("API Design", "ApiDesign", SeeAlsoType.BROADER,
                        List.of("REST API", "Apollo", "GraphQL Schema")));
    }

    /**
     * Generate See Also suggestions based on topic and content type.
     */
    public SeeAlsoSection generate(String topic, ContentType contentType, WikiLinkContext wikiContext) {
        if (topic == null || topic.isBlank()) {
            return SeeAlsoSection.empty("Unknown");
        }

        SeeAlsoSection.Builder builder = SeeAlsoSection.builder(topic);
        Set<String> addedTopics = new HashSet<>();

        // Find matching relationships
        for (Map.Entry<Pattern, TopicRelationship> entry : TOPIC_RELATIONSHIPS.entrySet()) {
            if (entry.getKey().matcher(topic).find()) {
                TopicRelationship rel = entry.getValue();

                // Add broader topic
                if (!addedTopics.contains(rel.broaderTopic())) {
                    addedTopics.add(rel.broaderTopic());
                    String wikiPage = determineWikiPage(rel.broaderWikiPage(), wikiContext);
                    builder.add(SeeAlsoEntry.broader(
                            rel.broaderTopic(),
                            wikiPage,
                            "Parent topic"
                    ).withRelevance(0.8));
                }

                // Add related topics
                for (String related : rel.relatedTopics()) {
                    if (!addedTopics.contains(related) && !topic.equalsIgnoreCase(related)) {
                        addedTopics.add(related);
                        String wikiPage = determineWikiPage(related.replaceAll("\\s+", ""), wikiContext);
                        builder.add(SeeAlsoEntry.related(
                                related,
                                wikiPage,
                                ""
                        ).withRelevance(0.6));
                    }
                }
            }
        }

        // Add content-type-specific suggestions
        addContentTypeSpecificSuggestions(builder, topic, contentType, addedTopics);

        return builder.build();
    }

    /**
     * Generate See Also suggestions without wiki context.
     */
    public SeeAlsoSection generate(String topic, ContentType contentType) {
        return generate(topic, contentType, null);
    }

    /**
     * Determine the wiki page to link to.
     */
    private String determineWikiPage(String candidatePage, WikiLinkContext wikiContext) {
        if (wikiContext != null && wikiContext.pageExists(candidatePage)) {
            return candidatePage;
        }
        // Return the candidate even if page doesn't exist - writer can create it
        return candidatePage;
    }

    /**
     * Add suggestions based on content type.
     */
    private void addContentTypeSpecificSuggestions(
            SeeAlsoSection.Builder builder,
            String topic,
            ContentType contentType,
            Set<String> addedTopics) {

        String topicBase = topic.replaceAll("(?i)\\s*(tutorial|guide|overview|reference|introduction)\\s*", "").trim();

        switch (contentType) {
            case TUTORIAL -> {
                // Tutorials should link to related concepts and guides
                if (!addedTopics.contains(topicBase + " Concepts")) {
                    builder.add(SeeAlsoEntry.internal(
                            topicBase + " Concepts",
                            SeeAlsoType.REFERENCE,
                            topicBase.replaceAll("\\s+", "") + "Concepts",
                            "Underlying concepts",
                            0.7
                    ));
                }
            }
            case REFERENCE -> {
                // References should link to tutorials and guides
                if (!addedTopics.contains(topicBase + " Tutorial")) {
                    builder.add(SeeAlsoEntry.internal(
                            topicBase + " Tutorial",
                            SeeAlsoType.TUTORIAL,
                            topicBase.replaceAll("\\s+", "") + "Tutorial",
                            "Step-by-step guide",
                            0.7
                    ));
                }
            }
            case COMPARISON -> {
                // Comparisons should link to individual topics being compared
                // Extract topics from "X vs Y" pattern
                String[] parts = topic.split("(?i)\\s+vs\\.?\\s+|\\s+versus\\s+");
                for (String part : parts) {
                    String cleanPart = part.trim();
                    if (!cleanPart.isBlank() && !addedTopics.contains(cleanPart)) {
                        addedTopics.add(cleanPart);
                        builder.add(SeeAlsoEntry.related(
                                cleanPart,
                                cleanPart.replaceAll("\\s+", ""),
                                "See individual topic"
                        ).withRelevance(0.9));
                    }
                }
            }
            case TROUBLESHOOTING -> {
                // Troubleshooting should link to main topic and tutorials
                if (!addedTopics.contains(topicBase)) {
                    builder.add(SeeAlsoEntry.internal(
                            topicBase,
                            SeeAlsoType.BROADER,
                            topicBase.replaceAll("\\s+", ""),
                            "Main topic overview",
                            0.8
                    ));
                }
            }
            default -> {
                // Default: suggest a tutorial if this is a concept/overview
                if (contentType == ContentType.CONCEPT || contentType == ContentType.OVERVIEW) {
                    if (!addedTopics.contains(topicBase + " Tutorial")) {
                        builder.add(SeeAlsoEntry.internal(
                                topicBase + " Tutorial",
                                SeeAlsoType.TUTORIAL,
                                topicBase.replaceAll("\\s+", "") + "Tutorial",
                                "Hands-on guide",
                                0.6
                        ));
                    }
                }
            }
        }
    }

    /**
     * Get suggested entries for interactive topic selection.
     */
    public List<String> suggestRelatedTopics(String topic) {
        Set<String> suggestions = new LinkedHashSet<>();

        for (Map.Entry<Pattern, TopicRelationship> entry : TOPIC_RELATIONSHIPS.entrySet()) {
            if (entry.getKey().matcher(topic).find()) {
                TopicRelationship rel = entry.getValue();
                suggestions.add(rel.broaderTopic());
                suggestions.addAll(rel.relatedTopics());
            }
        }

        return new ArrayList<>(suggestions);
    }

    /**
     * Internal record for topic relationships.
     */
    private record TopicRelationship(
            String broaderTopic,
            String broaderWikiPage,
            SeeAlsoType broaderType,
            List<String> relatedTopics
    ) {}
}
