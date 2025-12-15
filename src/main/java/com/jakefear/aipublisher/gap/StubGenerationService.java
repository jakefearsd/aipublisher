package com.jakefear.aipublisher.gap;

import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.util.PageNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for orchestrating stub page generation.
 * Detects gaps in wiki content and generates brief definition/redirect pages.
 */
@Service
public class StubGenerationService {

    private static final Logger log = LoggerFactory.getLogger(StubGenerationService.class);

    private final GapDetectionService gapDetectionService;
    private final StubWriterAgent stubWriterAgent;
    private final OutputProperties outputProperties;

    public StubGenerationService(
            GapDetectionService gapDetectionService,
            StubWriterAgent stubWriterAgent,
            OutputProperties outputProperties) {
        this.gapDetectionService = gapDetectionService;
        this.stubWriterAgent = stubWriterAgent;
        this.outputProperties = outputProperties;
    }

    /**
     * Result of stub generation.
     */
    public record StubGenerationResult(
            int gapsDetected,
            int stubsGenerated,
            int redirectsGenerated,
            int ignored,
            int flaggedForReview,
            int skipped,
            int failed,
            List<GapConcept> reviewNeeded,
            List<Path> generatedFiles
    ) {
        public boolean hasReviewItems() {
            return !reviewNeeded.isEmpty();
        }

        public int totalGenerated() {
            return stubsGenerated + redirectsGenerated;
        }
    }

    /**
     * Result of a single stub write attempt.
     */
    private enum WriteResult {
        SUCCESS,
        SKIPPED,  // File already exists
        FAILED    // Actual error
    }

    /**
     * Result holder for stub generation attempt.
     */
    private record StubWriteAttempt(WriteResult result, Path path) {
        static StubWriteAttempt success(Path path) {
            return new StubWriteAttempt(WriteResult.SUCCESS, path);
        }
        static StubWriteAttempt skipped() {
            return new StubWriteAttempt(WriteResult.SKIPPED, null);
        }
        static StubWriteAttempt failed() {
            return new StubWriteAttempt(WriteResult.FAILED, null);
        }
    }

