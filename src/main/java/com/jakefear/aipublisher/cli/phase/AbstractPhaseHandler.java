package com.jakefear.aipublisher.cli.phase;

import java.io.PrintWriter;

/**
 * Abstract base class for phase handlers.
 * Provides common utility methods.
 */
public abstract class AbstractPhaseHandler implements PhaseHandler {

    protected static final int TOTAL_PHASES = 8;

    /**
     * Print the phase header.
     */
    protected void printHeader(PhaseContext ctx) {
        ctx.printPhaseHeader(
                getPhase().getDisplayName().toUpperCase(),
                getPhaseNumber(),
                TOTAL_PHASES,
                getPhase().getDescription()
        );
    }

    /**
     * Print a success completion message.
     */
    protected void printSuccess(PrintWriter out, String message) {
        out.println("\n✓ " + message);
    }

    /**
     * Print a skip message.
     */
    protected void printSkipped(PrintWriter out, String phase) {
        out.println("\n→ Skipping " + phase + ".");
    }
}
