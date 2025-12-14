package com.jakefear.aipublisher.approval;

import com.jakefear.aipublisher.document.PublishingDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.concurrent.*;

/**
 * Console-based approval callback for interactive CLI usage.
 *
 * Displays approval request details and prompts for user input.
 */
@Component
@ConditionalOnProperty(name = "pipeline.approval.auto-approve", havingValue = "false")
public class ConsoleApprovalCallback implements ApprovalCallback {

    private static final Logger log = LoggerFactory.getLogger(ConsoleApprovalCallback.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(30);

    private final Duration timeout;
    private final BufferedReader reader;

    public ConsoleApprovalCallback() {
        this(DEFAULT_TIMEOUT, new BufferedReader(new InputStreamReader(System.in)));
    }

    // Constructor for testing
    ConsoleApprovalCallback(Duration timeout, BufferedReader reader) {
        this.timeout = timeout;
        this.reader = reader;
    }

    @Override
    public ApprovalDecision requestApproval(ApprovalRequest request) throws ApprovalTimeoutException {
        displayApprovalRequest(request);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<ApprovalDecision> future = executor.submit(() -> readDecision(request));

        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ApprovalTimeoutException("Approval request timed out after " + timeout.toMinutes() + " minutes");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApprovalTimeoutException("Approval request was interrupted");
        } catch (ExecutionException e) {
            throw new RuntimeException("Error reading approval decision", e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }

    private void displayApprovalRequest(ApprovalRequest request) {
        PublishingDocument doc = request.document();

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("APPROVAL REQUIRED");
        System.out.println("=".repeat(70));
        System.out.println();
        System.out.println("Request ID: " + request.id());
        System.out.println("Phase: " + request.atState());
        System.out.println("Topic: " + doc.getTopicBrief().topic());
        System.out.println();
        System.out.println(request.summary());
        System.out.println();

        // Show phase-specific details
        switch (request.atState()) {
            case RESEARCHING -> {
                if (doc.getResearchBrief() != null) {
                    System.out.println("Key Facts:");
                    doc.getResearchBrief().keyFacts().forEach(f ->
                            System.out.println("  - " + f.fact()));
                    System.out.println();
                    System.out.println("Suggested Outline: " + doc.getResearchBrief().suggestedOutline());
                }
            }
            case DRAFTING -> {
                if (doc.getDraft() != null) {
                    System.out.println("Summary: " + doc.getDraft().summary());
                    System.out.println("Categories: " + doc.getDraft().categories());
                    System.out.println();
                    System.out.println("Content Preview (first 500 chars):");
                    String content = doc.getDraft().wikiContent();
                    System.out.println(content.substring(0, Math.min(500, content.length())));
                    if (content.length() > 500) System.out.println("...");
                }
            }
            case FACT_CHECKING -> {
                if (doc.getFactCheckReport() != null) {
                    var report = doc.getFactCheckReport();
                    System.out.println("Verified Claims: " + report.verifiedClaims().size());
                    System.out.println("Questionable Claims: " + report.questionableClaims().size());
                    if (!report.questionableClaims().isEmpty()) {
                        System.out.println("Issues:");
                        report.questionableClaims().forEach(c ->
                                System.out.println("  - " + c.claim() + ": " + c.issue()));
                    }
                }
            }
            case EDITING -> {
                if (doc.getFinalArticle() != null) {
                    System.out.println("Quality Score: " + doc.getFinalArticle().qualityScore());
                    System.out.println("Word Count: " + doc.getFinalArticle().estimateWordCount());
                    System.out.println("Edit Summary: " + doc.getFinalArticle().editSummary());
                }
            }
            default -> { }
        }

        System.out.println();
        System.out.println("-".repeat(70));
        System.out.println("Options:");
        System.out.println("  [A]pprove  - Continue to next phase");
        System.out.println("  [R]eject   - Stop the pipeline");
        System.out.println("  [C]hanges  - Request changes and retry");
        System.out.println("-".repeat(70));
    }

    private ApprovalDecision readDecision(ApprovalRequest request) {
        while (true) {
            System.out.print("Enter decision (A/R/C): ");
            System.out.flush();
            try {
                String input = reader.readLine();
                if (input == null) {
                    // EOF reached (no stdin available) - auto-approve to avoid hanging
                    log.warn("No stdin available for approval prompt, auto-approving");
                    return ApprovalDecision.approve(request.id(), "auto-approved-no-stdin");
                }

                input = input.trim().toUpperCase();

                switch (input) {
                    case "A", "APPROVE" -> {
                        return ApprovalDecision.approve(request.id(), "console-user");
                    }
                    case "R", "REJECT" -> {
                        System.out.print("Reason for rejection: ");
                        String reason = reader.readLine();
                        return ApprovalDecision.reject(request.id(), "console-user", reason);
                    }
                    case "C", "CHANGES" -> {
                        System.out.print("Feedback for changes: ");
                        String feedback = reader.readLine();
                        return ApprovalDecision.requestChanges(request.id(), "console-user", feedback);
                    }
                    default -> System.out.println("Invalid option. Please enter A, R, or C.");
                }
            } catch (IOException e) {
                log.error("Error reading input", e);
                throw new RuntimeException("Failed to read approval decision", e);
            }
        }
    }
}
