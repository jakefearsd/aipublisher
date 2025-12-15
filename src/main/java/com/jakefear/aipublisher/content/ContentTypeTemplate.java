package com.jakefear.aipublisher.content;

import java.util.List;
import java.util.Map;

/**
 * Template definitions for each content type, specifying required structure.
 *
 * <p><b>IMPORTANT:</b> When adding a new {@link ContentType}, you MUST also add
 * a corresponding template to {@link #TEMPLATES}. Failure to do so will cause
 * an {@link ExceptionInInitializerError} at application startup.</p>
 *
 * @see ContentType
 */
public record ContentTypeTemplate(
        ContentType contentType,
        List<SectionDefinition> requiredSections,
        List<SectionDefinition> optionalSections,
        String structureGuidance,
        String toneGuidance,
        int minExamples
) {

    /**
     * Definition of a section within the content structure.
     */
    public record SectionDefinition(
            String name,
            String description,
            boolean required,
            int order
    ) {}

    /**
     * Get the template for a content type.
     */
    public static ContentTypeTemplate forType(ContentType type) {
        return TEMPLATES.get(type);
    }

    /**
     * Generate JSPWiki-formatted structure guidance for the writer agent.
     */
    public String toWriterGuidance() {
        StringBuilder sb = new StringBuilder();
        sb.append("CONTENT TYPE: ").append(contentType.getDisplayName()).append("\n");
        sb.append("PURPOSE: ").append(contentType.getDescription()).append("\n\n");

        sb.append("REQUIRED STRUCTURE:\n");
        for (SectionDefinition section : requiredSections) {
            sb.append("  ").append(section.order()).append(". ")
                    .append(section.name()).append(" - ").append(section.description()).append("\n");
        }

        if (!optionalSections.isEmpty()) {
            sb.append("\nOPTIONAL SECTIONS:\n");
            for (SectionDefinition section : optionalSections) {
                sb.append("  - ").append(section.name()).append(" - ").append(section.description()).append("\n");
            }
        }

        sb.append("\nSTRUCTURE GUIDANCE:\n").append(structureGuidance).append("\n");
        sb.append("\nTONE:\n").append(toneGuidance).append("\n");

        if (contentType.requiresExamples()) {
            sb.append("\nEXAMPLES: Include at least ").append(minExamples)
                    .append(" concrete example(s) to illustrate key points.\n");
        }

        return sb.toString();
    }

    private static final Map<ContentType, ContentTypeTemplate> TEMPLATES = Map.of(
            ContentType.CONCEPT, new ContentTypeTemplate(
                    ContentType.CONCEPT,
                    List.of(
                            new SectionDefinition("Definition", "Clear, concise definition of the concept", true, 1),
                            new SectionDefinition("Context", "Why this concept matters and where it fits", true, 2),
                            new SectionDefinition("Key Characteristics", "Main attributes or properties", true, 3),
                            new SectionDefinition("Examples", "Concrete examples illustrating the concept", true, 4)
                    ),
                    List.of(
                            new SectionDefinition("History", "Historical background if relevant", false, 0),
                            new SectionDefinition("Common Misconceptions", "Clarify frequent misunderstandings", false, 0),
                            new SectionDefinition("See Also", "Related concepts and further reading", false, 99)
                    ),
                    """
                            Start with a clear, jargon-free definition in the first paragraph.
                            Build context by explaining why the reader should care.
                            Use the Key Characteristics section for a structured breakdown.
                            End with concrete examples that make the abstract tangible.
                            Include a See Also section linking to related concepts.""",
                    """
                            Encyclopedic and authoritative, but accessible.
                            Explain technical terms when first used.
                            Use present tense for facts, past tense for history.
                            Maintain neutral point of view.""",
                    2
            ),

            ContentType.TUTORIAL, new ContentTypeTemplate(
                    ContentType.TUTORIAL,
                    List.of(
                            new SectionDefinition("Goal", "What the reader will accomplish", true, 1),
                            new SectionDefinition("Prerequisites", "What the reader needs before starting", true, 2),
                            new SectionDefinition("Steps", "Numbered steps to complete the task", true, 3),
                            new SectionDefinition("Verification", "How to confirm success", true, 4)
                    ),
                    List.of(
                            new SectionDefinition("Time Estimate", "How long this will take", false, 0),
                            new SectionDefinition("Troubleshooting", "Common problems and solutions", false, 0),
                            new SectionDefinition("Next Steps", "What to learn or do next", false, 99)
                    ),
                    """
                            Begin with a clear statement of what the reader will achieve.
                            List prerequisites explicitly - don't assume knowledge.
                            Number all steps and keep each step focused on one action.
                            Include expected output or results after key steps.
                            Provide a clear way to verify the tutorial worked.""",
                    """
                            Friendly and encouraging, like a helpful colleague.
                            Use imperative mood for instructions ("Click the button", not "You should click").
                            Be specific - use exact names, paths, and values.
                            Acknowledge when something might be confusing.""",
                    3
            ),

            ContentType.REFERENCE, new ContentTypeTemplate(
                    ContentType.REFERENCE,
                    List.of(
                            new SectionDefinition("Overview", "Brief description of what this references", true, 1),
                            new SectionDefinition("Reference Content", "The main reference material (tables, lists, specs)", true, 2)
                    ),
                    List.of(
                            new SectionDefinition("Usage Notes", "Important considerations when using", false, 0),
                            new SectionDefinition("Related References", "Links to related reference material", false, 99)
                    ),
                    """
                            Optimize for quick lookup, not linear reading.
                            Use tables for structured data.
                            Keep descriptions brief and factual.
                            Organize alphabetically or by logical grouping.
                            Include all essential details; omit editorial content.""",
                    """
                            Concise and factual.
                            No opinions or recommendations.
                            Consistent formatting throughout.
                            Use standard terminology.""",
                    0
            ),

            ContentType.GUIDE, new ContentTypeTemplate(
                    ContentType.GUIDE,
                    List.of(
                            new SectionDefinition("Overview", "The decision or situation this guide addresses", true, 1),
                            new SectionDefinition("Options", "Available choices or approaches", true, 2),
                            new SectionDefinition("Trade-offs", "Pros, cons, and considerations for each option", true, 3),
                            new SectionDefinition("Recommendations", "Guidance on when to choose what", true, 4)
                    ),
                    List.of(
                            new SectionDefinition("Decision Criteria", "Factors to consider when deciding", false, 0),
                            new SectionDefinition("Examples", "Real-world examples of each approach", false, 0),
                            new SectionDefinition("Further Reading", "Resources for deeper exploration", false, 99)
                    ),
                    """
                            Frame the decision clearly at the start.
                            Present options objectively before adding recommendations.
                            Be explicit about trade-offs - nothing is perfect.
                            Recommendations should be conditional ("If X, then Y").
                            Help the reader understand, not just follow instructions.""",
                    """
                            Thoughtful and balanced.
                            Acknowledge complexity and nuance.
                            Support recommendations with reasoning.
                            Respect the reader's ability to decide.""",
                    2
            ),

            ContentType.COMPARISON, new ContentTypeTemplate(
                    ContentType.COMPARISON,
                    List.of(
                            new SectionDefinition("Introduction", "What's being compared and why", true, 1),
                            new SectionDefinition("Comparison Criteria", "The dimensions being evaluated", true, 2),
                            new SectionDefinition("Detailed Comparison", "Analysis of each option by criteria", true, 3),
                            new SectionDefinition("Summary Table", "Quick-reference comparison table", true, 4)
                    ),
                    List.of(
                            new SectionDefinition("When to Choose Each", "Situational recommendations", false, 0),
                            new SectionDefinition("Migration Considerations", "Moving from one to another", false, 0)
                    ),
                    """
                            Establish fair, relevant comparison criteria upfront.
                            Evaluate each option against the same criteria.
                            Include a summary table for quick scanning.
                            Be balanced - acknowledge strengths of all options.
                            Situational recommendations are more useful than absolute rankings.""",
                    """
                            Objective and fair.
                            Evidence-based claims.
                            Avoid favoritism; acknowledge your perspective if relevant.
                            Focus on facts, not marketing language.""",
                    1
            ),

            ContentType.TROUBLESHOOTING, new ContentTypeTemplate(
                    ContentType.TROUBLESHOOTING,
                    List.of(
                            new SectionDefinition("Problem Description", "Clear description of the issue", true, 1),
                            new SectionDefinition("Symptoms", "How the problem manifests", true, 2),
                            new SectionDefinition("Causes", "Why this happens", true, 3),
                            new SectionDefinition("Solutions", "Steps to resolve the issue", true, 4)
                    ),
                    List.of(
                            new SectionDefinition("Prevention", "How to avoid this problem", false, 0),
                            new SectionDefinition("Related Issues", "Similar problems to check", false, 99)
                    ),
                    """
                            Start with recognizable symptoms so readers can confirm they have this issue.
                            Explain causes briefly - understanding helps prevent recurrence.
                            Order solutions from simplest/most likely to complex/rare.
                            Include verification steps after each solution.
                            Link to related issues that might be confused with this one.""",
                    """
                            Direct and solution-focused.
                            Empathetic - the reader has a problem.
                            Clear and unambiguous instructions.
                            Reassuring when appropriate.""",
                    0
            ),

            ContentType.OVERVIEW, new ContentTypeTemplate(
                    ContentType.OVERVIEW,
                    List.of(
                            new SectionDefinition("Introduction", "What this topic area covers", true, 1),
                            new SectionDefinition("Key Components", "Major parts or aspects of the topic", true, 2),
                            new SectionDefinition("Getting Started", "Entry points for learning more", true, 3)
                    ),
                    List.of(
                            new SectionDefinition("History", "How this area developed", false, 0),
                            new SectionDefinition("Current State", "Where things stand today", false, 0),
                            new SectionDefinition("Learning Paths", "Suggested sequences for learning", false, 0)
                    ),
                    """
                            Provide the big picture without getting lost in details.
                            Map out the landscape - what are the main areas?
                            Help readers find their path into the content.
                            Link generously to more detailed pages.
                            This is a hub page - optimize for navigation.""",
                    """
                            Welcoming and orienting.
                            High-level and accessible.
                            Enthusiastic but not hype-driven.
                            Helpful for both newcomers and those seeking specific topics.""",
                    1
            ),

            ContentType.DEFINITION, new ContentTypeTemplate(
                    ContentType.DEFINITION,
                    List.of(
                            new SectionDefinition("Definition", "Clear, concise definition of the term", true, 1),
                            new SectionDefinition("Context", "Brief context on where this term is used", true, 2),
                            new SectionDefinition("See Also", "Links to related concepts", true, 3)
                    ),
                    List.of(),
                    """
                            Keep it brief - this is a stub/definition page, not a full article.
                            Start with a one-sentence definition.
                            Add 1-2 sentences of context explaining why this matters.
                            End with links to related pages where readers can learn more.
                            Target 100-250 words total.""",
                    """
                            Concise and direct.
                            Encyclopedic tone.
                            No unnecessary elaboration.
                            Focus on clarity over completeness.""",
                    0
            )
    );

    // Static validation: fail fast if any ContentType is missing a template
    static {
        for (ContentType type : ContentType.values()) {
            if (!TEMPLATES.containsKey(type)) {
                throw new ExceptionInInitializerError(
                    "ContentTypeTemplate.TEMPLATES is missing template for " + type +
                    ". When adding a new ContentType, you must also add a corresponding template.");
            }
        }
    }
}
