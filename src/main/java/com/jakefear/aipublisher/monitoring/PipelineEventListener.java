package com.jakefear.aipublisher.monitoring;

/**
 * Listener interface for pipeline events.
 *
 * Implementations can receive notifications about pipeline state changes,
 * phase transitions, and errors.
 */
@FunctionalInterface
public interface PipelineEventListener {

    /**
     * Called when a pipeline event occurs.
     *
     * @param event The pipeline event
     */
    void onEvent(PipelineEvent event);
}
