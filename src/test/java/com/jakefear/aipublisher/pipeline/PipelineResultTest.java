package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.document.DocumentState;
import com.jakefear.aipublisher.document.PublishingDocument;
import com.jakefear.aipublisher.document.TopicBrief;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PipelineResult")
class PipelineResultTest {

    private final TopicBrief topicBrief = TopicBrief.simple("Test", "testers", 500);
    private final PublishingDocument document = new PublishingDocument(topicBrief);
    private final Duration duration = Duration.ofSeconds(10);

    @Nested
    @DisplayName("Success Factory Method")
    class SuccessFactoryMethod {

        @Test
        @DisplayName("Creates success result with all fields populated")
        void createsSuccessResult() {
            Path outputPath = Path.of("/output/test.txt");

            PipelineResult result = PipelineResult.success(document, outputPath, duration);

            assertTrue(result.success());
            assertEquals(document, result.document());
            assertEquals(outputPath, result.outputPath());
            assertEquals(duration, result.totalTime());
            assertNull(result.errorMessage());
            assertNull(result.failedAtState());
            assertNull(result.failedDocumentPath());
        }

        @Test
        @DisplayName("getOutputPath returns Optional with value for success")
        void getOutputPathReturnsOptionalWithValue() {
            Path outputPath = Path.of("/output/test.txt");

            PipelineResult result = PipelineResult.success(document, outputPath, duration);

            assertTrue(result.getOutputPath().isPresent());
            assertEquals(outputPath, result.getOutputPath().get());
        }

        @Test
        @DisplayName("getErrorMessage returns empty Optional for success")
        void getErrorMessageReturnsEmptyOptional() {
            PipelineResult result = PipelineResult.success(document, Path.of("/output"), duration);

            assertFalse(result.getErrorMessage().isPresent());
        }

        @Test
        @DisplayName("getFailedDocumentPath returns empty Optional for success")
        void getFailedDocumentPathReturnsEmptyOptional() {
            PipelineResult result = PipelineResult.success(document, Path.of("/output"), duration);

            assertFalse(result.getFailedDocumentPath().isPresent());
        }
    }

    @Nested
    @DisplayName("Failure Factory Method (without saved document)")
    class FailureFactoryMethodBasic {

        @Test
        @DisplayName("Creates failure result with error message and state")
        void createsFailureResult() {
            String errorMessage = "Test error";
            DocumentState failedState = DocumentState.DRAFTING;

            PipelineResult result = PipelineResult.failure(document, errorMessage, failedState, duration);

            assertFalse(result.success());
            assertEquals(document, result.document());
            assertNull(result.outputPath());
            assertEquals(duration, result.totalTime());
            assertEquals(errorMessage, result.errorMessage());
            assertEquals(failedState, result.failedAtState());
            assertNull(result.failedDocumentPath());
        }

        @Test
        @DisplayName("getOutputPath returns empty Optional for failure")
        void getOutputPathReturnsEmptyOptional() {
            PipelineResult result = PipelineResult.failure(document, "error", DocumentState.DRAFTING, duration);

            assertFalse(result.getOutputPath().isPresent());
        }

        @Test
        @DisplayName("getErrorMessage returns Optional with value for failure")
        void getErrorMessageReturnsOptionalWithValue() {
            String errorMessage = "Something went wrong";

            PipelineResult result = PipelineResult.failure(document, errorMessage, DocumentState.DRAFTING, duration);

            assertTrue(result.getErrorMessage().isPresent());
            assertEquals(errorMessage, result.getErrorMessage().get());
        }

        @Test
        @DisplayName("getFailedDocumentPath returns empty Optional when not provided")
        void getFailedDocumentPathReturnsEmptyOptional() {
            PipelineResult result = PipelineResult.failure(document, "error", DocumentState.DRAFTING, duration);

            assertFalse(result.getFailedDocumentPath().isPresent());
        }
    }

    @Nested
    @DisplayName("Failure Factory Method (with saved document)")
    class FailureFactoryMethodWithDebugPath {

        @Test
        @DisplayName("Creates failure result with failed document path")
        void createsFailureResultWithDebugPath() {
            String errorMessage = "Test error";
            DocumentState failedState = DocumentState.FACT_CHECKING;
            Path failedDocPath = Path.of("/debug/failed-doc.txt");

            PipelineResult result = PipelineResult.failure(document, errorMessage, failedState, duration, failedDocPath);

            assertFalse(result.success());
            assertEquals(failedDocPath, result.failedDocumentPath());
        }

        @Test
        @DisplayName("getFailedDocumentPath returns Optional with value when provided")
        void getFailedDocumentPathReturnsOptionalWithValue() {
            Path failedDocPath = Path.of("/debug/failed-doc.txt");

            PipelineResult result = PipelineResult.failure(document, "error", DocumentState.DRAFTING, duration, failedDocPath);

            assertTrue(result.getFailedDocumentPath().isPresent());
            assertEquals(failedDocPath, result.getFailedDocumentPath().get());
        }
    }

    @Nested
    @DisplayName("Null Handling in Optional Getters")
    class NullHandlingInOptionalGetters {

