package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.agent.*;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.config.QualityProperties;
import com.jakefear.aipublisher.document.*;
import com.jakefear.aipublisher.glossary.GlossaryService;
import com.jakefear.aipublisher.monitoring.PipelineMonitoringService;
import com.jakefear.aipublisher.output.WikiOutputService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Orchestrates the complete publishing pipeline from topic brief to published article.
 *
 * The pipeline executes these phases in order:
 * 1. Research - Gather source material
 * 2. Draft - Write the article
 * 3. Fact Check - Verify claims
 * 4. Edit - Polish and finalize
 * 5. Critique - Final quality and syntax review
 * 6. Publish - Write to output
 *
 * Each phase can be configured with approval checkpoints and revision cycles.
 */
@Service
public class PublishingPipeline {

    private static final Logger log = LoggerFactory.getLogger(PublishingPipeline.class);

    private final ResearchAgent researchAgent;
    private final WriterAgent writerAgent;
    private final FactCheckerAgent factCheckerAgent;
    private final EditorAgent editorAgent;
    private final CriticAgent criticAgent;
    private final WikiOutputService outputService;
    private final ApprovalService approvalService;
    private final PipelineMonitoringService monitoringService;
    private final GlossaryService glossaryService;
    private final PipelineProperties pipelineProperties;
    private final QualityProperties qualityProperties;

    public PublishingPipeline(
            ResearchAgent researchAgent,
            WriterAgent writerAgent,
            FactCheckerAgent factCheckerAgent,
            EditorAgent editorAgent,
            CriticAgent criticAgent,
            WikiOutputService outputService,
            ApprovalService approvalService,
            PipelineMonitoringService monitoringService,
            GlossaryService glossaryService,
            PipelineProperties pipelineProperties,
            QualityProperties qualityProperties) {
        this.researchAgent = researchAgent;
        this.writerAgent = writerAgent;
        this.factCheckerAgent = factCheckerAgent;
        this.editorAgent = editorAgent;
        this.criticAgent = criticAgent;
        this.outputService = outputService;
        this.approvalService = approvalService;
        this.monitoringService = monitoringService;
        this.glossaryService = glossaryService;
        this.pipelineProperties = pipelineProperties;
        this.qualityProperties = qualityProperties;
    }

    /**
     * Execute the full pipeline for a topic brief.
     *
     * @param topicBrief The topic to create an article about
     * @return Result containing the published document and output path
     */
    public PipelineResult execute(TopicBrief topicBrief) {
        Instant startTime = Instant.now();
        PublishingDocument document = new PublishingDocument(topicBrief);

        log.info("Starting pipeline for topic: {}", topicBrief.topic());
        monitoringService.pipelineStarted(document);

        try {
            // Phase 1: Research
            document = executeResearchPhase(document);

            // Phase 2: Drafting (with revision loop)
            document = executeDraftingPhase(document);

            // Phase 3: Fact Checking (with revision loop)
            document = executeFactCheckPhase(document);

            // Phase 4: Editing
            document = executeEditingPhase(document);

            // Phase 5: Critique (final quality check)
            document = executeCritiquePhase(document);

            // Phase 6: Publishing
            Path outputPath = executePublishPhase(document);

            Duration totalTime = Duration.between(startTime, Instant.now());
            log.info("Pipeline completed successfully in {} ms", totalTime.toMillis());
            monitoringService.pipelineCompleted(document, totalTime);

            return PipelineResult.success(document, outputPath, totalTime);

        } catch (PipelineException e) {
            Duration totalTime = Duration.between(startTime, Instant.now());
            log.error("Pipeline failed at {}: {}", e.getFailedAtState(), e.getMessage());
            monitoringService.pipelineFailed(document, e.getFailedAtState(), e.getMessage());

            // Save the failed document for debugging
            Path failedDocPath = saveFailedDocument(document, e.getFailedAtState(), e.getMessage());

            return PipelineResult.failure(document, e.getMessage(), e.getFailedAtState(), totalTime, failedDocPath);

        } catch (Exception e) {
            Duration totalTime = Duration.between(startTime, Instant.now());
            log.error("Pipeline failed with unexpected error: {}", e.getMessage(), e);
            monitoringService.pipelineFailed(document, document.getState(), e.getMessage());

            // Save the failed document for debugging
            Path failedDocPath = saveFailedDocument(document, document.getState(), e.getMessage());

            return PipelineResult.failure(document, e.getMessage(), document.getState(), totalTime, failedDocPath);
        }
    }

