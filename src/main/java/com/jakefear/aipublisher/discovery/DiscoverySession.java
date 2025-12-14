package com.jakefear.aipublisher.discovery;

import com.jakefear.aipublisher.domain.*;

import java.time.Instant;
import java.util.*;

/**
 * Manages an interactive domain discovery session.
 * Tracks state, pending suggestions, and user decisions.
 */
public class DiscoverySession {

    private final String sessionId;
    private final Instant startedAt;

    private DiscoveryPhase currentPhase;
    private TopicUniverse.Builder universeBuilder;

    // Pending items awaiting user decision
    private final List<TopicSuggestion> pendingTopicSuggestions;
    private final List<RelationshipSuggestion> pendingRelationshipSuggestions;
    private final List<String> pendingGaps;

    // Session history for audit
    private final List<SessionEvent> history;

    // Current expansion context
    private String currentExpansionSource;
    private int expansionDepth;

    public DiscoverySession(String domainName) {
        this.sessionId = UUID.randomUUID().toString().substring(0, 8);
        this.startedAt = Instant.now();
        this.currentPhase = DiscoveryPhase.SEED_INPUT;
        this.universeBuilder = TopicUniverse.builder(domainName);
        this.pendingTopicSuggestions = new ArrayList<>();
        this.pendingRelationshipSuggestions = new ArrayList<>();
        this.pendingGaps = new ArrayList<>();
        this.history = new ArrayList<>();
        this.expansionDepth = 0;

        recordEvent("Session started for domain: " + domainName);
    }

    // ==================== Phase Management ====================

    public DiscoveryPhase getCurrentPhase() {
        return currentPhase;
    }

    public void advancePhase() {
        DiscoveryPhase previous = currentPhase;
        currentPhase = currentPhase.next();
        recordEvent("Phase advanced: " + previous + " -> " + currentPhase);
    }

    public void goToPhase(DiscoveryPhase phase) {
        DiscoveryPhase previous = currentPhase;
        currentPhase = phase;
        recordEvent("Phase changed: " + previous + " -> " + currentPhase);
    }

    public boolean isComplete() {
        return currentPhase.isTerminal();
    }

    // ==================== Universe Access ====================

    public TopicUniverse buildUniverse() {
        return universeBuilder.build();
    }

    public String getDomainName() {
        return universeBuilder.build().name();
    }

    public void setDomainDescription(String description) {
        universeBuilder.description(description);
        recordEvent("Domain description set");
    }

    // ==================== Seed Topics ====================

    public void addSeedTopic(String name, String description) {
        Topic topic = Topic.builder(name)
                .description(description)
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.MUST_HAVE)
                .isLandingPage(false)
                .addedReason("User-provided seed topic")
                .build();

