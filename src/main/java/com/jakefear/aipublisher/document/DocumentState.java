package com.jakefear.aipublisher.document;

import java.util.EnumSet;
import java.util.Set;

/**
 * States in the document publishing lifecycle.
 * Documents progress through these states as they move through the pipeline.
 */
public enum DocumentState {
    /**
     * Initial state - document has been created with a topic brief.
     */
    CREATED,

    /**
     * Research phase - Research Agent is working or has completed.
     */
    RESEARCHING,

    /**
     * Draft phase - Writer Agent is working or has completed.
     */
    DRAFTING,

    /**
     * Fact-check phase - Fact Checker Agent is working or has completed.
     */
    FACT_CHECKING,

    /**
     * Editing phase - Editor Agent is working or has completed.
     */
    EDITING,

    /**
     * Critique phase - Critic Agent is reviewing article quality.
     */
    CRITIQUING,

    /**
     * Awaiting human approval at a checkpoint.
     */
    AWAITING_APPROVAL,

    /**
     * Final state - document has been published.
     */
    PUBLISHED,

    /**
     * Terminal state - document was rejected and cannot proceed.
     */
    REJECTED;

    /**
     * Valid transitions from each state.
     */
    private static final Set<DocumentState> CREATED_TRANSITIONS = EnumSet.of(RESEARCHING, REJECTED);
    private static final Set<DocumentState> RESEARCHING_TRANSITIONS = EnumSet.of(DRAFTING, AWAITING_APPROVAL, REJECTED);
    private static final Set<DocumentState> DRAFTING_TRANSITIONS = EnumSet.of(FACT_CHECKING, AWAITING_APPROVAL, REJECTED);
    private static final Set<DocumentState> FACT_CHECKING_TRANSITIONS = EnumSet.of(EDITING, DRAFTING, AWAITING_APPROVAL, REJECTED);
    private static final Set<DocumentState> EDITING_TRANSITIONS = EnumSet.of(CRITIQUING, FACT_CHECKING, DRAFTING, AWAITING_APPROVAL, REJECTED);
    private static final Set<DocumentState> CRITIQUING_TRANSITIONS = EnumSet.of(PUBLISHED, EDITING, DRAFTING, AWAITING_APPROVAL, REJECTED);
    private static final Set<DocumentState> AWAITING_APPROVAL_TRANSITIONS = EnumSet.of(RESEARCHING, DRAFTING, FACT_CHECKING, EDITING, CRITIQUING, PUBLISHED, REJECTED);
    private static final Set<DocumentState> TERMINAL_STATES = EnumSet.of(PUBLISHED, REJECTED);

    /**
     * Check if this state can transition to the target state.
     *
     * @param target the target state to transition to
     * @return true if the transition is valid
     */
    public boolean canTransitionTo(DocumentState target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return false; // Cannot transition to same state
        }
        return switch (this) {
            case CREATED -> CREATED_TRANSITIONS.contains(target);
            case RESEARCHING -> RESEARCHING_TRANSITIONS.contains(target);
            case DRAFTING -> DRAFTING_TRANSITIONS.contains(target);
            case FACT_CHECKING -> FACT_CHECKING_TRANSITIONS.contains(target);
            case EDITING -> EDITING_TRANSITIONS.contains(target);
            case CRITIQUING -> CRITIQUING_TRANSITIONS.contains(target);
            case AWAITING_APPROVAL -> AWAITING_APPROVAL_TRANSITIONS.contains(target);
            case PUBLISHED, REJECTED -> false; // Terminal states cannot transition
        };
    }

    /**
     * Check if this is a terminal state (no further transitions possible).
     */
    public boolean isTerminal() {
        return TERMINAL_STATES.contains(this);
    }

    /**
     * Check if this state represents active processing by an agent.
     */
    public boolean isProcessing() {
        return this == RESEARCHING || this == DRAFTING || this == FACT_CHECKING || this == EDITING || this == CRITIQUING;
    }

    /**
     * Get the next state in the normal happy-path flow.
     *
     * @return the next state, or null if this is a terminal state or awaiting approval
     */
    public DocumentState getNextInFlow() {
        return switch (this) {
            case CREATED -> RESEARCHING;
            case RESEARCHING -> DRAFTING;
            case DRAFTING -> FACT_CHECKING;
            case FACT_CHECKING -> EDITING;
            case EDITING -> CRITIQUING;
            case CRITIQUING -> PUBLISHED;
            default -> null;
        };
    }

    /**
     * Get the previous state for revision purposes.
     *
     * @return the previous processing state, or null if not applicable
     */
    public DocumentState getPreviousForRevision() {
        return switch (this) {
            case FACT_CHECKING -> DRAFTING;
            case EDITING -> FACT_CHECKING;
            case CRITIQUING -> EDITING;
            default -> null;
        };
    }

    /**
     * Get the set of valid target states from this state.
     */
    public Set<DocumentState> getValidTransitions() {
        return switch (this) {
            case CREATED -> EnumSet.copyOf(CREATED_TRANSITIONS);
            case RESEARCHING -> EnumSet.copyOf(RESEARCHING_TRANSITIONS);
            case DRAFTING -> EnumSet.copyOf(DRAFTING_TRANSITIONS);
            case FACT_CHECKING -> EnumSet.copyOf(FACT_CHECKING_TRANSITIONS);
            case EDITING -> EnumSet.copyOf(EDITING_TRANSITIONS);
            case CRITIQUING -> EnumSet.copyOf(CRITIQUING_TRANSITIONS);
            case AWAITING_APPROVAL -> EnumSet.copyOf(AWAITING_APPROVAL_TRANSITIONS);
            case PUBLISHED, REJECTED -> EnumSet.noneOf(DocumentState.class);
        };
    }
}
