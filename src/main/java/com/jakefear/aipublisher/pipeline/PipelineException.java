package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.document.DocumentState;

/**
 * Exception thrown when the publishing pipeline encounters an error.
 */
public class PipelineException extends RuntimeException {

    private final DocumentState failedAtState;
    private final boolean retriable;

    public PipelineException(String message, DocumentState failedAtState) {
        super(message);
        this.failedAtState = failedAtState;
        this.retriable = false;
    }

    public PipelineException(String message, DocumentState failedAtState, Throwable cause) {
        super(message, cause);
        this.failedAtState = failedAtState;
        this.retriable = false;
    }

    public PipelineException(String message, DocumentState failedAtState, boolean retriable) {
        super(message);
        this.failedAtState = failedAtState;
        this.retriable = retriable;
    }

    public PipelineException(String message, DocumentState failedAtState, Throwable cause, boolean retriable) {
        super(message, cause);
        this.failedAtState = failedAtState;
        this.retriable = retriable;
    }

    public DocumentState getFailedAtState() {
        return failedAtState;
    }

    public boolean isRetriable() {
        return retriable;
    }
}
