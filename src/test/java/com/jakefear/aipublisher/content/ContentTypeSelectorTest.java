package com.jakefear.aipublisher.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentTypeSelector")
class ContentTypeSelectorTest {

    private ContentTypeSelector selector;

    @BeforeEach
    void setUp() {
        selector = new ContentTypeSelector();
    }

    @Nested
    @DisplayName("detectFromTopic")
    class DetectFromTopic {

        @Test
        @DisplayName("Detects tutorial from 'how to' topics")
        void detectsTutorialFromHowTo() {
            assertEquals(ContentType.TUTORIAL, selector.detectFromTopic("How to set up Docker"));
            assertEquals(ContentType.TUTORIAL, selector.detectFromTopic("How To Configure Kubernetes"));
        }

        @Test
        @DisplayName("Detects tutorial from 'getting started' topics")
        void detectsTutorialFromGettingStarted() {
            assertEquals(ContentType.TUTORIAL, selector.detectFromTopic("Getting started with React"));
        }

        @Test
        @DisplayName("Detects comparison from 'vs' topics")
        void detectsComparisonFromVs() {
            assertEquals(ContentType.COMPARISON, selector.detectFromTopic("Kafka vs RabbitMQ"));
            assertEquals(ContentType.COMPARISON, selector.detectFromTopic("Docker vs. Podman"));
        }

        @Test
        @DisplayName("Detects comparison from 'versus' topics")
        void detectsComparisonFromVersus() {
            assertEquals(ContentType.COMPARISON, selector.detectFromTopic("Git versus SVN"));
        }

        @Test
        @DisplayName("Detects comparison from 'difference between' topics")
        void detectsComparisonFromDifferences() {
            assertEquals(ContentType.COMPARISON, selector.detectFromTopic("Differences between REST and GraphQL"));
        }

        @Test
        @DisplayName("Detects troubleshooting from 'fixing' topics")
        void detectsTroubleshootingFromFix() {
            assertEquals(ContentType.TROUBLESHOOTING, selector.detectFromTopic("Fixing connection timeout errors"));
            assertEquals(ContentType.TROUBLESHOOTING, selector.detectFromTopic("Fix slow database queries"));
        }

        @Test
        @DisplayName("Detects troubleshooting from 'error' topics")
        void detectsTroubleshootingFromError() {
            assertEquals(ContentType.TROUBLESHOOTING, selector.detectFromTopic("Connection error in Java"));
            assertEquals(ContentType.TROUBLESHOOTING, selector.detectFromTopic("Solve errors in production"));
        }

        @Test
        @DisplayName("Detects troubleshooting from 'not working' topics")
        void detectsTroubleshootingFromNotWorking() {
            assertEquals(ContentType.TROUBLESHOOTING, selector.detectFromTopic("Docker container not working"));
        }

        @Test
        @DisplayName("Detects reference from 'api' topics")
        void detectsReferenceFromApi() {
            assertEquals(ContentType.REFERENCE, selector.detectFromTopic("REST API reference"));
        }

        @Test
        @DisplayName("Detects reference from 'cheat sheet' topics")
        void detectsReferenceFromCheatSheet() {
            assertEquals(ContentType.REFERENCE, selector.detectFromTopic("Git cheat sheet"));
            assertEquals(ContentType.REFERENCE, selector.detectFromTopic("Docker cheatsheet"));
        }

        @Test
        @DisplayName("Detects guide from 'best practices' topics")
        void detectsGuideFromBestPractices() {
            assertEquals(ContentType.GUIDE, selector.detectFromTopic("Best practices for database design"));
            assertEquals(ContentType.GUIDE, selector.detectFromTopic("Kubernetes best practice"));
        }

        @Test
        @DisplayName("Detects guide from 'when to use' topics")
        void detectsGuideFromWhenToUse() {
            assertEquals(ContentType.GUIDE, selector.detectFromTopic("When to use microservices"));
        }

        @Test
        @DisplayName("Detects overview from 'introduction to' topics")
        void detectsOverviewFromIntro() {
            assertEquals(ContentType.OVERVIEW, selector.detectFromTopic("Introduction to Machine Learning"));
            assertEquals(ContentType.OVERVIEW, selector.detectFromTopic("Intro to Kubernetes"));
        }

        @Test
        @DisplayName("Detects overview from 'what is' topics")
        void detectsOverviewFromWhatIs() {
            assertEquals(ContentType.OVERVIEW, selector.detectFromTopic("What is event-driven architecture"));
        }

        @Test
        @DisplayName("Defaults to concept for simple topics")
        void defaultsToConceptForSimpleTopics() {
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic("Apache Kafka"));
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic("Microservices"));
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic("Event Sourcing"));
        }

        @Test
        @DisplayName("Handles null and blank topics")
        void handlesNullAndBlankTopics() {
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic(null));
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic(""));
            assertEquals(ContentType.CONCEPT, selector.detectFromTopic("   "));
        }
    }

    @Nested
    @DisplayName("recommend")
    class Recommend {

        @Test
        @DisplayName("Returns recommendation with content type")
        void returnsRecommendationWithContentType() {
            ContentTypeSelector.ContentTypeRecommendation recommendation =
                selector.recommend("How to deploy Docker containers");

            assertNotNull(recommendation);
            assertEquals(ContentType.TUTORIAL, recommendation.contentType());
        }

        @Test
        @DisplayName("Returns recommendation with confidence score")
        void returnsRecommendationWithConfidence() {
            ContentTypeSelector.ContentTypeRecommendation recommendation =
                selector.recommend("How to set up Docker containers step by step");

            assertTrue(recommendation.confidence() > 0.5);
            assertTrue(recommendation.confidence() <= 1.0);
        }

        @Test
        @DisplayName("Returns recommendation with rationale")
        void returnsRecommendationWithRationale() {
            ContentTypeSelector.ContentTypeRecommendation recommendation =
                selector.recommend("Kafka vs RabbitMQ");

            assertNotNull(recommendation.rationale());
            assertFalse(recommendation.rationale().isBlank());
        }

        @Test
        @DisplayName("Higher confidence for multiple pattern matches")
        void higherConfidenceForMultiplePatterns() {
            // "How to" + "step by step" should have higher confidence
            ContentTypeSelector.ContentTypeRecommendation multiMatch =
                selector.recommend("How to deploy step by step tutorial");

            ContentTypeSelector.ContentTypeRecommendation singleMatch =
                selector.recommend("How to deploy");

            assertTrue(multiMatch.confidence() >= singleMatch.confidence());
        }
    }
}
