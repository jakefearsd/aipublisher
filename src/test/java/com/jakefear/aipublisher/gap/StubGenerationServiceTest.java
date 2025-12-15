package com.jakefear.aipublisher.gap;

import com.jakefear.aipublisher.config.OutputProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StubGenerationService.
 */
class StubGenerationServiceTest {

    @TempDir
    Path tempDir;

    private GapDetectionService mockGapDetectionService;
    private StubWriterAgent mockStubWriterAgent;
    private OutputProperties mockOutputProperties;
    private StubGenerationService service;

    private StringWriter outputCapture;
    private PrintWriter out;

    @BeforeEach
    void setUp() {
        mockGapDetectionService = mock(GapDetectionService.class);
        mockStubWriterAgent = mock(StubWriterAgent.class);
        mockOutputProperties = mock(OutputProperties.class);

        when(mockOutputProperties.getDirectoryPath()).thenReturn(tempDir);
        when(mockOutputProperties.getFileExtension()).thenReturn(".txt");

        service = new StubGenerationService(
                mockGapDetectionService,
                mockStubWriterAgent,
                mockOutputProperties
        );

        outputCapture = new StringWriter();
        out = new PrintWriter(outputCapture);
    }

    @Test
    void generateStubs_noGaps_returnsEmptyResult() throws IOException {
        when(mockGapDetectionService.detectAndCategorizeGaps(anyString()))
                .thenReturn(List.of());

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        assertEquals(0, result.gapsDetected());
        assertEquals(0, result.stubsGenerated());
        assertEquals(0, result.totalGenerated());
        assertFalse(result.hasReviewItems());

        String output = outputCapture.toString();
        assertTrue(output.contains("No gaps detected"));
    }

    @Test
    void generateStubs_definitionsOnly_generatesStubs() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.definition("Present Value", List.of("Article1"), "Finance"),
                GapConcept.definition("Discount Rate", List.of("Article2"), "Finance")
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        when(mockStubWriterAgent.generateStub(any(), anyString(), anyString()))
                .thenReturn("!!! Generated content\nTest stub");

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        assertEquals(2, result.gapsDetected());
        assertEquals(2, result.stubsGenerated());
        assertEquals(0, result.redirectsGenerated());
        assertEquals(2, result.generatedFiles().size());

