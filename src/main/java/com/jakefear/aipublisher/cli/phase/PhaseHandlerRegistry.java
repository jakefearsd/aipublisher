package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.discovery.DiscoveryPhase;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for phase handlers.
 * Maps discovery phases to their handlers.
 */
public class PhaseHandlerRegistry {

    private final Map<DiscoveryPhase, PhaseHandler> handlers = new EnumMap<>(DiscoveryPhase.class);

    public PhaseHandlerRegistry() {
        // Register available handlers
        register(new SeedInputPhaseHandler());
        register(new ScopeSetupPhaseHandler());
        register(new ReviewPhaseHandler());
        // Other phases remain in DiscoveryInteractiveSession for now
    }

    /**
     * Register a handler for its phase.
     */
    public void register(PhaseHandler handler) {
        handlers.put(handler.getPhase(), handler);
    }

    /**
     * Get the handler for a phase.
     */
    public Optional<PhaseHandler> getHandler(DiscoveryPhase phase) {
        return Optional.ofNullable(handlers.get(phase));
    }

    /**
     * Check if a phase has a registered handler.
     */
    public boolean hasHandler(DiscoveryPhase phase) {
        return handlers.containsKey(phase);
    }

    /**
     * Get all registered handlers.
     */
    public Map<DiscoveryPhase, PhaseHandler> getAllHandlers() {
        return new EnumMap<>(handlers);
    }
}
