package com.jakefear.aipublisher.cli;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.content.ContentTypeSelector;
import com.jakefear.aipublisher.content.ContentTypeTemplate;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive CLI session that guides users through content configuration.
 */
public class InteractiveSession {

    private final BufferedReader in;
    private final PrintWriter out;
    private final ContentTypeSelector contentTypeSelector;

    // Session state
    private String topic;
    private ContentType contentType;
    private String audience;
    private int wordCount;
    private List<String> requiredSections = new ArrayList<>();
    private List<String> relatedPages = new ArrayList<>();
    private String domainContext;
    private String specificGoal;

    public InteractiveSession(BufferedReader in, PrintWriter out, ContentTypeSelector contentTypeSelector) {
        this.in = in;
        this.out = out;
        this.contentTypeSelector = contentTypeSelector;
    }

    /**
     * Run the complete interactive session.
     *
     * @return true if session completed successfully, false if cancelled
     */
    public boolean run() {
        try {
            printWelcome();

            // Step 1: Topic
            if (!promptForTopic()) {
                return false;
            }

            // Step 2: Content Type
            if (!promptForContentType()) {
                return false;
            }

            // Step 3: Audience
            if (!promptForAudience()) {
                return false;
            }

            // Step 4: Content-type-specific questions
            if (!promptContentTypeSpecificQuestions()) {
                return false;
            }

            // Step 5: Word count
            if (!promptForWordCount()) {
                return false;
            }

            // Step 6: Related pages
            if (!promptForRelatedPages()) {
                return false;
            }

            // Step 7: Confirmation
            return confirmConfiguration();

        } catch (Exception e) {
            out.println("\nSession interrupted: " + e.getMessage());
            return false;
        }
    }

    private void printWelcome() {
        out.println();
        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║                    AI PUBLISHER - INTERACTIVE MODE                ║");
        out.println("║                                                                   ║");
        out.println("║   Answer the following questions to configure your article.       ║");
        out.println("║   Press Enter to accept defaults shown in [brackets].             ║");
        out.println("║   Type 'quit' at any prompt to cancel.                            ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
    }

    private boolean promptForTopic() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 1 OF 6: TOPIC                                             │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();
        out.println("What topic would you like to write about?");
        out.println();
        out.println("Examples:");
        out.println("  • Apache Kafka");
        out.println("  • How to set up Docker containers");
        out.println("  • Kafka vs RabbitMQ");
        out.println("  • Troubleshooting connection timeouts");
        out.println();
        out.print("Topic: ");
        out.flush();

        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) {
            out.println("\nSession cancelled.");
            return false;
        }

        topic = input.trim();
        if (topic.isEmpty()) {
            out.println("Topic is required.");
            return promptForTopic();
        }