    /**
     * Execute the research phase.
     */
    private PublishingDocument executeResearchPhase(PublishingDocument document) {
        log.info("Phase 1: Research");
        DocumentState previousState = document.getState();
        document.transitionTo(DocumentState.RESEARCHING);
        monitoringService.phaseStarted(document, previousState, DocumentState.RESEARCHING);

        try {
            Instant phaseStart = Instant.now();
            document = researchAgent.process(document);
            monitoringService.recordAgentProcessing(AgentRole.RESEARCHER, Duration.between(phaseStart, Instant.now()));

            if (!researchAgent.validate(document)) {
                throw new PipelineException("Research validation failed",
                        DocumentState.RESEARCHING);
            }

            // Store glossary terms from research for cross-article consistency
            ResearchBrief researchBrief = document.getResearchBrief();
            if (!researchBrief.glossary().isEmpty()) {
                glossaryService.addFromMap(researchBrief.glossary(), document.getPageName());
                log.debug("Added {} glossary terms from research", researchBrief.glossary().size());
            }

            String summary = String.format("%d key facts gathered", researchBrief.keyFacts().size());
            log.info("Research complete: {}", summary);
            monitoringService.phaseCompleted(document, DocumentState.RESEARCHING, summary);

            // Check for approval after research
            checkApproval(document);

            return document;

        } catch (AgentException e) {
            throw new PipelineException("Research phase failed: " + e.getMessage(),
                    DocumentState.RESEARCHING, e);
        }
    }

    /**
     * Execute the drafting phase with revision support.
     */
    private PublishingDocument executeDraftingPhase(PublishingDocument document) {
        log.info("Phase 2: Drafting");
        DocumentState previousState = document.getState();
        document.transitionTo(DocumentState.DRAFTING);
        monitoringService.phaseStarted(document, previousState, DocumentState.DRAFTING);

        try {
            Instant phaseStart = Instant.now();
            document = writerAgent.process(document);
            monitoringService.recordAgentProcessing(AgentRole.WRITER, Duration.between(phaseStart, Instant.now()));

            if (!writerAgent.validate(document)) {
                throw new PipelineException("Draft validation failed",
                        DocumentState.DRAFTING);
            }

            String summary = String.format("%d words", document.getDraft().estimateWordCount());
            log.info("Draft complete: {}", summary);
            monitoringService.phaseCompleted(document, DocumentState.DRAFTING, summary);

            // Check for approval after draft
            checkApproval(document);

            return document;

        } catch (AgentException e) {
            throw new PipelineException("Drafting phase failed: " + e.getMessage(),
                    DocumentState.DRAFTING, e);
        }
    }

