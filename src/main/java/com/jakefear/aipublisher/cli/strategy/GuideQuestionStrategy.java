package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Question strategy for Guide content type.
 * Gathers information about the decision being made and constraints.
 */
public class GuideQuestionStrategy implements ContentTypeQuestionStrategy {

    private static final List<ContentTypeQuestion> QUESTIONS = List.of(
            ContentTypeQuestion.forGoal("What decision or choice is the reader facing?"),
            ContentTypeQuestion.forSection(
                    "What constraints or considerations matter? (e.g., 'limited budget', 'high scale')",
                    "Constraints: ")
    );

    @Override
    public Set<ContentType> getApplicableTypes() {
        return Set.of(ContentType.GUIDE);
    }

    @Override
    public String getIntroText(ContentType contentType) {
        return "For this decision guide:";
    }

    @Override
    public List<ContentTypeQuestion> getQuestions() {
        return QUESTIONS;
    }
}
