package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Question strategy for Comparison content type.
 * Gathers information about comparison criteria and use case focus.
 */
public class ComparisonQuestionStrategy implements ContentTypeQuestionStrategy {

    private static final List<ContentTypeQuestion> QUESTIONS = List.of(
            ContentTypeQuestion.forSection(
                    "What criteria matter most to your audience? (e.g., performance, cost, ease of use)",
                    "Comparison criteria: "),
            ContentTypeQuestion.forContext(
                    "Any specific use case to focus on?",
                    "general comparison")
    );

    @Override
    public Set<ContentType> getApplicableTypes() {
        return Set.of(ContentType.COMPARISON);
    }

    @Override
    public String getIntroText(ContentType contentType) {
        return "For this comparison:";
    }

    @Override
    public List<ContentTypeQuestion> getQuestions() {
        return QUESTIONS;
    }
}
