package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for content type question strategies.
 * Maps each ContentType to its corresponding question strategy.
 *
 * <p><b>IMPORTANT:</b> When adding a new {@link ContentType}, you MUST ensure
 * one of the registered strategies handles it (via {@code getApplicableTypes()}).
 * The constructor validates this and throws {@link IllegalStateException} if
 * any ContentType is unhandled.</p>
 *
 * @see ContentType
 * @see ContentTypeQuestionStrategy#getApplicableTypes()
 */
public class ContentTypeQuestionStrategyRegistry {

    private final Map<ContentType, ContentTypeQuestionStrategy> strategies = new HashMap<>();

    /**
     * Create a registry with default strategies for all content types.
     *
     * @throws IllegalStateException if any ContentType is not handled by a strategy
     */
    public ContentTypeQuestionStrategyRegistry() {
        registerDefaults();
        validateAllTypesHandled();
    }

    /**
     * Register the default strategies for all content types.
     */
    private void registerDefaults() {
        List<ContentTypeQuestionStrategy> defaultStrategies = List.of(
                new TutorialQuestionStrategy(),
                new ComparisonQuestionStrategy(),
                new TroubleshootingQuestionStrategy(),
                new GuideQuestionStrategy(),
                new ConceptQuestionStrategy()
        );

        for (ContentTypeQuestionStrategy strategy : defaultStrategies) {
            register(strategy);
        }
    }

    /**
     * Validate that all ContentType values have a registered strategy.
     * This catches missing strategies at startup rather than runtime.
     */
    private void validateAllTypesHandled() {
        for (ContentType type : ContentType.values()) {
            if (!strategies.containsKey(type)) {
                throw new IllegalStateException(
                    "ContentTypeQuestionStrategyRegistry has no strategy for " + type +
                    ". When adding a new ContentType, you must update an existing strategy's " +
                    "getApplicableTypes() or create a new strategy.");
            }
        }
    }

    /**
     * Register a strategy for its applicable content types.
     *
     * @param strategy The strategy to register
     */
    public void register(ContentTypeQuestionStrategy strategy) {
        for (ContentType type : strategy.getApplicableTypes()) {
            strategies.put(type, strategy);
        }
    }

    /**
     * Get the strategy for a content type.
     *
     * @param contentType The content type
     * @return The strategy, or null if none registered
     */
    public ContentTypeQuestionStrategy getStrategy(ContentType contentType) {
        return strategies.get(contentType);
    }

    /**
     * Check if a strategy exists for the given content type.
     *
     * @param contentType The content type to check
     * @return true if a strategy is registered
     */
    public boolean hasStrategy(ContentType contentType) {
        return strategies.containsKey(contentType);
    }
}
