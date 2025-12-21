package com.jakefear.aipublisher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for quality thresholds.
 */
@Component
@ConfigurationProperties(prefix = "quality")
public class QualityProperties {

    /**
     * Minimum confidence level required from fact checker to proceed.
     * Values: LOW, MEDIUM, HIGH
     */
    private String minFactcheckConfidence = "MEDIUM";

    /**
     * Minimum quality score (0.0-1.0) required from editor to publish.
     */
    private double minEditorScore = 0.8;

    /**
     * Whether to require at least one verified claim from fact checker.
     * If false, accepts APPROVE recommendation even with empty claim arrays.
     * Useful when models return "no issues" as empty arrays.
     */
    private boolean requireVerifiedClaims = true;

    public String getMinFactcheckConfidence() {
        return minFactcheckConfidence;
    }

    public void setMinFactcheckConfidence(String minFactcheckConfidence) {
        this.minFactcheckConfidence = minFactcheckConfidence;
    }

    public double getMinEditorScore() {
        return minEditorScore;
    }

    public void setMinEditorScore(double minEditorScore) {
        this.minEditorScore = minEditorScore;
    }

    public boolean isRequireVerifiedClaims() {
        return requireVerifiedClaims;
    }

    public void setRequireVerifiedClaims(boolean requireVerifiedClaims) {
        this.requireVerifiedClaims = requireVerifiedClaims;
    }
}
