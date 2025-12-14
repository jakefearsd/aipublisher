package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PhaseHandlerRegistry.
 */
@DisplayName("PhaseHandlerRegistry")
class PhaseHandlerRegistryTest {

    private PhaseHandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PhaseHandlerRegistry();
    }

    @Nested
    @DisplayName("Default Handlers")
    class DefaultHandlers {

        @Test
        @DisplayName("Has handler for SEED_INPUT")
        void hasHandlerForSeedInput() {
            assertTrue(registry.hasHandler(DiscoveryPhase.SEED_INPUT));
            Optional<PhaseHandler> handler = registry.getHandler(DiscoveryPhase.SEED_INPUT);
            assertTrue(handler.isPresent());
            assertInstanceOf(SeedInputPhaseHandler.class, handler.get());
        }

        @Test
        @DisplayName("Has handler for SCOPE_SETUP")
        void hasHandlerForScopeSetup() {
            assertTrue(registry.hasHandler(DiscoveryPhase.SCOPE_SETUP));
            Optional<PhaseHandler> handler = registry.getHandler(DiscoveryPhase.SCOPE_SETUP);
            assertTrue(handler.isPresent());
            assertInstanceOf(ScopeSetupPhaseHandler.class, handler.get());
        }

        @Test
        @DisplayName("Has handler for REVIEW")
        void hasHandlerForReview() {
            assertTrue(registry.hasHandler(DiscoveryPhase.REVIEW));
            Optional<PhaseHandler> handler = registry.getHandler(DiscoveryPhase.REVIEW);
            assertTrue(handler.isPresent());
            assertInstanceOf(ReviewPhaseHandler.class, handler.get());
        }

        @Test
        @DisplayName("Returns empty for unregistered phases")
        void returnsEmptyForUnregistered() {
            assertFalse(registry.hasHandler(DiscoveryPhase.TOPIC_EXPANSION));
            Optional<PhaseHandler> handler = registry.getHandler(DiscoveryPhase.TOPIC_EXPANSION);
            assertFalse(handler.isPresent());
        }
    }

    @Nested
    @DisplayName("Registration")
    class Registration {

        @Test
        @DisplayName("Can register custom handler")
        void canRegisterCustomHandler() {
            PhaseHandler customHandler = new PhaseHandler() {
                @Override
                public DiscoveryPhase getPhase() {
                    return DiscoveryPhase.TOPIC_EXPANSION;
                }

                @Override
                public PhaseResult execute(PhaseContext context) {
                    return PhaseResult.continueToNext();
                }
            };

            registry.register(customHandler);

            assertTrue(registry.hasHandler(DiscoveryPhase.TOPIC_EXPANSION));
            assertEquals(customHandler, registry.getHandler(DiscoveryPhase.TOPIC_EXPANSION).get());
        }

        @Test
        @DisplayName("getAllHandlers returns all registered handlers")
        void getAllHandlersReturnsAll() {
            var handlers = registry.getAllHandlers();

            assertTrue(handlers.containsKey(DiscoveryPhase.SEED_INPUT));
            assertTrue(handlers.containsKey(DiscoveryPhase.SCOPE_SETUP));
            assertTrue(handlers.containsKey(DiscoveryPhase.REVIEW));
        }
    }

    @Nested
    @DisplayName("Handler Properties")
    class HandlerProperties {

        @Test
        @DisplayName("SeedInputPhaseHandler returns correct phase")
        void seedInputReturnsCorrectPhase() {
            PhaseHandler handler = registry.getHandler(DiscoveryPhase.SEED_INPUT).get();
            assertEquals(DiscoveryPhase.SEED_INPUT, handler.getPhase());
            assertEquals(1, handler.getPhaseNumber());
            assertFalse(handler.isOptional());
        }

        @Test
        @DisplayName("ScopeSetupPhaseHandler is optional")
        void scopeSetupIsOptional() {
            PhaseHandler handler = registry.getHandler(DiscoveryPhase.SCOPE_SETUP).get();
            assertEquals(DiscoveryPhase.SCOPE_SETUP, handler.getPhase());
            assertTrue(handler.isOptional());
        }

        @Test
        @DisplayName("ReviewPhaseHandler returns correct phase")
        void reviewReturnsCorrectPhase() {
            PhaseHandler handler = registry.getHandler(DiscoveryPhase.REVIEW).get();
            assertEquals(DiscoveryPhase.REVIEW, handler.getPhase());
            assertFalse(handler.isOptional());
        }
    }
}