    /**
     * Detect gaps and generate stubs for existing wiki content.
     *
     * @param universeName The universe name for context
     * @param targetAudience The target audience
     * @param out PrintWriter for status output (may be null)
     * @return Result of the stub generation
     */
    public StubGenerationResult generateStubs(String universeName, String targetAudience, PrintWriter out) throws IOException {
        print(out, "Analyzing wiki content for gaps...");

        // Detect and categorize gaps
        List<GapConcept> gaps = gapDetectionService.detectAndCategorizeGaps(universeName);

        if (gaps.isEmpty()) {
            print(out, "No gaps detected - all internal links have corresponding pages.");
            return new StubGenerationResult(0, 0, 0, 0, 0, 0, 0, List.of(), List.of());
        }

        print(out, String.format("Detected %d gap concepts", gaps.size()));

        // Categorize results
        List<GapConcept> definitions = new ArrayList<>();
        List<GapConcept> redirects = new ArrayList<>();
        List<GapConcept> fullArticles = new ArrayList<>();
        List<GapConcept> ignored = new ArrayList<>();

        for (GapConcept gap : gaps) {
            switch (gap.type()) {
                case DEFINITION -> definitions.add(gap);
                case REDIRECT -> redirects.add(gap);
                case FULL_ARTICLE -> fullArticles.add(gap);
                case IGNORE -> ignored.add(gap);
            }
        }

        print(out, String.format("  - Definitions to generate: %d", definitions.size()));
        print(out, String.format("  - Redirects to create: %d", redirects.size()));
        print(out, String.format("  - Full articles needed: %d (flagged for review)", fullArticles.size()));
        print(out, String.format("  - Ignored (too generic): %d", ignored.size()));

        // Generate stubs
        List<Path> generatedFiles = new ArrayList<>();
        int stubsGenerated = 0;
        int redirectsGenerated = 0;
        int skipped = 0;
        int failed = 0;

        // Generate definition pages
        if (!definitions.isEmpty()) {
            print(out, "\nGenerating definition stubs...");
            for (GapConcept gap : definitions) {
                StubWriteAttempt attempt = generateAndWriteStub(gap, universeName, targetAudience, out);
                switch (attempt.result()) {
                    case SUCCESS -> {
                        generatedFiles.add(attempt.path());
                        stubsGenerated++;
                    }
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
            }
        }

        // Generate redirect pages
        if (!redirects.isEmpty()) {
            print(out, "\nCreating redirect pages...");
            for (GapConcept gap : redirects) {
                StubWriteAttempt attempt = generateAndWriteStub(gap, universeName, targetAudience, out);
                switch (attempt.result()) {
                    case SUCCESS -> {
                        generatedFiles.add(attempt.path());
                        redirectsGenerated++;
                    }
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
            }
        }

        // Report full articles that need review
        if (!fullArticles.isEmpty()) {
            print(out, "\nGaps requiring full articles (add to universe for generation):");
            for (GapConcept gap : fullArticles) {
                print(out, String.format("  - %s (referenced by: %s)",
                        gap.name(),
                        String.join(", ", gap.referencedBy())));
            }
        }

        return new StubGenerationResult(
                gaps.size(),
                stubsGenerated,
                redirectsGenerated,
                ignored.size(),
                fullArticles.size(),
                skipped,
                failed,
                fullArticles,
                generatedFiles
        );
    }

    /**
     * Just analyze gaps without generating anything.
     *
     * @param universeName The universe name for context
     * @param out PrintWriter for output
     * @return List of detected gaps
     */
    public List<GapConcept> analyzeGaps(String universeName, PrintWriter out) throws IOException {
        print(out, "Analyzing wiki content for gaps...");

        List<GapConcept> gaps = gapDetectionService.detectAndCategorizeGaps(universeName);

        if (gaps.isEmpty()) {
            print(out, "No gaps detected - all internal links have corresponding pages.");
            return gaps;
        }

        print(out, String.format("\nDetected %d gap concepts:\n", gaps.size()));

        // Group by type for display
        for (GapType type : GapType.values()) {
            List<GapConcept> ofType = gaps.stream()
                    .filter(g -> g.type() == type)
                    .toList();

            if (!ofType.isEmpty()) {
                print(out, String.format("%s (%d):", type.getDisplayName(), ofType.size()));
                for (GapConcept gap : ofType) {
                    print(out, "  - " + gap.toDisplayString());
                }
                print(out, "");
            }
        }

        return gaps;
    }

    /**
     * Generate stub content and write to file.
     *
     * @return Path to written file, or null if failed
     */
    private StubWriteAttempt generateAndWriteStub(GapConcept gap, String universeName, String targetAudience, PrintWriter out) {
        try {
            String content = stubWriterAgent.generateStub(gap, universeName, targetAudience);
            if (content == null) {
                print(out, String.format("  ✗ Failed to generate: %s", gap.name()));
                return StubWriteAttempt.failed();
            }

            Path outputPath = writeStubFile(gap, content);
            if (outputPath == null) {
                // File already exists - this is not an error, just skip it
                print(out, String.format("  ⊘ Skipped (exists): %s", gap.pageName()));
                return StubWriteAttempt.skipped();
            }
            print(out, String.format("  ✓ Generated: %s", outputPath.getFileName()));
            return StubWriteAttempt.success(outputPath);

        } catch (Exception e) {
            log.error("Failed to generate stub for '{}': {}", gap.name(), e.getMessage());
            print(out, String.format("  ✗ Error generating %s: %s", gap.name(), e.getMessage()));
            return StubWriteAttempt.failed();
        }
    }

    /**
     * Write stub content to a file.
     */
    Path writeStubFile(GapConcept gap, String content) throws IOException {
        Path outputDir = outputProperties.getDirectoryPath();
        String extension = outputProperties.getFileExtension();

        Files.createDirectories(outputDir);

        String filename = PageNameUtils.toCamelCaseOrDefault(gap.name(), "UnnamedPage") + extension;
        Path outputPath = outputDir.resolve(filename);

        // Don't overwrite existing files
        if (Files.exists(outputPath)) {
            log.warn("File already exists, skipping: {}", outputPath);
            return null;
        }

        Files.writeString(outputPath, content);
        return outputPath;
    }

    private void print(PrintWriter out, String message) {
        if (out != null) {
            out.println(message);
            out.flush();
        }
        log.info(message);
    }
}
