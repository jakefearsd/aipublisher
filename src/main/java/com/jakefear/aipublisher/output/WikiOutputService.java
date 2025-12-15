package com.jakefear.aipublisher.output;

import com.jakefear.aipublisher.config.OutputProperties;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.util.PageNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for writing wiki articles to the filesystem.
 * Handles file generation, existing page discovery, and JSPWiki formatting.
 */
@Service
public class WikiOutputService {

    private static final Logger log = LoggerFactory.getLogger(WikiOutputService.class);

    private final OutputProperties outputProperties;

    public WikiOutputService(OutputProperties outputProperties) {
        this.outputProperties = outputProperties;
    }

    /**
     * Write a completed document to the output directory.
     *
     * @param document The document to write
     * @return The path to the written file
     * @throws IOException if writing fails
     */
    public Path writeDocument(PublishingDocument document) throws IOException {
        FinalArticle article = document.getFinalArticle();
        if (article == null) {
            throw new IllegalStateException("Document has no final article to write");
        }

        Path outputDir = ensureOutputDirectory();
        String filename = generateFilename(document.getPageName());
        Path outputPath = outputDir.resolve(filename);

        String content = formatForOutput(article);

        Files.writeString(outputPath, content, StandardCharsets.UTF_8);

        log.info("Wrote article to {}: {} words, quality score {}",
                outputPath, article.estimateWordCount(), article.qualityScore());

        return outputPath;
    }

