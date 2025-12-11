package com.jakefear.aipublisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for the publishing pipeline.
 * Controls revision cycles, timeouts, and approval checkpoints.
 */
@Component
@ConfigurationProperties(prefix = "pipeline")
public class PipelineProperties {

    /**
     * Maximum number of revision cycles before escalating to human intervention.
     */
    private int maxRevisionCycles = 3;

    /**
     * Timeout for each pipeline phase.
     */
    private Duration phaseTimeout = Duration.ofMinutes(5);

    /**
     * Approval checkpoint settings.
     */
    private ApprovalSettings approval = new ApprovalSettings();

    public int getMaxRevisionCycles() {
        return maxRevisionCycles;
    }

    public void setMaxRevisionCycles(int maxRevisionCycles) {
        this.maxRevisionCycles = maxRevisionCycles;
    }

    public Duration getPhaseTimeout() {
        return phaseTimeout;
    }

    public void setPhaseTimeout(Duration phaseTimeout) {
        this.phaseTimeout = phaseTimeout;
    }

    public ApprovalSettings getApproval() {
        return approval;
    }

    public void setApproval(ApprovalSettings approval) {
        this.approval = approval;
    }

    /**
     * Settings for human approval checkpoints at various pipeline stages.
     */
    public static class ApprovalSettings {
        private boolean afterResearch = false;
        private boolean afterDraft = false;
        private boolean afterFactcheck = false;
        private boolean beforePublish = true;

        public boolean isAfterResearch() {
            return afterResearch;
        }

        public void setAfterResearch(boolean afterResearch) {
            this.afterResearch = afterResearch;
        }

        public boolean isAfterDraft() {
            return afterDraft;
        }

        public void setAfterDraft(boolean afterDraft) {
            this.afterDraft = afterDraft;
        }

        public boolean isAfterFactcheck() {
            return afterFactcheck;
        }

        public void setAfterFactcheck(boolean afterFactcheck) {
            this.afterFactcheck = afterFactcheck;
        }

        public boolean isBeforePublish() {
            return beforePublish;
        }

        public void setBeforePublish(boolean beforePublish) {
            this.beforePublish = beforePublish;
        }
    }
}
