package com.jakefear.aipublisher;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JUnit 5 annotation to enable tests only when an LLM provider is available.
 * Tests are enabled if either OLLAMA_BASE_URL or ANTHROPIC_API_KEY is set.
 *
 * Usage:
 * <pre>
 * {@code
 * @EnabledIfLlmAvailable
 * class MyIntegrationTest {
 *     // tests here run only when an LLM is available
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(EnabledIfLlmAvailable.Condition.class)
public @interface EnabledIfLlmAvailable {

    class Condition implements ExecutionCondition {
        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            if (IntegrationTestHelper.isLlmAvailable()) {
                return ConditionEvaluationResult.enabled(
                        "LLM available: " + IntegrationTestHelper.getProviderName());
            }
            return ConditionEvaluationResult.disabled(
                    "No LLM provider configured. Set OLLAMA_BASE_URL or ANTHROPIC_API_KEY.");
        }
    }
}
