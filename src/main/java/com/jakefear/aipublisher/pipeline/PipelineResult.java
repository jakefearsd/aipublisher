package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * Result of a pipeline execution.
 */
public record PipelineResult(
        /**
         * Whether the pipeline completed successfully.
         */
        boolean success,

        /**
         * The processed document.
         */
        PublishingDocument document,

        /**
         * Path to the output file if published.
         */
        Path outputPath,

        /**
         * Total processing time.
         */
        Duration totalTime,

        /**
         * Error message if failed.
         */
        String errorMessage,

        /**
         * State where failure occurred (if failed).
         */
        DocumentState failedAtState,

        /**
         * Path to the failed document saved for debugging (if failed and content was available).
         */
        Path failedDocumentPath
) {
    /**
     * Create a successful result.
     */
    public static PipelineResult success(PublishingDocument document, Path outputPath, Duration totalTime) {
        return new PipelineResult(true, document, outputPath, totalTime, null, null, null);
    }

    /**
     * Create a failed result without a saved debug document.
     */
    public static PipelineResult failure(PublishingDocument document, String errorMessage,
                                         DocumentState failedAtState, Duration totalTime) {
        return new PipelineResult(false, document, null, totalTime, errorMessage, failedAtState, null);
    }

    /**
     * Create a failed result with a saved debug document.
     */
    public static PipelineResult failure(PublishingDocument document, String errorMessage,
                                         DocumentState failedAtState, Duration totalTime,
                                         Path failedDocumentPath) {
        return new PipelineResult(false, document, null, totalTime, errorMessage, failedAtState, failedDocumentPath);
    }

    /**
     * Get the output path if present.
     */
    public Optional<Path> getOutputPath() {
        return Optional.ofNullable(outputPath);
    }

    /**
     * Get the error message if present.
     */
    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    /**
     * Get the failed document path if present.
     */
    public Optional<Path> getFailedDocumentPath() {
        return Optional.ofNullable(failedDocumentPath);
    }
}
