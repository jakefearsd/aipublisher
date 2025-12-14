package com.jakefear.aipublisher.cli.strategy;

/**
 * Defines where a question's answer should be stored.
 */
public enum QuestionTarget {
    /**
     * Answer goes to the specific goal field.
     */
    SPECIFIC_GOAL,

    /**
     * Answer goes to the domain context field.
     */
    DOMAIN_CONTEXT,

    /**
     * Answer is added to the required sections list with a prefix.
     */
    REQUIRED_SECTION
}
