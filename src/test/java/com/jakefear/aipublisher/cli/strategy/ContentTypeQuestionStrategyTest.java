package com.jakefear.aipublisher.cli.strategy;

import com.jakefear.aipublisher.content.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ContentTypeQuestionStrategy implementations.
 */
@DisplayName("ContentTypeQuestionStrategy")
class ContentTypeQuestionStrategyTest {

    @Nested
    @DisplayName("TutorialQuestionStrategy")
    class TutorialStrategyTests {

        private final TutorialQuestionStrategy strategy = new TutorialQuestionStrategy();

        @Test
        @DisplayName("Applies only to TUTORIAL content type")
        void appliesOnlyToTutorial() {
            assertTrue(strategy.getApplicableTypes().contains(ContentType.TUTORIAL));
            assertEquals(1, strategy.getApplicableTypes().size());
        }

        @Test
        @DisplayName("Provides appropriate intro text")
        void providesIntroText() {
            String intro = strategy.getIntroText(ContentType.TUTORIAL);
            assertTrue(intro.contains("tutorial"));
        }

        @Test
        @DisplayName("Has three questions")
        void hasThreeQuestions() {
            List<ContentTypeQuestion> questions = strategy.getQuestions();
            assertEquals(3, questions.size());
        }

        @Test
        @DisplayName("Questions target appropriate fields")
        void questionsTargetCorrectFields() {
            List<ContentTypeQuestion> questions = strategy.getQuestions();

            assertEquals(QuestionTarget.SPECIFIC_GOAL, questions.get(0).target());
            assertEquals(QuestionTarget.REQUIRED_SECTION, questions.get(1).target());
            assertEquals(QuestionTarget.DOMAIN_CONTEXT, questions.get(2).target());
        }

        @Test
        @DisplayName("Prerequisites question has section prefix")
        void prerequisitesQuestionHasPrefix() {
            List<ContentTypeQuestion> questions = strategy.getQuestions();
            ContentTypeQuestion prereqQuestion = questions.get(1);

            assertEquals("Prerequisites: ", prereqQuestion.getSectionPrefix());
        }
    }

    @Nested
    @DisplayName("ComparisonQuestionStrategy")
    class ComparisonStrategyTests {

        private final ComparisonQuestionStrategy strategy = new ComparisonQuestionStrategy();

        @Test
        @DisplayName("Applies only to COMPARISON content type")
        void appliesOnlyToComparison() {
            assertTrue(strategy.getApplicableTypes().contains(ContentType.COMPARISON));
            assertEquals(1, strategy.getApplicableTypes().size());
        }

        @Test
        @DisplayName("Has two questions")
        void hasTwoQuestions() {
            assertEquals(2, strategy.getQuestions().size());
        }

        @Test
        @DisplayName("Criteria question targets required section")
        void criteriaQuestionTargetsSection() {
            ContentTypeQuestion criteriaQ = strategy.getQuestions().get(0);
            assertEquals(QuestionTarget.REQUIRED_SECTION, criteriaQ.target());
            assertEquals("Comparison criteria: ", criteriaQ.getSectionPrefix());
        }
    }

    @Nested
    @DisplayName("TroubleshootingQuestionStrategy")
    class TroubleshootingStrategyTests {

        private final TroubleshootingQuestionStrategy strategy = new TroubleshootingQuestionStrategy();

        @Test
        @DisplayName("Applies only to TROUBLESHOOTING content type")
        void appliesOnlyToTroubleshooting() {
            assertTrue(strategy.getApplicableTypes().contains(ContentType.TROUBLESHOOTING));
            assertEquals(1, strategy.getApplicableTypes().size());
        }

        @Test
        @DisplayName("Has two questions")
        void hasTwoQuestions() {
            assertEquals(2, strategy.getQuestions().size());
        }

        @Test
        @DisplayName("Symptoms question targets required section")
        void symptomsQuestionTargetsSection() {
            ContentTypeQuestion symptomsQ = strategy.getQuestions().get(0);
            assertEquals(QuestionTarget.REQUIRED_SECTION, symptomsQ.target());
            assertEquals("Symptoms: ", symptomsQ.getSectionPrefix());
        }
    }

    @Nested
    @DisplayName("GuideQuestionStrategy")
    class GuideStrategyTests {

        private final GuideQuestionStrategy strategy = new GuideQuestionStrategy();

        @Test
        @DisplayName("Applies only to GUIDE content type")
        void appliesOnlyToGuide() {
            assertTrue(strategy.getApplicableTypes().contains(ContentType.GUIDE));
            assertEquals(1, strategy.getApplicableTypes().size());
        }

        @Test
        @DisplayName("Has two questions")
        void hasTwoQuestions() {
            assertEquals(2, strategy.getQuestions().size());
        }

        @Test
        @DisplayName("Decision question targets specific goal")
        void decisionQuestionTargetsGoal() {
            ContentTypeQuestion decisionQ = strategy.getQuestions().get(0);
            assertEquals(QuestionTarget.SPECIFIC_GOAL, decisionQ.target());
        }
    }

