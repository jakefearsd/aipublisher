package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Question strategy for Troubleshooting content type.
 * Gathers information about symptoms and environment context.
 */
public class TroubleshootingQuestionStrategy implements ContentTypeQuestionStrategy {

    private static final List<ContentTypeQuestion> QUESTIONS = List.of(
            ContentTypeQuestion.forSection(
                    "What are the main symptoms readers will see?",
                    "Symptoms: "),
            ContentTypeQuestion.forContext(
                    "What environment or context? (e.g., 'production Kubernetes', 'local Docker')")
    );

    @Override
    public Set<ContentType> getApplicableTypes() {
        return Set.of(ContentType.TROUBLESHOOTING);
    }

    @Override
    public String getIntroText(ContentType contentType) {
        return "For this troubleshooting guide:";
    }

    @Override
    public List<ContentTypeQuestion> getQuestions() {
        return QUESTIONS;
    }
}
