package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for content type question strategies.
 * Maps each ContentType to its corresponding question strategy.
 */
public class ContentTypeQuestionStrategyRegistry {

    private final Map<ContentType, ContentTypeQuestionStrategy> strategies = new HashMap<>();

    /**
     * Create a registry with default strategies for all content types.
     */
    public ContentTypeQuestionStrategyRegistry() {
        registerDefaults();
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