    /**
     * Execute the fact-checking phase with revision loop.
     * If maximum revisions are exceeded, the content is annotated with fact-check
     * failure markers instead of failing the pipeline.
     */
    private PublishingDocument executeFactCheckPhase(PublishingDocument document) {
        log.info("Phase 3: Fact Checking");
        DocumentState previousState = document.getState();
        document.transitionTo(DocumentState.FACT_CHECKING);
        monitoringService.phaseStarted(document, previousState, DocumentState.FACT_CHECKING);

        int revisionCount = 0;
        int maxRevisions = pipelineProperties.getMaxRevisionCycles();

        while (revisionCount < maxRevisions) {
            try {
                Instant phaseStart = Instant.now();
                document = factCheckerAgent.process(document);
                monitoringService.recordAgentProcessing(AgentRole.FACT_CHECKER, Duration.between(phaseStart, Instant.now()));

                if (!factCheckerAgent.validate(document)) {
                    throw new PipelineException("Fact check validation failed",
                            DocumentState.FACT_CHECKING);
                }

                FactCheckReport report = document.getFactCheckReport();
                String summary = String.format("%d verified, %d questionable, recommendation: %s",
                        report.verifiedClaims().size(),
                        report.questionableClaims().size(),
                        report.recommendedAction());
                log.info("Fact check complete: {}", summary);

                // Check if approved
                if (report.isPassed()) {
                    monitoringService.phaseCompleted(document, DocumentState.FACT_CHECKING, summary);
                    // Check for approval after fact check
                    checkApproval(document);
                    return document;
                }

                // Check if rejected (needs major rework)
                if (report.isRejected()) {
                    throw new PipelineException(
                            "Article rejected by fact checker: " + formatIssues(report),
                            DocumentState.FACT_CHECKING);
                }

                // Needs revision - go back to drafting
                revisionCount++;
                if (revisionCount < maxRevisions) {
                    log.info("Fact check requires revision ({}/{}), returning to draft",
                            revisionCount, maxRevisions);
                    monitoringService.revisionStarted(document, revisionCount, maxRevisions);
                    document.transitionTo(DocumentState.DRAFTING);
                    Instant revisionStart = Instant.now();
                    document = writerAgent.process(document);
                    monitoringService.recordAgentProcessing(AgentRole.WRITER, Duration.between(revisionStart, Instant.now()));
                    document.transitionTo(DocumentState.FACT_CHECKING);
                }

            } catch (AgentException e) {
                throw new PipelineException("Fact check phase failed: " + e.getMessage(),
                        DocumentState.FACT_CHECKING, e);
            }
        }

        // Max revisions exceeded - log issues and continue without modifying content
        log.warn("Maximum revision cycles ({}) exceeded during fact checking. " +
                "Logging issues to session log.", maxRevisions);

        FactCheckReport report = document.getFactCheckReport();
        logFactCheckFailures(document.getPageName(), report);

        String summary = String.format("Max revisions exceeded. %d questionable claims logged.",
                report.questionableClaims().size());
        monitoringService.phaseCompleted(document, DocumentState.FACT_CHECKING, summary);

        // Continue to editing phase - don't require approval for fact-check issues
        return document;
    }

    /**
     * Log fact-check failure information to session log.
     * Issues are logged for review but not embedded in the published content.
     *
     * @param pageName The page name for context in logs
     * @param report The fact-check report with issues
     */
    void logFactCheckFailures(String pageName, FactCheckReport report) {
        if (report == null || report.questionableClaims().isEmpty()) {
            return;
        }

        log.warn("=== FACT CHECK ISSUES FOR: {} ===", pageName);
        log.warn("The following claims could not be verified after {} revision attempts:",
                pipelineProperties.getMaxRevisionCycles());

        int claimNum = 1;
        for (QuestionableClaim claim : report.questionableClaims()) {
            log.warn("  {}. Questionable Claim:", claimNum++);
            log.warn("     Claim: {}", claim.claim());
            log.warn("     Issue: {}", claim.issue());
            if (claim.suggestion() != null && !claim.suggestion().isBlank()) {
                log.warn("     Suggestion: {}", claim.suggestion());
            }
        }

        if (!report.consistencyIssues().isEmpty()) {
            log.warn("  Consistency Issues:");
            for (String issue : report.consistencyIssues()) {
                log.warn("     - {}", issue);
            }
        }

        log.warn("=== END FACT CHECK ISSUES ===");
    }

    /**
     * Execute the editing phase.
     */
    private PublishingDocument executeEditingPhase(PublishingDocument document) {
        log.info("Phase 4: Editing");
        DocumentState previousState = document.getState();
        document.transitionTo(DocumentState.EDITING);
        monitoringService.phaseStarted(document, previousState, DocumentState.EDITING);

        // Provide existing pages to editor for link integration
        List<String> existingPages = outputService.getExistingPagesList();
        editorAgent.setExistingPages(existingPages);
        log.debug("Editor provided with {} existing pages for linking", existingPages.size());

        try {
            Instant phaseStart = Instant.now();
            document = editorAgent.process(document);
            monitoringService.recordAgentProcessing(AgentRole.EDITOR, Duration.between(phaseStart, Instant.now()));

            if (!editorAgent.validate(document)) {
                throw new PipelineException("Editor validation failed - quality score below threshold",
                        DocumentState.EDITING);
            }

            FinalArticle article = document.getFinalArticle();
            String summary = String.format("quality score %.2f, %d links added",
                    article.qualityScore(), article.addedLinks().size());
            log.info("Editing complete: {}", summary);
            monitoringService.phaseCompleted(document, DocumentState.EDITING, summary);

            // Check quality threshold
            double minScore = qualityProperties.getMinEditorScore();
            if (!article.meetsQualityThreshold(minScore)) {
                throw new PipelineException(
                        String.format("Quality score %.2f below minimum %.2f",
                                article.qualityScore(), minScore),
                        DocumentState.EDITING);
            }

            // Check for approval before publish
            checkApproval(document);

            return document;

        } catch (AgentException e) {
            throw new PipelineException("Editing phase failed: " + e.getMessage(),
                    DocumentState.EDITING, e);
        }
    }

