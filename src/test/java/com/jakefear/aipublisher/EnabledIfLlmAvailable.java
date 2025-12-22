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
 * JUnit 5 annotation to enable tests only when Ollama is available.
 * Tests are enabled if the Ollama server is reachable.
 *
 * Usage:
 * <pre>
 * {@code
 * @EnabledIfLlmAvailable
 * class MyIntegrationTest {
 *     // tests here run only when Ollama is available
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
                        "Ollama available: " + IntegrationTestHelper.getProviderName());
            }
            return ConditionEvaluationResult.disabled(
                    "Ollama server not reachable at " + IntegrationTestHelper.getOllamaBaseUrl());
        }
    }
}