        out.println();
        return true;
    }

    private boolean promptForContentType() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 2 OF 6: CONTENT TYPE                                      │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();

        // Auto-detect content type
        ContentTypeSelector.ContentTypeRecommendation recommendation =
                contentTypeSelector.recommend(topic);

        out.println("Based on your topic, I recommend: " + recommendation.contentType().getDisplayName());
        out.println("Reason: " + recommendation.rationale());
        out.println();
        out.println("Available content types:");
        out.println();

        int i = 1;
        for (ContentType type : ContentType.values()) {
            String marker = (type == recommendation.contentType()) ? " ← recommended" : "";
            out.printf("  %d. %-15s - %s%s%n",
                    i++, type.getDisplayName(), type.getDescription(), marker);
        }

        out.println();
        out.print("Select type [" + recommendation.contentType().getDisplayName() + "]: ");
        out.flush();

        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) {
            return false;
        }

        if (input.isBlank()) {
            contentType = recommendation.contentType();
        } else {
            // Try to parse as number first
            try {
                int selection = Integer.parseInt(input.trim());
                if (selection >= 1 && selection <= ContentType.values().length) {
                    contentType = ContentType.values()[selection - 1];
                } else {
                    out.println("Invalid selection. Using recommended type.");
                    contentType = recommendation.contentType();
                }
            } catch (NumberFormatException e) {
                // Try to parse as type name
                ContentType parsed = ContentType.fromString(input);
                if (parsed != null) {
                    contentType = parsed;
                } else {
                    out.println("Unrecognized type. Using recommended type.");
                    contentType = recommendation.contentType();
                }
            }
        }

        out.println("\nSelected: " + contentType.getDisplayName());
        out.println();
        return true;
    }

    private boolean promptForAudience() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 3 OF 6: TARGET AUDIENCE                                   │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();
        out.println("Who is this article for? Be specific about their background.");
        out.println();
        out.println("Examples:");
        out.println("  • developers new to event streaming");
        out.println("  • experienced Java developers learning Kotlin");
        out.println("  • DevOps engineers familiar with Docker");
        out.println("  • technical managers evaluating solutions");
        out.println();
        out.print("Target audience [general readers]: ");
        out.flush();

        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) {
            return false;
        }

        audience = input.isBlank() ? "general readers" : input.trim();
        out.println("\nAudience: " + audience);
        out.println();
        return true;
    }

    private boolean promptContentTypeSpecificQuestions() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 4 OF 6: CONTENT DETAILS                                   │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();

        switch (contentType) {
            case TUTORIAL -> {
                return promptTutorialQuestions();
            }
            case COMPARISON -> {
                return promptComparisonQuestions();
            }
            case TROUBLESHOOTING -> {
                return promptTroubleshootingQuestions();
            }
            case GUIDE -> {
                return promptGuideQuestions();
            }
            case CONCEPT, OVERVIEW, REFERENCE -> {
                return promptConceptQuestions();
            }
        }
        return true;
    }

    private boolean promptTutorialQuestions() throws Exception {
        out.println("For this tutorial, let's define the scope:");
        out.println();

        out.print("What will readers accomplish? (specific goal): ");
        out.flush();
        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        specificGoal = input.isBlank() ? null : input.trim();

        out.println();
        out.print("What tools/technologies should readers have? (prerequisites): ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        if (!input.isBlank()) {
            requiredSections.add("Prerequisites: " + input.trim());
        }

        out.println();
        out.print("Any specific domain context (e.g., 'e-commerce', 'microservices')? [none]: ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        domainContext = input.isBlank() ? null : input.trim();

        out.println();
        return true;
    }

    private boolean promptComparisonQuestions() throws Exception {
        out.println("For this comparison:");
        out.println();

        out.print("What criteria matter most to your audience? (e.g., performance, cost, ease of use): ");
        out.flush();
        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        if (!input.isBlank()) {
            requiredSections.add("Comparison criteria: " + input.trim());
        }

        out.println();
        out.print("Any specific use case to focus on? [general comparison]: ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        domainContext = input.isBlank() ? null : input.trim();

        out.println();
        return true;
    }

    private boolean promptTroubleshootingQuestions() throws Exception {
        out.println("For this troubleshooting guide:");
        out.println();

        out.print("What are the main symptoms readers will see? ");
        out.flush();
        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        if (!input.isBlank()) {
            requiredSections.add("Symptoms: " + input.trim());
        }

        out.println();
        out.print("What environment or context? (e.g., 'production Kubernetes', 'local Docker'): ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        domainContext = input.isBlank() ? null : input.trim();

        out.println();
        return true;
    }

    private boolean promptGuideQuestions() throws Exception {
        out.println("For this decision guide:");
        out.println();

        out.print("What decision or choice is the reader facing? ");
        out.flush();
        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        specificGoal = input.isBlank() ? null : input.trim();

        out.println();
        out.print("What constraints or considerations matter? (e.g., 'limited budget', 'high scale'): ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        if (!input.isBlank()) {
            requiredSections.add("Constraints: " + input.trim());
        }

        out.println();
        return true;
    }

    private boolean promptConceptQuestions() throws Exception {
        out.println("For this " + contentType.getDisplayName().toLowerCase() + ":");
        out.println();

        out.print("What's the broader context or system this fits into? [none]: ");
        out.flush();
        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        domainContext = input.isBlank() ? null : input.trim();

        out.println();
        out.print("Any specific aspects to emphasize? [comprehensive coverage]: ");
        out.flush();
        input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) return false;
        if (!input.isBlank()) {
            requiredSections.add("Focus: " + input.trim());
        }

        out.println();
        return true;
    }

    private boolean promptForWordCount() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 5 OF 6: LENGTH                                            │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();

        int defaultWordCount = contentType.getDefaultWordCount();
        int min = contentType.getMinWordCount();
        int max = contentType.getMaxWordCount();

        out.printf("Recommended length for %s: %d-%d words%n",
                contentType.getDisplayName(), min, max);
        out.println();
        out.printf("Target word count [%d]: ", defaultWordCount);
        out.flush();

        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) {
            return false;
        }

        if (input.isBlank()) {
            wordCount = defaultWordCount;
        } else {
            try {
                int parsed = Integer.parseInt(input.trim());
                if (parsed < 100) {
                    out.println("Minimum 100 words. Using minimum.");
                    wordCount = 100;
                } else if (parsed > 5000) {
                    out.println("Maximum 5000 words. Using maximum.");
                    wordCount = 5000;
                } else {
                    wordCount = parsed;
                }
            } catch (NumberFormatException e) {
                out.println("Invalid number. Using default: " + defaultWordCount);
                wordCount = defaultWordCount;
            }
        }

        out.println("\nWord count: " + wordCount);
        out.println();
        return true;
    }

    private boolean promptForRelatedPages() throws Exception {
        out.println("┌─────────────────────────────────────────────────────────────────┐");
        out.println("│  STEP 6 OF 6: RELATED PAGES                                     │");
        out.println("└─────────────────────────────────────────────────────────────────┘");
        out.println();
        out.println("Are there existing wiki pages that should be linked?");
        out.println("Enter page names in CamelCase, comma-separated.");
        out.println();
        out.println("Examples: ApacheKafka, EventDrivenArchitecture, Microservices");
        out.println();
        out.print("Related pages [none]: ");
        out.flush();

        String input = in.readLine();
        if (input == null || input.equalsIgnoreCase("quit")) {
            return false;
        }

        if (!input.isBlank()) {
            String[] pages = input.split(",");
            for (String page : pages) {
                String trimmed = page.trim();
                if (!trimmed.isEmpty()) {
                    relatedPages.add(trimmed);
                }
            }
        }

        if (!relatedPages.isEmpty()) {
            out.println("\nRelated pages: " + String.join(", ", relatedPages));
        } else {
            out.println("\nNo related pages specified.");
        }
        out.println();
        return true;
    }

    private boolean confirmConfiguration() throws Exception {
        out.println("╔═══════════════════════════════════════════════════════════════════╗");
        out.println("║                      CONFIGURATION SUMMARY                        ║");
        out.println("╚═══════════════════════════════════════════════════════════════════╝");
        out.println();
        out.printf("  Topic:        %s%n", topic);
        out.printf("  Content Type: %s%n", contentType.getDisplayName());
        out.printf("  Audience:     %s%n", audience);
        out.printf("  Word Count:   %d words%n", wordCount);

        if (domainContext != null) {
            out.printf("  Context:      %s%n", domainContext);
        }
        if (specificGoal != null) {
            out.printf("  Goal:         %s%n", specificGoal);
        }
        if (!requiredSections.isEmpty()) {
            out.println("  Sections:     " + String.join("; ", requiredSections));
        }
        if (!relatedPages.isEmpty()) {
            out.println("  Related:      " + String.join(", ", relatedPages));
        }

        out.println();
        out.println("─".repeat(67));
        out.println();
        out.print("Proceed with generation? [Y/n]: ");
        out.flush();

        String input = in.readLine();
        if (input == null) {
            return false;
        }

        input = input.trim().toLowerCase();
        if (input.isEmpty() || input.equals("y") || input.equals("yes")) {
            out.println("\nStarting generation...\n");
            return true;
        } else {
            out.println("\nGeneration cancelled.");
            return false;
        }
    }

    // Getters for the collected configuration

    public String getTopic() {
        return topic;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getAudience() {
        return audience;
    }

    public int getWordCount() {
        return wordCount;
    }

    public List<String> getRequiredSections() {
        return requiredSections;
    }

    public List<String> getRelatedPages() {
        return relatedPages;
    }

    public String getDomainContext() {
        return domainContext;
    }

    public String getSpecificGoal() {
        return specificGoal;
    }

    /**
     * Get the enhanced topic with context included.
     */
    public String getEnhancedTopic() {
        StringBuilder enhanced = new StringBuilder(topic);

        if (specificGoal != null && !specificGoal.isEmpty()) {
            enhanced.append(" (Goal: ").append(specificGoal).append(")");
        }

        if (domainContext != null && !domainContext.isEmpty()) {
            enhanced.append(" [Context: ").append(domainContext).append("]");
        }

        return enhanced.toString();
    }
}