        universeBuilder.addTopic(topic);
        recordEvent("Seed topic added: " + name);
    }

    public void addLandingPage(String name, String description) {
        Topic topic = Topic.builder(name)
                .description(description)
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.MUST_HAVE)
                .isLandingPage(true)
                .addedReason("Landing page for domain")
                .build();

        universeBuilder.addTopic(topic);
        recordEvent("Landing page added: " + name);
    }

    // ==================== Scope Configuration ====================

    public void configureScope(ScopeConfiguration scope) {
        universeBuilder.scope(scope);
        recordEvent("Scope configured");
    }

    public ScopeConfiguration getScope() {
        return universeBuilder.build().scope();
    }

    // ==================== Topic Suggestions ====================

    public void addTopicSuggestions(List<TopicSuggestion> suggestions) {
        pendingTopicSuggestions.addAll(suggestions);
        recordEvent("Added " + suggestions.size() + " topic suggestions");
    }

    public List<TopicSuggestion> getPendingTopicSuggestions() {
        return Collections.unmodifiableList(pendingTopicSuggestions);
    }

    public void clearPendingTopicSuggestions() {
        pendingTopicSuggestions.clear();
    }

    public void acceptTopicSuggestion(TopicSuggestion suggestion) {
        Topic topic = Topic.builder(suggestion.name())
                .description(suggestion.description())
                .contentType(suggestion.suggestedContentType())
                .complexity(suggestion.suggestedComplexity())
                .estimatedWords(suggestion.suggestedWordCount())
                .category(suggestion.category())
                .status(TopicStatus.ACCEPTED)
                .priority(Priority.SHOULD_HAVE)
                .addedReason("AI suggested: " + suggestion.rationale())
                .build();

        universeBuilder.addTopic(topic);
        pendingTopicSuggestions.remove(suggestion);
        recordEvent("Accepted topic: " + suggestion.name());
    }

    public void rejectTopicSuggestion(TopicSuggestion suggestion) {
        pendingTopicSuggestions.remove(suggestion);
        recordEvent("Rejected topic: " + suggestion.name());
    }

    public void deferTopicSuggestion(TopicSuggestion suggestion) {
        universeBuilder.addToBacklog(suggestion.name() + ": " + suggestion.description());
        pendingTopicSuggestions.remove(suggestion);
        recordEvent("Deferred topic to backlog: " + suggestion.name());
    }

    public Topic modifyAndAcceptTopic(TopicSuggestion suggestion, Topic.Builder modifications) {
        Topic topic = modifications
                .addedReason("AI suggested, user modified")
                .build();

        universeBuilder.addTopic(topic);
        pendingTopicSuggestions.remove(suggestion);
        recordEvent("Modified and accepted topic: " + topic.name());
        return topic;
    }

    // ==================== Relationship Suggestions ====================

    public void addRelationshipSuggestions(List<RelationshipSuggestion> suggestions) {
        pendingRelationshipSuggestions.addAll(suggestions);
        recordEvent("Added " + suggestions.size() + " relationship suggestions");
    }

    public List<RelationshipSuggestion> getPendingRelationshipSuggestions() {
        return Collections.unmodifiableList(pendingRelationshipSuggestions);
    }

    public void clearPendingRelationshipSuggestions() {
        pendingRelationshipSuggestions.clear();
    }

    public void confirmRelationship(RelationshipSuggestion suggestion) {
        TopicRelationship rel = TopicRelationship.confirmed(
                Topic.generateId(suggestion.sourceTopicName()),
                Topic.generateId(suggestion.targetTopicName()),
                suggestion.suggestedType()
        );

        universeBuilder.addRelationship(rel);
        pendingRelationshipSuggestions.remove(suggestion);
        recordEvent("Confirmed relationship: " + suggestion.describe());
    }

    public void confirmRelationshipAs(RelationshipSuggestion suggestion, RelationshipType newType) {
        TopicRelationship rel = TopicRelationship.confirmed(
                Topic.generateId(suggestion.sourceTopicName()),
                Topic.generateId(suggestion.targetTopicName()),
                newType
        );

        universeBuilder.addRelationship(rel);
        pendingRelationshipSuggestions.remove(suggestion);
        recordEvent("Confirmed relationship as " + newType + ": " + suggestion.describe());
    }

    public void rejectRelationship(RelationshipSuggestion suggestion) {
        pendingRelationshipSuggestions.remove(suggestion);
        recordEvent("Rejected relationship: " + suggestion.describe());
    }

    // ==================== Gap Analysis ====================

    public void addGaps(List<String> gaps) {
        pendingGaps.addAll(gaps);
        recordEvent("Added " + gaps.size() + " potential gaps");
    }

    public List<String> getPendingGaps() {
        return Collections.unmodifiableList(pendingGaps);
    }

    public void clearPendingGaps() {
        pendingGaps.clear();
    }

    public void addressGapWithTopic(String gap, Topic topic) {
        universeBuilder.addTopic(topic);
        pendingGaps.remove(gap);
        recordEvent("Gap addressed with topic: " + topic.name());
    }

    public void addressGapWithGlossary(String gap) {
        pendingGaps.remove(gap);
        recordEvent("Gap addressed via glossary: " + gap);
    }

    public void ignoreGap(String gap) {
        pendingGaps.remove(gap);
        recordEvent("Gap ignored: " + gap);
    }

    // ==================== Topic Modification ====================

    public void updateTopicPriority(String topicId, Priority priority) {
        TopicUniverse current = universeBuilder.build();
        current.getTopicById(topicId).ifPresent(topic -> {
            Topic updated = topic.toBuilder().priority(priority).build();
            List<Topic> newTopics = current.topics().stream()
                    .map(t -> t.id().equals(topicId) ? updated : t)
                    .toList();
            universeBuilder.topics(newTopics);
            recordEvent("Priority updated for " + topic.name() + ": " + priority);
        });
    }

    public void updateTopicDepth(String topicId, int wordCount) {
        TopicUniverse current = universeBuilder.build();
        current.getTopicById(topicId).ifPresent(topic -> {
            Topic updated = topic.toBuilder().estimatedWords(wordCount).build();
            List<Topic> newTopics = current.topics().stream()
                    .map(t -> t.id().equals(topicId) ? updated : t)
                    .toList();
            universeBuilder.topics(newTopics);
            recordEvent("Depth updated for " + topic.name() + ": " + wordCount + " words");
        });
    }

    public void addTopicEmphasis(String topicId, String emphasis) {
        TopicUniverse current = universeBuilder.build();
        current.getTopicById(topicId).ifPresent(topic -> {
            Topic updated = topic.toBuilder().addEmphasis(emphasis).build();
            List<Topic> newTopics = current.topics().stream()
                    .map(t -> t.id().equals(topicId) ? updated : t)
                    .toList();
            universeBuilder.topics(newTopics);
            recordEvent("Emphasis added to " + topic.name() + ": " + emphasis);
        });
    }

    public void addTopicSkip(String topicId, String skip) {
        TopicUniverse current = universeBuilder.build();
        current.getTopicById(topicId).ifPresent(topic -> {
            Topic updated = topic.toBuilder().addSkip(skip).build();
            List<Topic> newTopics = current.topics().stream()
                    .map(t -> t.id().equals(topicId) ? updated : t)
                    .toList();
            universeBuilder.topics(newTopics);
            recordEvent("Skip added to " + topic.name() + ": " + skip);
        });
    }

    // ==================== Expansion Tracking ====================

    public void setExpansionSource(String topicName) {
        this.currentExpansionSource = topicName;
        this.expansionDepth++;
        recordEvent("Expanding from: " + topicName + " (depth " + expansionDepth + ")");
    }

    public String getCurrentExpansionSource() {
        return currentExpansionSource;
    }

    public int getExpansionDepth() {
        return expansionDepth;
    }

    // ==================== Session Info ====================

    public String getSessionId() {
        return sessionId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public List<SessionEvent> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public int getAcceptedTopicCount() {
        return buildUniverse().getAcceptedCount();
    }

    public int getPendingSuggestionCount() {
        return pendingTopicSuggestions.size() +
                pendingRelationshipSuggestions.size() +
                pendingGaps.size();
    }

    // ==================== Internal ====================

    private void recordEvent(String description) {
        history.add(new SessionEvent(Instant.now(), currentPhase, description));
    }

    /**
     * Record of a session event for audit.
     */
    public record SessionEvent(Instant timestamp, DiscoveryPhase phase, String description) {}
}
