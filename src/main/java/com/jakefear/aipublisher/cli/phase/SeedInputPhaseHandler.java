package com.jakefear.aipublisher.cli.phase;

import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoveryPhase;
import com.jakefear.aipublisher.discovery.DiscoverySession;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler for Phase 1: Seed Input.
 * Collects domain name, description, and initial seed topics.
 */
public class SeedInputPhaseHandler implements PhaseHandler {

    private static final int TOTAL_PHASES = 8;

    @Override
    public DiscoveryPhase getPhase() {
        return DiscoveryPhase.SEED_INPUT;
    }

    @Override
    public PhaseResult execute(PhaseContext ctx) throws Exception {
        ctx.printPhaseHeader("SEED INPUT", 1, TOTAL_PHASES, "Provide initial topics to explore");

        PrintWriter out = ctx.out();

        // Get domain name
        out.println("What domain or subject area is this wiki about?");
        out.println();
        out.println("Examples:");
        out.println("  • Apache Kafka");
        out.println("  • Cloud Native Development");
        out.println("  • Machine Learning Operations");
        out.println();

        InputResponse domainResponse = ctx.input().promptRequired("Domain name");
        if (domainResponse.isQuit()) {
            return PhaseResult.cancel();
        }
        String domainName = domainResponse.value();
        if (domainName == null || domainName.isBlank()) {
            out.println("Domain name is required.");
            return execute(ctx); // Retry
        }

        // Initialize session with domain name and set on context
        DiscoverySession session = new DiscoverySession(domainName);
        ctx.setSession(session);
        out.println();

        // Get domain description
        InputResponse descResponse = ctx.input().promptOptional(
                "Brief description of what this wiki will cover", null);
        if (descResponse.isQuit()) {
            return PhaseResult.cancel();
        }
        if (descResponse.hasValue() && !descResponse.value().isBlank()) {
            session.setDomainDescription(descResponse.value());
        }
        out.println();

        // Get seed topics
        out.println("Enter your initial seed topics (one per line, empty line to finish):");
        out.println("These are the core topics you definitely want to cover.");
        out.println();

        List<String[]> seeds = new ArrayList<>();
        int seedNum = 1;
        while (true) {
            InputResponse topicResponse = ctx.input().promptOptional(
                    "Seed topic " + seedNum + " (or press Enter to finish)", null);
            if (topicResponse.isQuit()) {
                return PhaseResult.cancel();
            }
            String topicName = topicResponse.value();
            if (topicName == null || topicName.isBlank()) {
                break;
            }

            InputResponse descriptionResponse = ctx.input().promptOptional("  Brief description", "");
            String topicDesc = descriptionResponse.getValueOrDefault("");

            seeds.add(new String[]{topicName, topicDesc});
            seedNum++;
        }

        if (seeds.isEmpty()) {
            out.println("\nAt least one seed topic is required.");
            return execute(ctx); // Retry
        }

        // Mark one as landing page
        out.println();
        out.println("Which topic should be the main landing page?");
        List<String> topicNames = seeds.stream().map(s -> s[0]).toList();
        InputResponse landingResponse = ctx.input().promptSelection("Selection", topicNames, 0);
        if (landingResponse.isQuit()) {
            return PhaseResult.cancel();
        }
        int landingIndex = Integer.parseInt(landingResponse.value());

        // Add topics to session
        for (int i = 0; i < seeds.size(); i++) {
            String[] seed = seeds.get(i);
            if (i == landingIndex) {
                session.addLandingPage(seed[0], seed[1]);
            } else {
                session.addSeedTopic(seed[0], seed[1]);
            }
        }

        out.println();
        out.printf("✓ Created domain '%s' with %d seed topics%n", domainName, seeds.size());
        session.advancePhase();

        return PhaseResult.continueToNext();
    }
}
