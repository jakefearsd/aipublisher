package com.jakefear.aipublisher.document;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Record of an agent's contribution to the document, for audit trail purposes.
 */
public record AgentContribution(
        /**
         * Which agent made this contribution.
         */
        String agentRole,

        /**
         * When the contribution was made.
         */
        Instant timestamp,

        /**
         * Hash of the input provided to the agent.
         */
        String inputHash,

        /**
         * Hash of the output produced by the agent.
         */
        String outputHash,

        /**
         * How long the agent took to process.
         */
        Duration processingTime,

        /**
         * Additional metrics (token counts, etc.).
         */
        Map<String, Object> metrics
) {
    public AgentContribution {
        Objects.requireNonNull(agentRole, "agentRole must not be null");
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        metrics = metrics == null ? Map.of() : Map.copyOf(metrics);
    }

    /**
     * Create a contribution record with just the essential fields.
     */
    public static AgentContribution create(String agentRole, Duration processingTime) {
        return new AgentContribution(
                agentRole,
                Instant.now(),
                null,
                null,
                processingTime,
                Map.of()
        );
    }

    /**
     * Create a contribution record with metrics.
     */
    public static AgentContribution withMetrics(
            String agentRole,
            Duration processingTime,
            Map<String, Object> metrics
    ) {
        return new AgentContribution(
                agentRole,
                Instant.now(),
                null,
                null,
                processingTime,
                metrics
        );
    }
}
