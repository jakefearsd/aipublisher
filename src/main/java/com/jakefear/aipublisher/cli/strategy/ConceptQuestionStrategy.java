package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Question strategy for Concept, Overview, Reference, and Definition content types.
 * These types share similar question patterns about context and focus areas.
 */
public class ConceptQuestionStrategy implements ContentTypeQuestionStrategy {

    private static final List<ContentTypeQuestion> QUESTIONS = List.of(
            ContentTypeQuestion.forContext(
                    "What's the broader context or system this fits into?",
                    "none"),
            ContentTypeQuestion.forSection(
                    "Any specific aspects to emphasize?",
                    "Focus: ")
    );

    @Override
    public Set<ContentType> getApplicableTypes() {
        return Set.of(ContentType.CONCEPT, ContentType.OVERVIEW, ContentType.REFERENCE, ContentType.DEFINITION);
    }

    @Override
    public String getIntroText(ContentType contentType) {
        return "For this " + contentType.getDisplayName().toLowerCase() + ":";
    }

    @Override
    public List<ContentTypeQuestion> getQuestions() {
        return QUESTIONS;
    }
}