    /**
     * Check approval at current state and throw if rejected.
     */
    private void checkApproval(PublishingDocument document) {
        monitoringService.approvalRequested(document, document.getState());
        try {
            boolean approved = approvalService.checkAndApprove(document);
            monitoringService.approvalReceived(document, document.getState(), approved);
            if (!approved) {
                // Changes requested - this would typically trigger a retry
                // For now, we treat it as a pipeline exception
                throw new PipelineException(
                        "Changes requested during approval at " + document.getState(),
                        document.getState(), true);
            }
        } catch (ApprovalService.ApprovalRejectedException e) {
            monitoringService.approvalReceived(document, document.getState(), false);
            throw new PipelineException("Approval rejected: " + e.getMessage(),
                    e.getState(), false);
        }
    }

    /**
     * Execute the critique phase - final quality and syntax review.
     * If maximum revisions are exceeded, the content is annotated with critique
     * markers instead of failing the pipeline.
     */
    private PublishingDocument executeCritiquePhase(PublishingDocument document) {
        log.info("Phase 5: Critique");
        DocumentState previousState = document.getState();
        document.transitionTo(DocumentState.CRITIQUING);
        monitoringService.phaseStarted(document, previousState, DocumentState.CRITIQUING);

        int revisionCount = 0;
        int maxRevisions = pipelineProperties.getMaxRevisionCycles();

        while (revisionCount < maxRevisions) {
            try {
                Instant phaseStart = Instant.now();
                document = criticAgent.process(document);
                monitoringService.recordAgentProcessing(AgentRole.CRITIC, Duration.between(phaseStart, Instant.now()));

                if (!criticAgent.validate(document)) {
                    throw new PipelineException("Critic validation failed",
                            DocumentState.CRITIQUING);
                }

                CriticReport report = document.getCriticReport();
                String summary = String.format("overall=%.2f, syntax=%.2f, recommendation=%s",
                        report.overallScore(), report.syntaxScore(), report.recommendedAction());
                log.info("Critique complete: {}", summary);

                // Check if approved
                if (report.isApproved()) {
                    monitoringService.phaseCompleted(document, DocumentState.CRITIQUING, summary);
                    // Check for approval after critique
                    checkApproval(document);
                    return document;
                }

                // Check if needs major rework
                if (report.needsRework()) {
                    throw new PipelineException(
                            "Article rejected by critic: " + report.getIssueSummary(),
                            DocumentState.CRITIQUING);
                }

                // Needs revision - go back to editing
                revisionCount++;
                if (revisionCount < maxRevisions) {
                    log.info("Critique requires revision ({}/{}), returning to editor",
                            revisionCount, maxRevisions);
                    monitoringService.revisionStarted(document, revisionCount, maxRevisions);

                    // Go back to editing to fix issues
                    document.transitionTo(DocumentState.EDITING);

                    // Provide existing pages to editor again
                    List<String> existingPages = outputService.getExistingPagesList();
                    editorAgent.setExistingPages(existingPages);

                    Instant revisionStart = Instant.now();
                    document = editorAgent.process(document);
                    monitoringService.recordAgentProcessing(AgentRole.EDITOR, Duration.between(revisionStart, Instant.now()));
                    document.transitionTo(DocumentState.CRITIQUING);
                }

            } catch (AgentException e) {
                throw new PipelineException("Critique phase failed: " + e.getMessage(),
                        DocumentState.CRITIQUING, e);
            }
        }

        // Max revisions exceeded - log issues and continue without modifying content
        log.warn("Maximum revision cycles ({}) exceeded during critique. " +
                "Logging issues to session log.", maxRevisions);

        CriticReport report = document.getCriticReport();
        logCritiqueFailures(document.getPageName(), report);

        String summary = String.format("Max revisions exceeded. %s logged.",
                report.getIssueSummary());
        monitoringService.phaseCompleted(document, DocumentState.CRITIQUING, summary);

        // Continue to publishing phase
        return document;
    }

