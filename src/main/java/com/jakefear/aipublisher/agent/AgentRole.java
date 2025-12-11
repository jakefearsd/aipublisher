package com.jakefear.aipublisher.agent;

/**
 * Roles that agents can play in the publishing pipeline.
 */
public enum AgentRole {
    /**
     * Gathers and synthesizes source material for article creation.
     */
    RESEARCHER("Research Agent"),

    /**
     * Transforms research into coherent, well-structured prose.
     */
    WRITER("Writer Agent"),

    /**
     * Verifies claims, checks consistency, and flags potential issues.
     */
    FACT_CHECKER("Fact Checker Agent"),

    /**
     * Polishes prose, ensures style consistency, and prepares final output.
     */
    EDITOR("Editor Agent");

    private final String displayName;

    AgentRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
