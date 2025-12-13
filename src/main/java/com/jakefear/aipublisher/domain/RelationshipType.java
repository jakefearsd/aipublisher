package com.jakefear.aipublisher.domain;

/**
 * Types of relationships between topics in a knowledge domain.
 */
public enum RelationshipType {
    /**
     * Source topic must be understood before target topic.
     * Example: "Java" is PREREQUISITE_OF "Spring Boot"
     */
    PREREQUISITE_OF("Prerequisite of", "Must understand source before target", true),

    /**
     * Source topic is a component or subtopic of target.
     * Example: "Consumer Groups" is PART_OF "Apache Kafka"
     */
    PART_OF("Part of", "Source is a component of target", true),

    /**
     * Source topic illustrates or demonstrates target concept.
     * Example: "Shopping Cart Saga" is EXAMPLE_OF "Saga Pattern"
     */
    EXAMPLE_OF("Example of", "Source illustrates target concept", true),

    /**
     * Topics are related but neither depends on the other.
     * Example: "Kafka" is RELATED_TO "RabbitMQ"
     */
    RELATED_TO("Related to", "Topics share context but are independent", false),

    /**
     * Topics are alternatives or competitors, worth comparing.
     * Example: "Event Sourcing" CONTRASTS_WITH "Traditional CRUD"
     */
    CONTRASTS_WITH("Contrasts with", "Alternative approaches worth comparing", false),

    /**
     * Source implements or realizes target concept.
     * Example: "Apache Kafka" IMPLEMENTS "Message Broker"
     */
    IMPLEMENTS("Implements", "Source is a concrete form of target", true),

    /**
     * Source replaces or updates target.
     * Example: "Kafka Streams 3.0" SUPERSEDES "Kafka Streams 2.x"
     */
    SUPERSEDES("Supersedes", "Source replaces or updates target", true),

    /**
     * Topics are often used together.
     * Example: "CQRS" PAIRS_WITH "Event Sourcing"
     */
    PAIRS_WITH("Pairs with", "Often used together in practice", false);

    private final String displayName;
    private final String description;
    private final boolean directional;

    RelationshipType(String displayName, String description, boolean directional) {
        this.displayName = displayName;
        this.description = description;
        this.directional = directional;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Whether the relationship has a direction (source → target matters).
     */
    public boolean isDirectional() {
        return directional;
    }

    /**
     * Whether this relationship implies generation ordering.
     * Source should be generated before target.
     */
    public boolean impliesOrdering() {
        return this == PREREQUISITE_OF || this == PART_OF || this == IMPLEMENTS;
    }

    /**
     * Whether this relationship suggests a comparison page.
     */
    public boolean suggestsComparison() {
        return this == CONTRASTS_WITH || this == RELATED_TO;
    }

    /**
     * Whether this relationship should create a link in the source page.
     */
    public boolean shouldLinkFromSource() {
        return true; // All relationships create links
    }

    /**
     * Whether this relationship should create a backlink in the target page.
     */
    public boolean shouldLinkFromTarget() {
        return !directional || this == PART_OF;
    }
}
