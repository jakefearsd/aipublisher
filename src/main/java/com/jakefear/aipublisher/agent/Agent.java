package com.jakefear.aipublisher.agent;

import com.jakefear.aipublisher.document.PublishingDocument;

/**
 * Interface for all publishing pipeline agents.
 * Each agent processes the document and adds its contribution.
 */
public interface Agent {

    /**
     * Process the document and update it with this agent's contribution.
     *
     * @param document the document to process
     * @return the updated document
     * @throws AgentException if processing fails
     */
    PublishingDocument process(PublishingDocument document) throws AgentException;

    /**
     * Validate that the document has the expected content after processing.
     *
     * @param document the document to validate
     * @return true if valid, false otherwise
     */
    boolean validate(PublishingDocument document);

    /**
     * Get the role of this agent.
     *
     * @return the agent's role
     */
    AgentRole getRole();

    /**
     * Get a human-readable name for this agent.
     *
     * @return the agent's display name
     */
    default String getName() {
        return getRole().getDisplayName();
    }
}
