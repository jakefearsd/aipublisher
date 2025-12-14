package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import com.jakefear.aipublisher.domain.Topic;
import com.jakefear.aipublisher.domain.TopicUniverse;

import java.io.PrintWriter;
import java.util.List;

/**
 * Handler for Phase 8: Review.
 * Final review and confirmation of the topic universe.
 */
public class ReviewPhaseHandler extends AbstractPhaseHandler {

    @Override
    public DiscoveryPhase getPhase() {
        return DiscoveryPhase.REVIEW;
    }

    @Override
    public PhaseResult execute(PhaseContext ctx) throws Exception {
        printHeader(ctx);

        PrintWriter out = ctx.out();
        TopicUniverse universe = ctx.session().buildUniverse();

        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║                        DISCOVERY SUMMARY                          ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
        out.printf("  Domain:        %s%n", universe.name());
        out.printf("  Topics:        %d accepted%n", universe.getAcceptedCount());
        out.printf("  Relationships: %d mapped%n", universe.relationships().size());
        out.printf("  Backlog:       %d items%n", universe.backlog().size());
        out.println();

        // Show generation order preview
        List<Topic> order = universe.getGenerationOrder();
        out.println("  Suggested generation order:");
        int shown = Math.min(10, order.size());
        for (int i = 0; i < shown; i++) {
            Topic topic = order.get(i);
            out.printf("    %2d. %s [%s]%n", i + 1, topic.name(), topic.priority().getDisplayName());
        }
        if (order.size() > shown) {
            out.printf("    ... and %d more%n", order.size() - shown);
        }

        out.println();
        out.println("─".repeat(67));
        out.println();

        InputResponse finalizeResponse = ctx.input().promptYesNo("Finalize this topic universe?", true);
        if (finalizeResponse.isQuit()) {
            return PhaseResult.cancel();
        }

        if ("yes".equals(finalizeResponse.value())) {
            out.println("\n✓ Topic universe finalized!");
            out.println();
            out.printf("Session ID: %s%n", ctx.session().getSessionId());
            out.printf("You can now generate content for these %d topics.%n", universe.getAcceptedCount());
            return PhaseResult.continueToNext();
        } else {
            out.println("\n→ Going back to prioritization.");
            return PhaseResult.goTo(DiscoveryPhase.PRIORITIZATION);
        }
    }
}
