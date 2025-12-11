package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents an event in the publishing pipeline lifecycle.
 */
public record PipelineEvent(
        UUID id,
        EventType type,
        String topic,
        DocumentState previousState,
        DocumentState currentState,
        String message,
        Instant timestamp,
        PublishingDocument document
) {
    /**
     * Types of pipeline events.
     */
    public enum EventType {
        /** Pipeline execution started */
        PIPELINE_STARTED,

        /** Phase transition occurred */
        PHASE_STARTED,

        /** Phase completed successfully */
        PHASE_COMPLETED,

        /** Approval requested */
        APPROVAL_REQUESTED,

        /** Approval received */
        APPROVAL_RECEIVED,

        /** Revision cycle triggered */
        REVISION_STARTED,

        /** Pipeline completed successfully */
        PIPELINE_COMPLETED,

        /** Pipeline failed */
        PIPELINE_FAILED,

        /** General warning */
        WARNING,

        /** General info */
        INFO
    }

    /**
     * Create a pipeline started event.
     */
    public static PipelineEvent pipelineStarted(PublishingDocument document) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.PIPELINE_STARTED,
                document.getTopicBrief().topic(),
                null,
                DocumentState.CREATED,
                "Pipeline started for: " + document.getTopicBrief().topic(),
                Instant.now(),
                document
        );
    }

    /**
     * Create a phase started event.
     */
    public static PipelineEvent phaseStarted(PublishingDocument document, DocumentState previousState, DocumentState newState) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.PHASE_STARTED,
                document.getTopicBrief().topic(),
                previousState,
                newState,
                "Phase started: " + newState,
                Instant.now(),
                document
        );
    }

    /**
     * Create a phase completed event.
     */
    public static PipelineEvent phaseCompleted(PublishingDocument document, DocumentState state, String summary) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.PHASE_COMPLETED,
                document.getTopicBrief().topic(),
                state,
                state,
                summary,
                Instant.now(),
                document
        );
    }

    /**
     * Create an approval requested event.
     */
    public static PipelineEvent approvalRequested(PublishingDocument document, DocumentState atState) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.APPROVAL_REQUESTED,
                document.getTopicBrief().topic(),
                atState,
                atState,
                "Approval requested at: " + atState,
                Instant.now(),
                document
        );
    }

    /**
     * Create an approval received event.
     */
    public static PipelineEvent approvalReceived(PublishingDocument document, DocumentState atState, boolean approved) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.APPROVAL_RECEIVED,
                document.getTopicBrief().topic(),
                atState,
                atState,
                approved ? "Approved at: " + atState : "Changes requested at: " + atState,
                Instant.now(),
                document
        );
    }

    /**
     * Create a revision started event.
     */
    public static PipelineEvent revisionStarted(PublishingDocument document, int revisionNumber, int maxRevisions) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.REVISION_STARTED,
                document.getTopicBrief().topic(),
                DocumentState.FACT_CHECKING,
                DocumentState.DRAFTING,
                String.format("Revision cycle %d/%d started", revisionNumber, maxRevisions),
                Instant.now(),
                document
        );
    }

    /**
     * Create a pipeline completed event.
     */
    public static PipelineEvent pipelineCompleted(PublishingDocument document, long totalMillis) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.PIPELINE_COMPLETED,
                document.getTopicBrief().topic(),
                DocumentState.EDITING,
                DocumentState.PUBLISHED,
                String.format("Pipeline completed in %d ms", totalMillis),
                Instant.now(),
                document
        );
    }

    /**
     * Create a pipeline failed event.
     */
    public static PipelineEvent pipelineFailed(PublishingDocument document, DocumentState failedAt, String error) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.PIPELINE_FAILED,
                document.getTopicBrief().topic(),
                failedAt,
                failedAt,
                "Pipeline failed at " + failedAt + ": " + error,
                Instant.now(),
                document
        );
    }

    /**
     * Create a warning event.
     */
    public static PipelineEvent warning(PublishingDocument document, String message) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.WARNING,
                document.getTopicBrief().topic(),
                document.getState(),
                document.getState(),
                message,
                Instant.now(),
                document
        );
    }

    /**
     * Create an info event.
     */
    public static PipelineEvent info(PublishingDocument document, String message) {
        return new PipelineEvent(
                UUID.randomUUID(),
                EventType.INFO,
                document.getTopicBrief().topic(),
                document.getState(),
                document.getState(),
                message,
                Instant.now(),
                document
        );
    }
}
