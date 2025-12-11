package com.jakefear.aipublisher.pipeline;

import com.jakefear.aipublisher.agent.*;
import com.jakefear.aipublisher.approval.ApprovalService;
import com.jakefear.aipublisher.config.PipelineProperties;
import com.jakefear.aipublisher.config.QualityProperties;
import com.jakefear.aipublisher.document.*;
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
 * 5. Publish - Write to output
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
    private final WikiOutputService outputService;
    private final ApprovalService approvalService;
    private final PipelineMonitoringService monitoringService;
    private final PipelineProperties pipelineProperties;
    private final QualityProperties qualityProperties;

    public PublishingPipeline(
            ResearchAgent researchAgent,
            WriterAgent writerAgent,
            FactCheckerAgent factCheckerAgent,
            EditorAgent editorAgent,
            WikiOutputService outputService,
            ApprovalService approvalService,
            PipelineMonitoringService monitoringService,
            PipelineProperties pipelineProperties,
            QualityProperties qualityProperties) {
        this.researchAgent = researchAgent;
        this.writerAgent = writerAgent;
        this.factCheckerAgent = factCheckerAgent;
        this.editorAgent = editorAgent;
        this.outputService = outputService;
        this.approvalService = approvalService;
        this.monitoringService = monitoringService;
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

            // Phase 5: Publishing
            Path outputPath = executePublishPhase(document);

            Duration totalTime = Duration.between(startTime, Instant.now());
            log.info("Pipeline completed successfully in {} ms", totalTime.toMillis());
            monitoringService.pipelineCompleted(document, totalTime);

            return PipelineResult.success(document, outputPath, totalTime);

        } catch (PipelineException e) {
            Duration totalTime = Duration.between(startTime, Instant.now());
            log.error("Pipeline failed at {}: {}", e.getFailedAtState(), e.getMessage());
            monitoringService.pipelineFailed(document, e.getFailedAtState(), e.getMessage());
            return PipelineResult.failure(document, e.getMessage(), e.getFailedAtState(), totalTime);

        } catch (Exception e) {
            Duration totalTime = Duration.between(startTime, Instant.now());
            log.error("Pipeline failed with unexpected error: {}", e.getMessage(), e);
            monitoringService.pipelineFailed(document, document.getState(), e.getMessage());
            return PipelineResult.failure(document, e.getMessage(), document.getState(), totalTime);
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

            String summary = String.format("%d key facts gathered", document.getResearchBrief().keyFacts().size());
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

        // Max revisions exceeded
        throw new PipelineException(
                "Maximum revision cycles (" + maxRevisions + ") exceeded during fact checking",
                DocumentState.FACT_CHECKING);
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
     * Execute the publish phase - write to output.
     */
    private Path executePublishPhase(PublishingDocument document) {
        log.info("Phase 5: Publishing");

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

    public WikiOutputService getOutputService() {
        return outputService;
    }
}
