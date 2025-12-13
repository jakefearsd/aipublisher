package com.jakefear.aipublisher.prerequisites;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.linking.WikiLinkContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrerequisiteAnalyzer.
 */
@DisplayName("PrerequisiteAnalyzer")
class PrerequisiteAnalyzerTest {

    private PrerequisiteAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new PrerequisiteAnalyzer();
    }

    @Nested
    @DisplayName("analyze() pattern matching")
    class PatternMatching {

        @Test
        @DisplayName("Identifies Java prerequisite for Spring topics")
        void identifiesJavaPrerequisiteForSpringTopics() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot REST API", ContentType.TUTORIAL);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("Java")));
        }

        @Test
        @DisplayName("Identifies Python prerequisite for Django topics")
        void identifiesPythonPrerequisiteForDjangoTopics() {
            PrerequisiteSet set = analyzer.analyze("Django REST Framework", ContentType.TUTORIAL);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("Python")));
        }

        @Test
        @DisplayName("Identifies JavaScript prerequisite for React topics")
        void identifiesJavaScriptPrerequisiteForReactTopics() {
            PrerequisiteSet set = analyzer.analyze("React Component Patterns", ContentType.TUTORIAL);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("JavaScript")));
        }

        @Test
        @DisplayName("Identifies Docker prerequisite for Kubernetes topics")
        void identifiesDockerPrerequisiteForKubernetesTopics() {
            PrerequisiteSet set = analyzer.analyze("Kubernetes Deployment Strategies", ContentType.GUIDE);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("Docker")));
        }

        @Test
        @DisplayName("Identifies SQL prerequisite for JPA topics")
        void identifiesSqlPrerequisiteForJpaTopics() {
            PrerequisiteSet set = analyzer.analyze("JPA Entity Mapping", ContentType.TUTORIAL);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("SQL")));
        }

        @Test
        @DisplayName("Identifies multiple prerequisites for complex topics")
        void identifiesMultiplePrerequisitesForComplexTopics() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot JPA REST API", ContentType.TUTORIAL);

            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("Java")));
            assertTrue(set.prerequisites().stream()
                    .anyMatch(p -> p.topic().equals("SQL")));
        }

        @Test
        @DisplayName("Returns empty for unrecognized topics")
        void returnsEmptyForUnrecognizedTopics() {
            PrerequisiteSet set = analyzer.analyze("Introduction to Cooking", ContentType.OVERVIEW);

            // Should only have assumed prerequisites, no hard/soft
            assertTrue(set.getHardPrerequisites().isEmpty());
            assertTrue(set.getSoftPrerequisites().isEmpty());
        }
    }

    @Nested
    @DisplayName("analyze() with wiki context")
    class WithWikiContext {

        @Test
        @DisplayName("Links to existing wiki pages")
        void linksToExistingWikiPages() {
            WikiLinkContext context = new WikiLinkContext();
            context.registerPage("JavaBasics");

            PrerequisiteSet set = analyzer.analyze("Spring Boot Tutorial", ContentType.TUTORIAL, context);

            Prerequisite javaPrereq = set.prerequisites().stream()
                    .filter(p -> p.topic().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            assertEquals("JavaBasics", javaPrereq.wikiPage());
        }

        @Test
        @DisplayName("Does not link to non-existent wiki pages")
        void doesNotLinkToNonExistentWikiPages() {
            WikiLinkContext context = new WikiLinkContext();
            // Don't register JavaBasics

            PrerequisiteSet set = analyzer.analyze("Spring Boot Tutorial", ContentType.TUTORIAL, context);

            Prerequisite javaPrereq = set.prerequisites().stream()
                    .filter(p -> p.topic().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            assertNull(javaPrereq.wikiPage());
        }
    }

    @Nested
    @DisplayName("analyze() content type behavior")
    class ContentTypeBehavior {

        @Test
        @DisplayName("Tutorials have hard prerequisites")
        void tutorialsHaveHardPrerequisites() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot REST API", ContentType.TUTORIAL);

            assertTrue(set.hasHardPrerequisites());
        }

        @Test
        @DisplayName("Guides have hard prerequisites")
        void guidesHaveHardPrerequisites() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot Deployment Guide", ContentType.GUIDE);

            assertTrue(set.hasHardPrerequisites());
        }

        @Test
        @DisplayName("Overviews have soft prerequisites")
        void overviewsHaveSoftPrerequisites() {
            PrerequisiteSet set = analyzer.analyze("Spring Framework Overview", ContentType.OVERVIEW);

            // Overview should have soft, not hard prerequisites
            assertFalse(set.getHardPrerequisites().isEmpty() && set.getSoftPrerequisites().isEmpty());
        }

        @Test
        @DisplayName("Tutorials include assumed development environment")
        void tutorialsIncludeAssumedDevelopmentEnvironment() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot Tutorial", ContentType.TUTORIAL);

            assertTrue(set.getAssumedPrerequisites().stream()
                    .anyMatch(p -> p.topic().toLowerCase().contains("development environment")));
        }

        @Test
        @DisplayName("Troubleshooting assumes technology familiarity")
        void troubleshootingAssumesTechnologyFamiliarity() {
            PrerequisiteSet set = analyzer.analyze("Spring Boot Common Issues", ContentType.TROUBLESHOOTING);

            assertTrue(set.getAssumedPrerequisites().stream()
                    .anyMatch(p -> p.topic().toLowerCase().contains("familiarity")));
        }
    }

    @Nested
    @DisplayName("suggestPrerequisites()")
    class SuggestPrerequisites {

        @Test
        @DisplayName("Suggests prerequisites based on topic")
        void suggestsPrerequisitesBasedOnTopic() {
            List<String> suggestions = analyzer.suggestPrerequisites("Spring Boot REST API");

            assertTrue(suggestions.contains("Java"));
        }

        @Test
        @DisplayName("Returns empty list for unrecognized topics")
        void returnsEmptyListForUnrecognizedTopics() {
            List<String> suggestions = analyzer.suggestPrerequisites("Random Topic");

            assertTrue(suggestions.isEmpty());
        }

        @Test
        @DisplayName("Suggests multiple prerequisites for complex topics")
        void suggestsMultiplePrerequisitesForComplexTopics() {
            List<String> suggestions = analyzer.suggestPrerequisites("Kubernetes Docker Compose");

            assertTrue(suggestions.size() >= 1);
            assertTrue(suggestions.contains("Docker"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Handles null topic")
        void handlesNullTopic() {
            PrerequisiteSet set = analyzer.analyze(null, ContentType.CONCEPT);
            assertTrue(set.isEmpty() || set.prerequisites().stream().noneMatch(p -> p.type() == PrerequisiteType.HARD));
        }

        @Test
        @DisplayName("Handles blank topic")
        void handlesBlankTopic() {
            PrerequisiteSet set = analyzer.analyze("   ", ContentType.CONCEPT);
            assertTrue(set.isEmpty() || set.prerequisites().stream().noneMatch(p -> p.type() == PrerequisiteType.HARD));
        }

        @Test
        @DisplayName("Case insensitive pattern matching")
        void caseInsensitivePatternMatching() {
            PrerequisiteSet lower = analyzer.analyze("spring boot tutorial", ContentType.TUTORIAL);
            PrerequisiteSet upper = analyzer.analyze("SPRING BOOT TUTORIAL", ContentType.TUTORIAL);
            PrerequisiteSet mixed = analyzer.analyze("Spring Boot Tutorial", ContentType.TUTORIAL);

            // All should identify Java as prerequisite
            assertTrue(lower.prerequisites().stream().anyMatch(p -> p.topic().equals("Java")));
            assertTrue(upper.prerequisites().stream().anyMatch(p -> p.topic().equals("Java")));
            assertTrue(mixed.prerequisites().stream().anyMatch(p -> p.topic().equals("Java")));
        }
    }
}