    /**
     * Discover existing wiki pages in the output directory.
     * These can be linked to from new articles using [PageName]() syntax.
     *
     * @return Set of existing page names (without .md extension)
     */
    public Set<String> discoverExistingPages() {
        Path outputDir = outputProperties.getDirectoryPath();

        if (!Files.exists(outputDir)) {
            log.debug("Output directory does not exist yet: {}", outputDir);
            return Set.of();
        }

        try (Stream<Path> files = Files.list(outputDir)) {
            Set<String> pages = files
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.endsWith(outputProperties.getFileExtension()))
                    .map(this::removeExtension)
                    .collect(Collectors.toSet());

            log.debug("Discovered {} existing pages in {}", pages.size(), outputDir);
            return pages;
        } catch (IOException e) {
            log.warn("Failed to discover existing pages: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Get the list of existing pages as a sorted list.
     *
     * @return Sorted list of existing page names
     */
    public List<String> getExistingPagesList() {
        return discoverExistingPages().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Check if a page already exists in the output directory.
     *
     * @param pageName The page name to check (without extension)
     * @return true if the page exists
     */
    public boolean pageExists(String pageName) {
        Path outputDir = outputProperties.getDirectoryPath();
        String filename = generateFilename(pageName);
        return Files.exists(outputDir.resolve(filename));
    }

    /**
     * Generate the filename for a page.
     *
     * @param pageName The page name (should be CamelCase)
     * @return The filename with extension
     */
    public String generateFilename(String pageName) {
        return PageNameUtils.toCamelCaseOrDefault(pageName, "UnnamedPage") + outputProperties.getFileExtension();
    }

    /**
     * Format the final article content for JSPWiki output.
     * The article content should already be properly formatted by the editor,
     * so this mainly ensures consistent line endings.
     * No metadata comments are added as they can cause issues with JSPWiki parsing.
     *
     * @param article The final article
     * @return Formatted content ready to write
     */
    public String formatForOutput(FinalArticle article) {
        StringBuilder sb = new StringBuilder();

        // Main content - no header comment as JSPWiki can fail on it
        sb.append(article.wikiContent());

        // Ensure file ends with newline
        if (!article.wikiContent().endsWith("\n")) {
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Ensure the output directory exists, creating it if necessary.
     *
     * @return Path to the output directory
     * @throws IOException if directory creation fails
     */
    public Path ensureOutputDirectory() throws IOException {
        Path outputDir = outputProperties.getDirectoryPath();

        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            log.info("Created output directory: {}", outputDir);
        }

        return outputDir;
    }

    /**
     * Remove the file extension from a filename.
     */
    private String removeExtension(String filename) {
        String ext = outputProperties.getFileExtension();
        if (filename.endsWith(ext)) {
            return filename.substring(0, filename.length() - ext.length());
        }
        return filename;
    }

    /**
     * Write a failed document to the output directory for debugging.
     * The filename will include a suffix indicating the failure state.
     * The content will include comment blocks highlighting the issues.
     *
     * @param document The document that failed
     * @param failedState The state at which the pipeline failed
     * @param errorMessage The error message describing the failure
     * @return The path to the written file, or null if writing failed
     */
    public Path writeFailedDocument(PublishingDocument document, DocumentState failedState, String errorMessage) {
        try {
            Path outputDir = ensureOutputDirectory();
            String filename = generateFailedFilename(document.getPageName(), failedState);
            Path outputPath = outputDir.resolve(filename);

            String content = formatFailedDocument(document, failedState, errorMessage);

            Files.writeString(outputPath, content, StandardCharsets.UTF_8);

            log.info("Wrote failed document to {} for debugging (failed at {})", outputPath, failedState);

            return outputPath;
        } catch (IOException e) {
            log.error("Failed to write failed document for debugging: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Generate the filename for a failed document.
     * Format: PageName_FAILED_STATE_timestamp.md
     *
     * @param pageName The page name
     * @param failedState The state at which it failed
     * @return The filename with failure suffix
     */
    public String generateFailedFilename(String pageName, DocumentState failedState) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String normalizedPageName = PageNameUtils.toCamelCaseOrDefault(pageName, "UnnamedPage");
        return String.format("%s_FAILED_%s_%s%s",
                normalizedPageName,
                failedState.name(),
                timestamp,
                outputProperties.getFileExtension());
    }

    /**
     * Format a failed document with debugging information and highlighted issues.
     *
     * @param document The failed document
     * @param failedState The state at which it failed
     * @param errorMessage The error message
     * @return Formatted content with debugging information
     */
    public String formatFailedDocument(PublishingDocument document, DocumentState failedState, String errorMessage) {
        StringBuilder sb = new StringBuilder();

        // Header with failure information
        sb.append("<!--\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                    PIPELINE FAILURE - DEBUG DOCUMENT                  ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        sb.append("\n");
        sb.append("  Topic: ").append(document.getTopicBrief().topic()).append("\n");
        sb.append("  Failed At: ").append(failedState.name()).append("\n");
        sb.append("  Timestamp: ").append(LocalDateTime.now()).append("\n");
        sb.append("  Error: ").append(errorMessage).append("\n");
        sb.append("-->\n\n");

        // Add fact-check issues if available
        FactCheckReport factCheckReport = document.getFactCheckReport();
        if (factCheckReport != null && failedState == DocumentState.FACT_CHECKING) {
            sb.append(formatFactCheckIssues(factCheckReport));
        }

        // Add the draft content if available
        ArticleDraft draft = document.getDraft();
        if (draft != null) {
            sb.append("---\n\n");
            sb.append("# Draft Content\n\n");
            sb.append(draft.wikiContent());
            sb.append("\n");
        }

        // Add research brief summary if available
        ResearchBrief researchBrief = document.getResearchBrief();
        if (researchBrief != null) {
            sb.append("\n---\n\n");
            sb.append("<!--\n");
            sb.append("## Research Brief Summary\n");
            sb.append("Key Facts: ").append(researchBrief.keyFacts().size()).append("\n");
            sb.append("Sources: ").append(researchBrief.sources().size()).append("\n");
            if (!researchBrief.uncertainAreas().isEmpty()) {
                sb.append("\nUncertain Areas:\n");
                for (String area : researchBrief.uncertainAreas()) {
                    sb.append("  - ").append(area).append("\n");
                }
            }
            sb.append("-->\n");
        }

        return sb.toString();
    }

    /**
     * Format fact-check issues as a highlighted comment block.
     *
     * @param report The fact-check report
     * @return Formatted issues section
     */
    private String formatFactCheckIssues(FactCheckReport report) {
        StringBuilder sb = new StringBuilder();

        sb.append("<!--\n");
        sb.append("╔══════════════════════════════════════════════════════════════════════╗\n");
        sb.append("║                         FACT-CHECK ISSUES                             ║\n");
        sb.append("╚══════════════════════════════════════════════════════════════════════╝\n");
        sb.append("\n");
        sb.append("Overall Confidence: ").append(report.overallConfidence()).append("\n");
        sb.append("Recommendation: ").append(report.recommendedAction()).append("\n");
        sb.append("Verified Claims: ").append(report.verifiedClaims().size()).append("\n");
        sb.append("Questionable Claims: ").append(report.questionableClaims().size()).append("\n");
        sb.append("Consistency Issues: ").append(report.consistencyIssues().size()).append("\n");
        sb.append("\n");

        // Questionable claims
        if (!report.questionableClaims().isEmpty()) {
            sb.append("─────────────────────────────────────────────────────────────────────────\n");
            sb.append("QUESTIONABLE CLAIMS:\n");
            sb.append("─────────────────────────────────────────────────────────────────────────\n\n");

            int claimNum = 1;
            for (QuestionableClaim claim : report.questionableClaims()) {
                sb.append("[").append(claimNum++).append("] CLAIM: \"").append(claim.claim()).append("\"\n");
                sb.append("    ISSUE: ").append(claim.issue()).append("\n");
                if (claim.suggestion() != null && !claim.suggestion().isBlank()) {
                    sb.append("    SUGGESTION: ").append(claim.suggestion()).append("\n");
                }
                sb.append("\n");
            }
        }

        // Consistency issues
        if (!report.consistencyIssues().isEmpty()) {
            sb.append("─────────────────────────────────────────────────────────────────────────\n");
            sb.append("CONSISTENCY ISSUES:\n");
            sb.append("─────────────────────────────────────────────────────────────────────────\n\n");

            int issueNum = 1;
            for (String issue : report.consistencyIssues()) {
                sb.append("[").append(issueNum++).append("] ").append(issue).append("\n");
            }
            sb.append("\n");
        }

        // Verified claims (for reference)
        if (!report.verifiedClaims().isEmpty()) {
            sb.append("─────────────────────────────────────────────────────────────────────────\n");
            sb.append("VERIFIED CLAIMS (for reference):\n");
            sb.append("─────────────────────────────────────────────────────────────────────────\n\n");

            int verifiedNum = 1;
            for (VerifiedClaim claim : report.verifiedClaims()) {
                sb.append("[").append(verifiedNum++).append("] ").append(claim.claim());
                sb.append(" (").append(claim.status()).append(")\n");
            }
            sb.append("\n");
        }

        sb.append("-->\n\n");

        return sb.toString();
    }

    // Getters for testing
    public OutputProperties getOutputProperties() {
        return outputProperties;
    }
}
