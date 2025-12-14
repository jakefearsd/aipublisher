package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import com.jakefear.aipublisher.domain.ScopeConfiguration;

import java.io.PrintWriter;
import java.util.Arrays;

/**
 * Handler for Phase 2: Scope Setup.
 * Configures scope boundaries and assumptions (optional phase).
 */
public class ScopeSetupPhaseHandler extends AbstractPhaseHandler {

    @Override
    public DiscoveryPhase getPhase() {
        return DiscoveryPhase.SCOPE_SETUP;
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    public PhaseResult execute(PhaseContext ctx) throws Exception {
        printHeader(ctx);

        PrintWriter out = ctx.out();

        out.println("Would you like to configure scope boundaries?");
        out.println("This helps the AI generate more relevant suggestions.");
        out.println();

        InputResponse configureResponse = ctx.input().promptWithNavigationAndDefault(
                "Configure scope? [Y/n/skip]", "y");
        if (configureResponse.isQuit()) {
            return PhaseResult.cancel();
        }
        if (configureResponse.isSkip() || "n".equalsIgnoreCase(configureResponse.value())) {
            printSkipped(out, "scope configuration");
            ctx.session().advancePhase();
            return PhaseResult.continueToNext();
        }
        if (configureResponse.isBack()) {
            return PhaseResult.goTo(DiscoveryPhase.SEED_INPUT);
        }

        ScopeConfiguration.Builder scopeBuilder = ScopeConfiguration.builder();

        // Assumed knowledge
        out.println();
        out.println("What knowledge should readers already have? (comma-separated)");
        out.println("These topics won't be covered in detail.");
        out.println();
        out.println("Examples: Java programming, basic SQL, command line familiarity");

        InputResponse assumedResponse = ctx.input().promptOptional("Assumed knowledge", null);
        if (assumedResponse.hasValue()) {
            Arrays.stream(assumedResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addAssumedKnowledge);
        }

        // Out of scope
        out.println();
        out.println("What topics should be explicitly excluded? (comma-separated)");

        InputResponse excludeResponse = ctx.input().promptOptional("Out of scope", null);
        if (excludeResponse.hasValue()) {
            Arrays.stream(excludeResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addOutOfScope);
        }

        // Focus areas
        out.println();
        out.println("Any specific areas to prioritize? (comma-separated)");

        InputResponse focusResponse = ctx.input().promptOptional("Focus areas", null);
        if (focusResponse.hasValue()) {
            Arrays.stream(focusResponse.value().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(scopeBuilder::addFocusArea);
        }

        // Target audience
        out.println();
        InputResponse audienceResponse = ctx.input().promptOptional("Target audience description", null);
        if (audienceResponse.hasValue()) {
            scopeBuilder.audienceDescription(audienceResponse.value());
        }

        ctx.session().configureScope(scopeBuilder.build());
        printSuccess(out, "Scope configured.");
        ctx.session().advancePhase();
        return PhaseResult.continueToNext();
    }
}