    @Nested
    @DisplayName("ConceptQuestionStrategy")
    class ConceptStrategyTests {

        private final ConceptQuestionStrategy strategy = new ConceptQuestionStrategy();

        @Test
        @DisplayName("Applies to CONCEPT, OVERVIEW, REFERENCE, and DEFINITION")
        void appliesToMultipleTypes() {
            assertTrue(strategy.getApplicableTypes().contains(ContentType.CONCEPT));
            assertTrue(strategy.getApplicableTypes().contains(ContentType.OVERVIEW));
            assertTrue(strategy.getApplicableTypes().contains(ContentType.REFERENCE));
            assertTrue(strategy.getApplicableTypes().contains(ContentType.DEFINITION));
            assertEquals(4, strategy.getApplicableTypes().size());
        }

        @Test
        @DisplayName("Intro text includes content type name")
        void introTextIncludesTypeName() {
            String conceptIntro = strategy.getIntroText(ContentType.CONCEPT);
            assertTrue(conceptIntro.toLowerCase().contains("concept"));

            String overviewIntro = strategy.getIntroText(ContentType.OVERVIEW);
            assertTrue(overviewIntro.toLowerCase().contains("overview"));

            String referenceIntro = strategy.getIntroText(ContentType.REFERENCE);
            assertTrue(referenceIntro.toLowerCase().contains("reference"));
        }

        @Test
        @DisplayName("Has two questions")
        void hasTwoQuestions() {
            assertEquals(2, strategy.getQuestions().size());
        }
    }

    @Nested
    @DisplayName("ContentTypeQuestionStrategyRegistry")
    class RegistryTests {

        private final ContentTypeQuestionStrategyRegistry registry =
                new ContentTypeQuestionStrategyRegistry();

        @Test
        @DisplayName("All content types have a registered strategy")
        void allTypesHaveStrategy() {
            for (ContentType type : ContentType.values()) {
                assertTrue(registry.hasStrategy(type),
                        "No strategy registered for " + type);
            }
        }

        @Test
        @DisplayName("Returns correct strategy for each type")
        void returnsCorrectStrategy() {
            assertInstanceOf(TutorialQuestionStrategy.class,
                    registry.getStrategy(ContentType.TUTORIAL));
            assertInstanceOf(ComparisonQuestionStrategy.class,
                    registry.getStrategy(ContentType.COMPARISON));
            assertInstanceOf(TroubleshootingQuestionStrategy.class,
                    registry.getStrategy(ContentType.TROUBLESHOOTING));
            assertInstanceOf(GuideQuestionStrategy.class,
                    registry.getStrategy(ContentType.GUIDE));
            assertInstanceOf(ConceptQuestionStrategy.class,
                    registry.getStrategy(ContentType.CONCEPT));
            assertInstanceOf(ConceptQuestionStrategy.class,
                    registry.getStrategy(ContentType.OVERVIEW));
            assertInstanceOf(ConceptQuestionStrategy.class,
                    registry.getStrategy(ContentType.REFERENCE));
        }
    }

    @Nested
    @DisplayName("ContentTypeQuestion")
    class QuestionTests {

        @Test
        @DisplayName("forGoal creates goal-targeted question")
        void forGoalCreatesGoalQuestion() {
            ContentTypeQuestion q = ContentTypeQuestion.forGoal("Test prompt");
            assertEquals("Test prompt", q.prompt());
            assertEquals(QuestionTarget.SPECIFIC_GOAL, q.target());
            assertNull(q.defaultValue());
        }

        @Test
        @DisplayName("forContext creates context-targeted question")
        void forContextCreatesContextQuestion() {
            ContentTypeQuestion q = ContentTypeQuestion.forContext("Test prompt", "default");
            assertEquals("Test prompt", q.prompt());
            assertEquals(QuestionTarget.DOMAIN_CONTEXT, q.target());
            assertEquals("default", q.getDisplayDefault());
        }

        @Test
        @DisplayName("forSection creates section-targeted question with prefix")
        void forSectionCreatesSectionQuestion() {
            ContentTypeQuestion q = ContentTypeQuestion.forSection("Test prompt", "Prefix: ");
            assertEquals("Test prompt", q.prompt());
            assertEquals(QuestionTarget.REQUIRED_SECTION, q.target());
            assertEquals("Prefix: ", q.getSectionPrefix());
            assertNull(q.getDisplayDefault()); // prefix is stored in defaultValue, not displayed
        }

        @Test
        @DisplayName("getSectionPrefix returns empty string for non-section questions")
        void getSectionPrefixReturnsEmptyForNonSection() {
            ContentTypeQuestion goalQ = ContentTypeQuestion.forGoal("Test");
            assertEquals("", goalQ.getSectionPrefix());

            ContentTypeQuestion contextQ = ContentTypeQuestion.forContext("Test");
            assertEquals("", contextQ.getSectionPrefix());
        }
    }
}
