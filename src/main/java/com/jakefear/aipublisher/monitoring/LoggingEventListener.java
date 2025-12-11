package com.jakefear.aipublisher.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Event listener that logs pipeline events.
 */
@Component
public class LoggingEventListener implements PipelineEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventListener.class);

    @Override
    public void onEvent(PipelineEvent event) {
        switch (event.type()) {
            case PIPELINE_STARTED, PIPELINE_COMPLETED ->
                    log.info("[{}] {}", event.type(), event.message());

            case PHASE_STARTED, PHASE_COMPLETED ->
                    log.info("[{}] {} - {}", event.type(), event.topic(), event.message());

            case APPROVAL_REQUESTED, APPROVAL_RECEIVED ->
                    log.info("[{}] {} at {}", event.type(), event.topic(), event.currentState());

            case REVISION_STARTED ->
                    log.info("[{}] {}", event.type(), event.message());

            case PIPELINE_FAILED ->
                    log.error("[{}] {}", event.type(), event.message());

            case WARNING ->
                    log.warn("[{}] {}", event.topic(), event.message());

            case INFO ->
                    log.info("[{}] {}", event.topic(), event.message());
        }
    }
}
