package com.jakefear.aipublisher.document;

import com.jakefear.aipublisher.util.PageNameUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Main entity representing a document moving through the publishing pipeline.
 * This is a mutable entity that accumulates content from each agent phase.
 */
public class PublishingDocument {

    // Identity
    private final UUID id;
    private final String pageName;
    private String title;

    // State
    private DocumentState state;
    private final Instant createdAt;
    private Instant updatedAt;

    // Content at each phase
    private TopicBrief topicBrief;
    private ResearchBrief researchBrief;
    private ArticleDraft draft;
    private FactCheckReport factCheckReport;
    private FinalArticle finalArticle;
    private CriticReport criticReport;

    // Audit trail
    private final List<AgentContribution> contributions;

    // Revision tracking
    private int revisionCycleCount;

    /**
     * Create a new document from a topic brief.
     */
    public PublishingDocument(TopicBrief topicBrief) {
        Objects.requireNonNull(topicBrief, "topicBrief must not be null");

        this.id = UUID.randomUUID();
        this.pageName = PageNameUtils.toCamelCase(topicBrief.topic());
        this.title = topicBrief.topic();
        this.state = DocumentState.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.topicBrief = topicBrief;
        this.contributions = new ArrayList<>();
        this.revisionCycleCount = 0;
    }

    /**
     * Create a document with a specific ID (for deserialization/recovery).
     */
    public PublishingDocument(UUID id, String pageName, TopicBrief topicBrief) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(pageName, "pageName must not be null");
        Objects.requireNonNull(topicBrief, "topicBrief must not be null");

        this.id = id;
        this.pageName = pageName;
        this.title = topicBrief.topic();
        this.state = DocumentState.CREATED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        this.topicBrief = topicBrief;
        this.contributions = new ArrayList<>();
        this.revisionCycleCount = 0;
    }

    // State transitions

    /**
     * Transition to a new state.
     *
     * @param newState the target state
     * @throws IllegalStateException if the transition is not valid
     */
    public void transitionTo(DocumentState newState) {
        if (!state.canTransitionTo(newState)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", state, newState));
        }
        this.state = newState;
        this.updatedAt = Instant.now();
    }

    /**
     * Advance to the next state in the normal flow.
     *
     * @throws IllegalStateException if no next state exists
     */
    public void advanceToNextState() {
        DocumentState next = state.getNextInFlow();
        if (next == null) {
            throw new IllegalStateException("No next state from " + state);
        }
        transitionTo(next);
    }

    /**
     * Revert to a previous state for revision.
     *
     * @throws IllegalStateException if revision is not possible from current state
     */
    public void revertForRevision() {
        DocumentState previous = state.getPreviousForRevision();
        if (previous == null) {
            throw new IllegalStateException("Cannot revert from state " + state);
        }
        transitionTo(previous);
        revisionCycleCount++;
    }

    /**
     * Mark the document as rejected.
     */
    public void reject() {
        transitionTo(DocumentState.REJECTED);
    }

    // Content setters (with state validation)

    /**
     * Set the research brief (from Research Agent).
     */
    public void setResearchBrief(ResearchBrief researchBrief) {
        if (state != DocumentState.RESEARCHING) {
            throw new IllegalStateException("Can only set research brief in RESEARCHING state");
        }
        this.researchBrief = Objects.requireNonNull(researchBrief);
        this.updatedAt = Instant.now();
    }

    /**
     * Set the article draft (from Writer Agent).
     */
    public void setDraft(ArticleDraft draft) {
        if (state != DocumentState.DRAFTING) {
            throw new IllegalStateException("Can only set draft in DRAFTING state");
        }
        this.draft = Objects.requireNonNull(draft);
        this.updatedAt = Instant.now();
    }

    /**
     * Set the fact check report (from Fact Checker Agent).
     */
    public void setFactCheckReport(FactCheckReport factCheckReport) {
        if (state != DocumentState.FACT_CHECKING) {
            throw new IllegalStateException("Can only set fact check report in FACT_CHECKING state");
        }
        this.factCheckReport = Objects.requireNonNull(factCheckReport);
        this.updatedAt = Instant.now();
    }

    /**
     * Set the final article (from Editor Agent).
     */
    public void setFinalArticle(FinalArticle finalArticle) {
        if (state != DocumentState.EDITING) {
            throw new IllegalStateException("Can only set final article in EDITING state");
        }
        this.finalArticle = Objects.requireNonNull(finalArticle);
        this.updatedAt = Instant.now();
    }

    /**
     * Set the critic report (from Critic Agent).
     */
    public void setCriticReport(CriticReport criticReport) {
        if (state != DocumentState.CRITIQUING) {
            throw new IllegalStateException("Can only set critic report in CRITIQUING state");
        }
        this.criticReport = Objects.requireNonNull(criticReport);
        this.updatedAt = Instant.now();
    }

    /**
     * Record an agent's contribution.
     */
    public void addContribution(AgentContribution contribution) {
        contributions.add(Objects.requireNonNull(contribution));
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public String getPageName() {
        return pageName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = Instant.now();
    }

    public DocumentState getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public TopicBrief getTopicBrief() {
        return topicBrief;
    }

    public ResearchBrief getResearchBrief() {
        return researchBrief;
    }

    public ArticleDraft getDraft() {
        return draft;
    }

    public FactCheckReport getFactCheckReport() {
        return factCheckReport;
    }

    public FinalArticle getFinalArticle() {
        return finalArticle;
    }

    public CriticReport getCriticReport() {
        return criticReport;
    }

    public List<AgentContribution> getContributions() {
        return Collections.unmodifiableList(contributions);
    }

    public int getRevisionCycleCount() {
        return revisionCycleCount;
    }

    // Status helpers

    /**
     * Check if the document is in a terminal state.
     */
    public boolean isComplete() {
        return state.isTerminal();
    }

    /**
     * Check if the document was successfully published.
     */
    public boolean isPublished() {
        return state == DocumentState.PUBLISHED;
    }

    /**
     * Check if the document was rejected.
     */
    public boolean isRejected() {
        return state == DocumentState.REJECTED;
    }

    /**
     * Check if revision is needed and allowed.
     */
    public boolean canRevise(int maxRevisionCycles) {
        return revisionCycleCount < maxRevisionCycles && !state.isTerminal();
    }

    // Utility methods

    @Override
    public String toString() {
        return String.format("PublishingDocument{id=%s, pageName='%s', state=%s, revisions=%d}",
                id, pageName, state, revisionCycleCount);
    }
}
