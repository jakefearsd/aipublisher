package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.agent.AgentRole;
import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central service for pipeline monitoring.
 *
 * Dispatches events to registered listeners and collects metrics.
 */
@Service
public class PipelineMonitoringService {

    private final List<PipelineEventListener> listeners = new CopyOnWriteArrayList<>();
    private final PipelineMetrics metrics = new PipelineMetrics();

    public PipelineMonitoringService(List<PipelineEventListener> autoRegisteredListeners) {
        this.listeners.addAll(autoRegisteredListeners);
    }

    /**
     * Register an event listener.
     */
    public void addListener(PipelineEventListener listener) {
        listeners.add(listener);
    }

    /**
     * Unregister an event listener.
     */
    public void removeListener(PipelineEventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Emit a pipeline event to all listeners.
     */
    public void emit(PipelineEvent event) {
        listeners.forEach(listener -> {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                // Don't let listener errors break the pipeline
            }
        });
    }

    /**
     * Record and emit pipeline started event.
     */
    public void pipelineStarted(PublishingDocument document) {
        metrics.recordPipelineStarted();
        emit(PipelineEvent.pipelineStarted(document));
    }

    /**
     * Record and emit phase started event.
     */
    public void phaseStarted(PublishingDocument document, DocumentState previousState, DocumentState newState) {
        emit(PipelineEvent.phaseStarted(document, previousState, newState));
    }

    /**
     * Record and emit phase completed event.
     */
    public void phaseCompleted(PublishingDocument document, DocumentState state, String summary) {
        emit(PipelineEvent.phaseCompleted(document, state, summary));
    }

    /**
     * Record and emit approval requested event.
     */
    public void approvalRequested(PublishingDocument document, DocumentState atState) {
        metrics.recordApprovalRequested();
        emit(PipelineEvent.approvalRequested(document, atState));
    }

    /**
     * Record and emit approval received event.
     */
    public void approvalReceived(PublishingDocument document, DocumentState atState, boolean approved) {
        if (approved) {
            metrics.recordApprovalGranted();
        } else {
            metrics.recordApprovalRejected();
        }
        emit(PipelineEvent.approvalReceived(document, atState, approved));
    }

    /**
     * Record and emit revision started event.
     */
    public void revisionStarted(PublishingDocument document, int revisionNumber, int maxRevisions) {
        metrics.recordRevisionCycle();
        emit(PipelineEvent.revisionStarted(document, revisionNumber, maxRevisions));
    }

    /**
     * Record and emit pipeline completed event.
     */
    public void pipelineCompleted(PublishingDocument document, Duration totalTime) {
        metrics.recordPipelineCompleted(totalTime);
        emit(PipelineEvent.pipelineCompleted(document, totalTime.toMillis()));
    }

    /**
     * Record and emit pipeline failed event.
     */
    public void pipelineFailed(PublishingDocument document, DocumentState failedAt, String error) {
        metrics.recordPipelineFailed(failedAt);
        emit(PipelineEvent.pipelineFailed(document, failedAt, error));
    }

    /**
     * Record agent processing time.
     */
    public void recordAgentProcessing(AgentRole role, Duration processingTime) {
        metrics.recordAgentProcessing(role, processingTime);
    }

    /**
     * Emit a warning event.
     */
    public void warn(PublishingDocument document, String message) {
        emit(PipelineEvent.warning(document, message));
    }

    /**
     * Emit an info event.
     */
    public void info(PublishingDocument document, String message) {
        emit(PipelineEvent.info(document, message));
    }

    /**
     * Get the current metrics.
     */
    public PipelineMetrics getMetrics() {
        return metrics;
    }

    /**
     * Generate a metrics report.
     */
    public String generateMetricsReport() {
        return metrics.generateReport();
    }

    /**
     * Get the number of registered listeners.
     */
    public int getListenerCount() {
        return listeners.size();
    }
}
