package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Question strategy for Tutorial content type.
 * Gathers information about learning goals, prerequisites, and domain context.
 */
public class TutorialQuestionStrategy implements ContentTypeQuestionStrategy {

    private static final List<ContentTypeQuestion> QUESTIONS = List.of(
            ContentTypeQuestion.forGoal("What will readers accomplish? (specific goal)"),
            ContentTypeQuestion.forSection(
                    "What tools/technologies should readers have? (prerequisites)",
                    "Prerequisites: "),
            ContentTypeQuestion.forContext(
                    "Any specific domain context (e.g., 'e-commerce', 'microservices')?",
                    "none")
    );

    @Override
    public Set<ContentType> getApplicableTypes() {
        return Set.of(ContentType.TUTORIAL);
    }

    @Override
    public String getIntroText(ContentType contentType) {
        return "For this tutorial, let's define the scope:";
    }

    @Override
    public List<ContentTypeQuestion> getQuestions() {
        return QUESTIONS;
    }
}
