package com.jakefear.aipublisher.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A complete topic universe representing a knowledge domain.
 * Contains all topics, relationships, and configuration for wiki generation.
 */
public record TopicUniverse(
        String id,
        String name,
        String description,
        List<Topic> topics,
        List<TopicRelationship> relationships,
        ScopeConfiguration scope,
        DomainContext domainContext,
        List<String> backlog,
        Instant createdAt,
        Instant modifiedAt
) {
    /**
     * Compact constructor with validation and normalization.
     */
    public TopicUniverse {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(name, "name cannot be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name cannot be blank");
        }

        // Normalize
        if (description == null) description = "";
        topics = topics == null ? List.of() : List.copyOf(topics);
        relationships = relationships == null ? List.of() : List.copyOf(relationships);
        if (scope == null) scope = ScopeConfiguration.empty();
        if (domainContext == null) domainContext = DomainContext.empty();
        backlog = backlog == null ? List.of() : List.copyOf(backlog);
        if (createdAt == null) createdAt = Instant.now();
        if (modifiedAt == null) modifiedAt = createdAt;
    }

    /**
     * Create a new empty universe.
     */
    public static TopicUniverse create(String name, String description) {
        return new TopicUniverse(
                generateId(name),
                name,
                description,
                List.of(),
                List.of(),
                ScopeConfiguration.empty(),
                DomainContext.empty(),
                List.of(),
                Instant.now(),
                Instant.now()
        );
    }

    /**
     * Create a builder for this universe.
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Create a new builder.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * Generate an ID from name.
     */
    public static String generateId(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    // ==================== Query Methods ====================

    /**
     * Get all accepted topics.
     */
    @JsonIgnore
    public List<Topic> getAcceptedTopics() {
        return topics.stream()
                .filter(t -> t.status() == TopicStatus.ACCEPTED)
                .toList();
    }

    /**
     * Get all proposed topics awaiting decision.
     */
    @JsonIgnore
    public List<Topic> getProposedTopics() {
        return topics.stream()
                .filter(t -> t.status() == TopicStatus.PROPOSED)
                .toList();
    }

    /**
     * Get topics by priority.
     */
    @JsonIgnore
    public List<Topic> getTopicsByPriority(Priority priority) {
        return topics.stream()
                .filter(t -> t.priority() == priority && t.status().isActive())
                .toList();
    }

    /**
     * Get topics ready for generation, ordered by priority and dependencies.
     */
    @JsonIgnore
    public List<Topic> getGenerationOrder() {
        List<Topic> ready = topics.stream()
                .filter(Topic::isReadyForGeneration)
                .collect(Collectors.toCollection(ArrayList::new));

        // Sort by priority first
        ready.sort(Comparator.comparingInt(t -> t.priority().getOrder()));

        // Then apply topological sort based on dependencies
        return topologicalSort(ready);
    }

    /**
     * Get a topic by ID.
     */
    public Optional<Topic> getTopicById(String id) {
        return topics.stream()
                .filter(t -> t.id().equals(id))
                .findFirst();
    }

    /**
     * Get a topic by name (case-insensitive).
     */
    public Optional<Topic> getTopicByName(String name) {
        return topics.stream()
                .filter(t -> t.name().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Get relationships for a topic.
     */
    @JsonIgnore
    public List<TopicRelationship> getRelationshipsFor(String topicId) {
        return relationships.stream()
                .filter(r -> r.sourceTopicId().equals(topicId) ||
                        r.targetTopicId().equals(topicId))
                .filter(TopicRelationship::isActive)
                .toList();
    }

    /**
     * Get outgoing relationships from a topic.
     */
    @JsonIgnore
    public List<TopicRelationship> getOutgoingRelationships(String topicId) {
        return relationships.stream()
                .filter(r -> r.sourceTopicId().equals(topicId))
                .filter(TopicRelationship::isActive)
                .toList();
    }

    /**
     * Get incoming relationships to a topic.
     */
    @JsonIgnore
    public List<TopicRelationship> getIncomingRelationships(String topicId) {
        return relationships.stream()
                .filter(r -> r.targetTopicId().equals(topicId))
                .filter(TopicRelationship::isActive)
                .toList();
    }

    /**
     * Get prerequisites for a topic.
     */
    @JsonIgnore
    public List<Topic> getPrerequisites(String topicId) {
        return relationships.stream()
                .filter(r -> r.targetTopicId().equals(topicId))
                .filter(r -> r.type() == RelationshipType.PREREQUISITE_OF)
                .filter(TopicRelationship::isActive)
                .map(r -> getTopicById(r.sourceTopicId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    /**
     * Get related topics (any active relationship).
     */
    @JsonIgnore
    public List<Topic> getRelatedTopics(String topicId) {
        Set<String> relatedIds = new HashSet<>();

        for (TopicRelationship rel : getRelationshipsFor(topicId)) {
            if (rel.sourceTopicId().equals(topicId)) {
                relatedIds.add(rel.targetTopicId());
            } else {
                relatedIds.add(rel.sourceTopicId());
            }
        }

        return relatedIds.stream()
                .map(this::getTopicById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    // ==================== Statistics ====================

    /**
     * Get total accepted topic count.
     */
    @JsonIgnore
    public int getAcceptedCount() {
        return (int) topics.stream().filter(t -> t.status().isActive()).count();
    }

    /**
     * Get estimated total word count.
     */
    @JsonIgnore
    public int getEstimatedWordCount() {
        return topics.stream()
                .filter(t -> t.status().isActive())
                .mapToInt(Topic::estimatedWords)
                .sum();
    }

    /**
     * Get count by priority.
     */
    @JsonIgnore
    public Map<Priority, Long> getCountByPriority() {
        return topics.stream()
                .filter(t -> t.status().isActive())
                .collect(Collectors.groupingBy(Topic::priority, Collectors.counting()));
    }

    /**
     * Get count by status.
     */
    @JsonIgnore
    public Map<TopicStatus, Long> getCountByStatus() {
        return topics.stream()
                .collect(Collectors.groupingBy(Topic::status, Collectors.counting()));
    }

    // ==================== Modification ====================

    /**
     * Add a topic to the universe.
     */
    public TopicUniverse addTopic(Topic topic) {
        List<Topic> newTopics = new ArrayList<>(topics);
        newTopics.add(topic);
        return toBuilder().topics(newTopics).build();
    }

    /**
     * Update a topic in the universe.
     */
    public TopicUniverse updateTopic(Topic updatedTopic) {
        List<Topic> newTopics = topics.stream()
                .map(t -> t.id().equals(updatedTopic.id()) ? updatedTopic : t)
                .toList();
        return toBuilder().topics(newTopics).build();
    }

    /**
     * Add a relationship to the universe.
     */
    public TopicUniverse addRelationship(TopicRelationship relationship) {
        List<TopicRelationship> newRels = new ArrayList<>(relationships);
        newRels.add(relationship);
        return toBuilder().relationships(newRels).build();
    }

    /**
     * Update a relationship in the universe.
     */
    public TopicUniverse updateRelationship(TopicRelationship updatedRel) {
        List<TopicRelationship> newRels = relationships.stream()
                .map(r -> r.id().equals(updatedRel.id()) ? updatedRel : r)
                .toList();
        return toBuilder().relationships(newRels).build();
    }

    /**
     * Add item to backlog.
     */
    public TopicUniverse addToBacklog(String item) {
        List<String> newBacklog = new ArrayList<>(backlog);
        newBacklog.add(item);
        return toBuilder().backlog(newBacklog).build();
    }

    // ==================== Helper Methods ====================

    /**
     * Topological sort based on prerequisite relationships.
     */
    private List<Topic> topologicalSort(List<Topic> topics) {
        // Use merge function to handle any duplicate topic IDs (keep first occurrence)
        Map<String, Topic> topicMap = topics.stream()
                .collect(Collectors.toMap(Topic::id, t -> t, (existing, replacement) -> existing));

        Map<String, Set<String>> dependencies = new HashMap<>();
        for (Topic topic : topics) {
            dependencies.put(topic.id(), new HashSet<>());
        }

        // Build dependency graph
        for (TopicRelationship rel : relationships) {
            if (rel.impliesOrdering() &&
                    topicMap.containsKey(rel.sourceTopicId()) &&
                    topicMap.containsKey(rel.targetTopicId())) {
                // Target depends on source
                dependencies.get(rel.targetTopicId()).add(rel.sourceTopicId());
            }
        }

        // Kahn's algorithm
        List<Topic> result = new ArrayList<>();
        Queue<String> ready = new LinkedList<>();

        // Find topics with no dependencies
        for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
            if (entry.getValue().isEmpty()) {
                ready.add(entry.getKey());
            }
        }

        while (!ready.isEmpty()) {
            String current = ready.poll();
            Topic topic = topicMap.get(current);
            if (topic != null) {
                result.add(topic);
            }

            // Remove this topic from others' dependencies
            for (Map.Entry<String, Set<String>> entry : dependencies.entrySet()) {
                if (entry.getValue().remove(current) && entry.getValue().isEmpty()) {
                    ready.add(entry.getKey());
                }
            }
        }

        // Add any remaining topics (cycles or missing from relationships)
        for (Topic topic : topics) {
            if (!result.contains(topic)) {
                result.add(topic);
            }
        }

        return result;
    }

    /**
     * Builder for TopicUniverse.
     */
    public static class Builder {
        private String id;
        private String name;
        private String description = "";
        private List<Topic> topics = new ArrayList<>();
        private List<TopicRelationship> relationships = new ArrayList<>();
        private ScopeConfiguration scope = ScopeConfiguration.empty();
        private DomainContext domainContext = DomainContext.empty();
        private List<String> backlog = new ArrayList<>();
        private Instant createdAt = Instant.now();
        private Instant modifiedAt = Instant.now();

        public Builder(String name) {
            this.name = name;
            this.id = TopicUniverse.generateId(name);
        }

        public Builder(TopicUniverse universe) {
            this.id = universe.id;
            this.name = universe.name;
            this.description = universe.description;
            this.topics = new ArrayList<>(universe.topics);
            this.relationships = new ArrayList<>(universe.relationships);
            this.scope = universe.scope;
            this.domainContext = universe.domainContext;
            this.backlog = new ArrayList<>(universe.backlog);
            this.createdAt = universe.createdAt;
            this.modifiedAt = Instant.now();
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder topics(List<Topic> topics) {
            this.topics = new ArrayList<>(topics);
            return this;
        }

        public Builder addTopic(Topic topic) {
            // Check for duplicates by ID to prevent adding same topic twice
            boolean exists = this.topics.stream()
                    .anyMatch(t -> t.id().equals(topic.id()));
            if (!exists) {
                this.topics.add(topic);
            }
            return this;
        }

        public Builder relationships(List<TopicRelationship> relationships) {
            this.relationships = new ArrayList<>(relationships);
            return this;
        }

        public Builder addRelationship(TopicRelationship relationship) {
            this.relationships.add(relationship);
            return this;
        }

        public Builder scope(ScopeConfiguration scope) {
            this.scope = scope;
            return this;
        }

        public Builder domainContext(DomainContext domainContext) {
            this.domainContext = domainContext;
            return this;
        }

        public Builder backlog(List<String> backlog) {
            this.backlog = new ArrayList<>(backlog);
            return this;
        }

        public Builder addToBacklog(String item) {
            this.backlog.add(item);
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TopicUniverse build() {
            return new TopicUniverse(
                    id, name, description, topics, relationships,
                    scope, domainContext, backlog, createdAt, modifiedAt
            );
        }
    }
}