    /**
     * Log critique failure information to session log.
     * Issues are logged for review but not embedded in the published content.
     *
     * @param pageName The page name for context in logs
     * @param report The critic report with issues
     */
    void logCritiqueFailures(String pageName, CriticReport report) {
        if (report == null) {
            return;
        }

        boolean hasIssues = !report.structureIssues().isEmpty() ||
                !report.syntaxIssues().isEmpty() ||
                !report.styleIssues().isEmpty();

        if (!hasIssues) {
            return;
        }

        log.warn("=== CRITIQUE ISSUES FOR: {} ===", pageName);
        log.warn("The following issues were noted after {} revision attempts:",
                pipelineProperties.getMaxRevisionCycles());

        if (!report.syntaxIssues().isEmpty()) {
            log.warn("  Syntax Issues:");
            for (String issue : report.syntaxIssues()) {
                log.warn("     - {}", issue);
            }
        }

        if (!report.structureIssues().isEmpty()) {
            log.warn("  Structure Issues:");
            for (String issue : report.structureIssues()) {
                log.warn("     - {}", issue);
            }
        }

        if (!report.styleIssues().isEmpty()) {
            log.warn("  Style Issues:");
            for (String issue : report.styleIssues()) {
                log.warn("     - {}", issue);
            }
        }

        if (!report.suggestions().isEmpty()) {
            log.warn("  Suggestions:");
            for (String suggestion : report.suggestions()) {
                log.warn("     - {}", suggestion);
            }
        }

        log.warn("=== END CRITIQUE ISSUES ===");
    }

    /**
     * Execute the publish phase - write to output.
     */
    private Path executePublishPhase(PublishingDocument document) {
        log.info("Phase 6: Publishing");

        try {
            Path outputPath = outputService.writeDocument(document);
            document.transitionTo(DocumentState.PUBLISHED);

            log.info("Published to: {}", outputPath);
            return outputPath;

        } catch (IOException e) {
            throw new PipelineException("Publishing failed: " + e.getMessage(),
                    DocumentState.EDITING, e);
        }
    }

    /**
     * Save a failed document for debugging purposes.
     * The document is saved with a suffix indicating the failure state.
     *
     * @param document The failed document
     * @param failedState The state at which it failed
     * @param errorMessage The error message
     * @return Path to the saved file, or null if saving failed
     */
    private Path saveFailedDocument(PublishingDocument document, DocumentState failedState, String errorMessage) {
        // Only save if we have some content (at least a draft)
        if (document.getDraft() == null && document.getResearchBrief() == null) {
            log.debug("Not saving failed document - no content to save");
            return null;
        }

        try {
            Path savedPath = outputService.writeFailedDocument(document, failedState, errorMessage);
            if (savedPath != null) {
                log.info("Saved failed document for debugging: {}", savedPath);
            }
            return savedPath;
        } catch (Exception e) {
            log.warn("Could not save failed document for debugging: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Format fact-check issues for error messages.
     */
    private String formatIssues(FactCheckReport report) {
        StringBuilder sb = new StringBuilder();

        if (!report.questionableClaims().isEmpty()) {
            sb.append(report.questionableClaims().size()).append(" questionable claims");
        }

        if (!report.consistencyIssues().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(report.consistencyIssues().size()).append(" consistency issues");
        }

        return sb.toString();
    }

    // Getters for testing

    public ResearchAgent getResearchAgent() {
        return researchAgent;
    }

    public WriterAgent getWriterAgent() {
        return writerAgent;
    }

    public FactCheckerAgent getFactCheckerAgent() {
        return factCheckerAgent;
    }

    public EditorAgent getEditorAgent() {
        return editorAgent;
    }

    public CriticAgent getCriticAgent() {
        return criticAgent;
    }

    public WikiOutputService getOutputService() {
        return outputService;
    }

    public GlossaryService getGlossaryService() {
        return glossaryService;
    }
}