        @Test
        @DisplayName("Handles null outputPath gracefully")
        void handlesNullOutputPath() {
            PipelineResult result = PipelineResult.failure(document, "error", DocumentState.DRAFTING, duration);

            assertNull(result.outputPath());
            assertFalse(result.getOutputPath().isPresent());
            assertDoesNotThrow(() -> result.getOutputPath().orElse(null));
        }

        @Test
        @DisplayName("Handles null errorMessage gracefully")
        void handlesNullErrorMessage() {
            PipelineResult result = PipelineResult.success(document, Path.of("/output"), duration);

            assertNull(result.errorMessage());
            assertFalse(result.getErrorMessage().isPresent());
            assertDoesNotThrow(() -> result.getErrorMessage().orElse(null));
        }

        @Test
        @DisplayName("Handles null failedDocumentPath gracefully")
        void handlesNullFailedDocumentPath() {
            PipelineResult result = PipelineResult.failure(document, "error", DocumentState.DRAFTING, duration);

            assertNull(result.failedDocumentPath());
            assertFalse(result.getFailedDocumentPath().isPresent());
            assertDoesNotThrow(() -> result.getFailedDocumentPath().orElse(null));
        }

        @Test
        @DisplayName("All Optional getters work with success result")
        void allOptionalGettersWorkWithSuccess() {
            Path outputPath = Path.of("/output/test.txt");
            PipelineResult result = PipelineResult.success(document, outputPath, duration);

            // These should not throw
            assertDoesNotThrow(() -> {
                result.getOutputPath().orElse(Path.of("/default"));
                result.getErrorMessage().orElse("default");
                result.getFailedDocumentPath().orElse(Path.of("/default"));
            });
        }

        @Test
        @DisplayName("All Optional getters work with failure result")
        void allOptionalGettersWorkWithFailure() {
            PipelineResult result = PipelineResult.failure(
                    document, "error", DocumentState.DRAFTING, duration, Path.of("/debug"));

            // These should not throw
            assertDoesNotThrow(() -> {
                result.getOutputPath().orElse(Path.of("/default"));
                result.getErrorMessage().orElse("default");
                result.getFailedDocumentPath().orElse(Path.of("/default"));
            });
        }
    }

    @Nested
    @DisplayName("Different Failure States")
    class DifferentFailureStates {

        @Test
        @DisplayName("Can record failure at RESEARCHING state")
        void canRecordFailureAtResearching() {
            PipelineResult result = PipelineResult.failure(document, "Research failed", DocumentState.RESEARCHING, duration);

            assertEquals(DocumentState.RESEARCHING, result.failedAtState());
        }

        @Test
        @DisplayName("Can record failure at DRAFTING state")
        void canRecordFailureAtDrafting() {
            PipelineResult result = PipelineResult.failure(document, "Draft failed", DocumentState.DRAFTING, duration);

            assertEquals(DocumentState.DRAFTING, result.failedAtState());
        }

        @Test
        @DisplayName("Can record failure at FACT_CHECKING state")
        void canRecordFailureAtFactChecking() {
            PipelineResult result = PipelineResult.failure(document, "Fact check failed", DocumentState.FACT_CHECKING, duration);

            assertEquals(DocumentState.FACT_CHECKING, result.failedAtState());
        }

        @Test
        @DisplayName("Can record failure at EDITING state")
        void canRecordFailureAtEditing() {
            PipelineResult result = PipelineResult.failure(document, "Edit failed", DocumentState.EDITING, duration);

            assertEquals(DocumentState.EDITING, result.failedAtState());
        }

        @Test
        @DisplayName("Can record failure at CRITIQUING state")
        void canRecordFailureAtCritiquing() {
            PipelineResult result = PipelineResult.failure(document, "Critique failed", DocumentState.CRITIQUING, duration);

            assertEquals(DocumentState.CRITIQUING, result.failedAtState());
        }
    }

    @Nested
    @DisplayName("Skip Phase Scenario Support")
    class SkipPhaseScenarioSupport {

        @Test
        @DisplayName("Success result can have null FactCheckReport in document")
        void successResultCanHaveNullFactCheckReport() {
            // This simulates the skip-fact-check scenario
            PublishingDocument docWithoutFactCheck = new PublishingDocument(topicBrief);
            // FactCheckReport is not set (null)

            PipelineResult result = PipelineResult.success(docWithoutFactCheck, Path.of("/output"), duration);

            assertTrue(result.success());
            assertNull(result.document().getFactCheckReport());
        }

        @Test
        @DisplayName("Success result can have null CriticReport in document")
        void successResultCanHaveNullCriticReport() {
            // This simulates the skip-critique scenario
            PublishingDocument docWithoutCritique = new PublishingDocument(topicBrief);
            // CriticReport is not set (null)

            PipelineResult result = PipelineResult.success(docWithoutCritique, Path.of("/output"), duration);

            assertTrue(result.success());
            assertNull(result.document().getCriticReport());
        }

        @Test
        @DisplayName("Success result can have both null reports in document")
        void successResultCanHaveBothNullReports() {
            // This simulates the combined skip scenario
            PublishingDocument docWithBothSkipped = new PublishingDocument(topicBrief);
            // Neither FactCheckReport nor CriticReport is set

            PipelineResult result = PipelineResult.success(docWithBothSkipped, Path.of("/output"), duration);

            assertTrue(result.success());
            assertNull(result.document().getFactCheckReport());
            assertNull(result.document().getCriticReport());
        }
    }
}
