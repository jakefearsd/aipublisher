package com.jakefear.aipublisher.agent;

/**
 * Exception thrown when an agent fails to process a document.
 */
public class AgentException extends RuntimeException {

    private final AgentRole agentRole;
    private final boolean retryable;

    public AgentException(AgentRole agentRole, String message) {
        super(message);
        this.agentRole = agentRole;
        this.retryable = true;
    }

    public AgentException(AgentRole agentRole, String message, Throwable cause) {
        super(message, cause);
        this.agentRole = agentRole;
        this.retryable = true;
    }

    public AgentException(AgentRole agentRole, String message, boolean retryable) {
        super(message);
        this.agentRole = agentRole;
        this.retryable = retryable;
    }

    public AgentException(AgentRole agentRole, String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.agentRole = agentRole;
        this.retryable = retryable;
    }

    public AgentRole getAgentRole() {
        return agentRole;
    }

    /**
     * Whether this error can potentially be resolved by retrying.
     */
    public boolean isRetryable() {
        return retryable;
    }

    @Override
    public String toString() {
        return String.format("AgentException[%s]: %s (retryable=%s)",
                agentRole, getMessage(), retryable);
    }
}