        // Verify files were created
        assertTrue(Files.exists(tempDir.resolve("PresentValue.txt")));
        assertTrue(Files.exists(tempDir.resolve("DiscountRate.txt")));
    }

    @Test
    void generateStubs_redirectsOnly_createsRedirects() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.redirect("CI", "CompoundInterest"),
                GapConcept.redirect("present value", "PresentValue")
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        when(mockStubWriterAgent.generateStub(any(), anyString(), anyString()))
                .thenReturn("This page redirects to [Target].");

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        assertEquals(2, result.gapsDetected());
        assertEquals(0, result.stubsGenerated());
        assertEquals(2, result.redirectsGenerated());
    }

    @Test
    void generateStubs_mixedTypes_handlesProperly() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.definition("NewTerm", List.of("Source"), "Category"),
                GapConcept.redirect("alias", "Target"),
                GapConcept.of("BigTopic", GapType.FULL_ARTICLE),
                GapConcept.of("generic", GapType.IGNORE)
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        // Use lenient stubbing for any gap concept
        when(mockStubWriterAgent.generateStub(any(GapConcept.class), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    GapConcept gap = invocation.getArgument(0);
                    if (gap.type() == GapType.DEFINITION) {
                        return "Definition content";
                    } else if (gap.type() == GapType.REDIRECT) {
                        return "Redirect content";
                    }
                    return null;
                });

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        assertEquals(4, result.gapsDetected());
        assertEquals(1, result.stubsGenerated());
        assertEquals(1, result.redirectsGenerated());
        assertEquals(1, result.ignored());
        assertEquals(1, result.flaggedForReview());
        assertTrue(result.hasReviewItems());
        assertEquals("BigTopic", result.reviewNeeded().get(0).name());
    }

    @Test
    void generateStubs_generationFailure_countsAsFailed() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.definition("Term1", List.of(), null),
                GapConcept.definition("Term2", List.of(), null)
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        when(mockStubWriterAgent.generateStub(any(), anyString(), anyString()))
                .thenReturn("Success")
                .thenReturn(null); // Second generation fails

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        assertEquals(2, result.gapsDetected());
        assertEquals(1, result.stubsGenerated());
        assertEquals(1, result.failed());
    }

    @Test
    void generateStubs_existingFile_skipped() throws IOException {
        // Create existing file
        Files.writeString(tempDir.resolve("ExistingTerm.txt"), "Existing content");

        List<GapConcept> gaps = List.of(
                GapConcept.definition("Existing Term", List.of(), null)
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        when(mockStubWriterAgent.generateStub(any(), anyString(), anyString()))
                .thenReturn("New content");

        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", out);

        // File should not be overwritten, counts as failed
        assertEquals(0, result.stubsGenerated());
        assertEquals("Existing content", Files.readString(tempDir.resolve("ExistingTerm.txt")));
    }

    @Test
    void analyzeGaps_reportsGapsWithoutGeneration() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.definition("Term1", List.of("Source1"), "Cat1"),
                GapConcept.of("Term2", GapType.FULL_ARTICLE),
                GapConcept.of("Term3", GapType.IGNORE)
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);

        List<GapConcept> result = service.analyzeGaps("Test Universe", out);

        assertEquals(3, result.size());
        verify(mockStubWriterAgent, never()).generateStub(any(), anyString(), anyString());

        String output = outputCapture.toString();
        assertTrue(output.contains("3 gap concepts"));
    }

    @Test
    void analyzeGaps_noGaps_reportsClean() throws IOException {
        when(mockGapDetectionService.detectAndCategorizeGaps(anyString()))
                .thenReturn(List.of());

        List<GapConcept> result = service.analyzeGaps("Test Universe", out);

        assertTrue(result.isEmpty());
        String output = outputCapture.toString();
        assertTrue(output.contains("No gaps detected"));
    }

    @Test
    void writeStubFile_createsFileWithCorrectName() throws IOException {
        GapConcept gap = GapConcept.definition("Present Value", List.of(), null);
        String content = "Test content";

        Path result = service.writeStubFile(gap, content);

        assertNotNull(result);
        assertEquals("PresentValue.txt", result.getFileName().toString());
        assertTrue(Files.exists(result));
        assertEquals("Test content", Files.readString(result));
    }

    @Test
    void writeStubFile_existingFile_returnsNull() throws IOException {
        Files.writeString(tempDir.resolve("Existing.txt"), "Original");

        GapConcept gap = new GapConcept("Existing", "Existing", GapType.DEFINITION, List.of(), null, null);
        String content = "New content";

        Path result = service.writeStubFile(gap, content);

        assertNull(result);
        assertEquals("Original", Files.readString(tempDir.resolve("Existing.txt")));
    }

    @Test
    void generateStubs_nullOutput_stillWorks() throws IOException {
        List<GapConcept> gaps = List.of(
                GapConcept.definition("Term", List.of(), null)
        );

        when(mockGapDetectionService.detectAndCategorizeGaps(anyString())).thenReturn(gaps);
        when(mockStubWriterAgent.generateStub(any(), anyString(), anyString()))
                .thenReturn("Content");

        // Pass null PrintWriter
        StubGenerationService.StubGenerationResult result =
                service.generateStubs("Test Universe", "general readers", null);

        assertEquals(1, result.stubsGenerated());
    }

    @Test
    void stubGenerationResult_totalGenerated() {
        // Parameters: gapsDetected, stubsGenerated, redirectsGenerated, ignored, flaggedForReview, skipped, failed, reviewNeeded, generatedFiles
        StubGenerationService.StubGenerationResult result =
                new StubGenerationService.StubGenerationResult(
                        10, 5, 3, 1, 1, 0, 0, List.of(), List.of()
                );

        assertEquals(8, result.totalGenerated()); // 5 stubs + 3 redirects
    }

    @Test
    void stubGenerationResult_hasReviewItems() {
        // Parameters: gapsDetected, stubsGenerated, redirectsGenerated, ignored, flaggedForReview, skipped, failed, reviewNeeded, generatedFiles
        StubGenerationService.StubGenerationResult withReview =
                new StubGenerationService.StubGenerationResult(
                        5, 2, 1, 1, 1, 0, 0,
                        List.of(GapConcept.of("BigTopic", GapType.FULL_ARTICLE)),
                        List.of()
                );

        StubGenerationService.StubGenerationResult withoutReview =
                new StubGenerationService.StubGenerationResult(
                        3, 2, 1, 0, 0, 0, 0, List.of(), List.of()
                );

        assertTrue(withReview.hasReviewItems());
        assertFalse(withoutReview.hasReviewItems());
    }
}
