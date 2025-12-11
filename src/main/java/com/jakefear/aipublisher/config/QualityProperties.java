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
}
