package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for content-type-specific questions.
 * <p>
 * Each content type (Tutorial, Comparison, etc.) has different questions
 * to gather relevant configuration. This interface defines the contract
 * for providing those questions.
 * <p>
 * This is an implementation of the Strategy Pattern (Gang of Four),
 * allowing the question-asking algorithm to vary independently from
 * the InteractiveSession that uses it.
 */
public interface ContentTypeQuestionStrategy {

    /**
     * Get the content types this strategy handles.
     * Most strategies handle a single type, but some (like Concept)
     * may handle multiple related types.
     *
     * @return Set of content types this strategy applies to
     */
    Set<ContentType> getApplicableTypes();

    /**
     * Get the introductory text shown before questions.
     *
     * @param contentType The specific content type being configured
     * @return Intro text to display
     */
    String getIntroText(ContentType contentType);

    /**
     * Get the list of questions to ask for this content type.
     *
     * @return Ordered list of questions
     */
    List<ContentTypeQuestion> getQuestions();
}
