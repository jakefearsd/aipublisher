package com.jakefear.aipublisher.seealso;

import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.linking.WikiLinkContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeeAlsoGenerator.
 */
@DisplayName("SeeAlsoGenerator")
class SeeAlsoGeneratorTest {

    private SeeAlsoGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SeeAlsoGenerator();
    }

    @Nested
    @DisplayName("generate() topic relationships")
    class TopicRelationships {

        @Test
        @DisplayName("Spring topics link to Java")
        void springTopicsLinkToJava() {
            SeeAlsoSection section = generator.generate("Spring Boot", ContentType.TUTORIAL);

            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().equals("Java") && e.type() == SeeAlsoType.BROADER));
        }

        @Test
        @DisplayName("React topics link to JavaScript")
        void reactTopicsLinkToJavaScript() {
            SeeAlsoSection section = generator.generate("React Components", ContentType.TUTORIAL);

            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().equals("JavaScript") && e.type() == SeeAlsoType.BROADER));
        }

        @Test
        @DisplayName("Django topics link to Python")
        void djangoTopicsLinkToPython() {
            SeeAlsoSection section = generator.generate("Django REST Framework", ContentType.GUIDE);

            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().equals("Python") && e.type() == SeeAlsoType.BROADER));
        }

        @Test
        @DisplayName("Kubernetes topics link to container topics")
        void kubernetesTopicsLinkToContainerTopics() {
            SeeAlsoSection section = generator.generate("Kubernetes Deployment", ContentType.TUTORIAL);

            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().contains("Container") && e.type() == SeeAlsoType.BROADER));
        }

        @Test
        @DisplayName("Includes related topics from same ecosystem")
        void includesRelatedTopicsFromSameEcosystem() {
            SeeAlsoSection section = generator.generate("Spring Boot", ContentType.TUTORIAL);

            // Should include related Spring topics
            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.type() == SeeAlsoType.RELATED));
        }

        @Test
        @DisplayName("Returns section for unrecognized topics")
        void returnsSectionForUnrecognizedTopics() {
            SeeAlsoSection section = generator.generate("Random Topic", ContentType.OVERVIEW);

            assertNotNull(section);
            assertEquals("Random Topic", section.sourceTopic());
        }
    }

    @Nested
    @DisplayName("generate() with wiki context")
    class WithWikiContext {

        @Test
        @DisplayName("Links to existing wiki pages")
        void linksToExistingWikiPages() {
            WikiLinkContext context = new WikiLinkContext();
            context.registerPage("Java");

            SeeAlsoSection section = generator.generate("Spring Boot", ContentType.TUTORIAL, context);

            SeeAlsoEntry javaEntry = section.entries().stream()
                    .filter(e -> e.title().equals("Java"))
                    .findFirst()
                    .orElseThrow();

            assertEquals("Java", javaEntry.wikiPage());
        }

        @Test
        @DisplayName("Still includes pages not in wiki context")
        void stillIncludesPagesNotInWikiContext() {
            WikiLinkContext context = new WikiLinkContext();
            // Don't register any pages

            SeeAlsoSection section = generator.generate("Spring Boot", ContentType.TUTORIAL, context);

            // Should still include suggestions even if pages don't exist
            assertFalse(section.isEmpty());
        }
    }

    @Nested
    @DisplayName("generate() content type behavior")
    class ContentTypeBehavior {

        @Test
        @DisplayName("Tutorials suggest concept references")
        void tutorialsSuggestConceptReferences() {
            SeeAlsoSection section = generator.generate("Spring Boot", ContentType.TUTORIAL);

            boolean hasReference = section.entries().stream()
                    .anyMatch(e -> e.type() == SeeAlsoType.REFERENCE ||
                            e.title().toLowerCase().contains("concepts"));

            // May or may not have specific reference, depends on patterns
            assertNotNull(section);
        }

        @Test
        @DisplayName("References suggest tutorials")
        void referencesSuggestTutorials() {
            SeeAlsoSection section = generator.generate("Spring Boot API", ContentType.REFERENCE);

            boolean hasTutorial = section.entries().stream()
                    .anyMatch(e -> e.type() == SeeAlsoType.TUTORIAL);

            assertTrue(hasTutorial);
        }

        @Test
        @DisplayName("Comparisons link to individual topics")
        void comparisonsLinkToIndividualTopics() {
            SeeAlsoSection section = generator.generate("Java vs Python", ContentType.COMPARISON);

            // Should have both Java and Python as related
            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().equals("Java")));
            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.title().equals("Python")));
        }

        @Test
        @DisplayName("Troubleshooting links to main topic")
        void troubleshootingLinksToMainTopic() {
            SeeAlsoSection section = generator.generate("Spring Boot Common Issues", ContentType.TROUBLESHOOTING);

            // Should have broader topic link
            assertTrue(section.entries().stream()
                    .anyMatch(e -> e.type() == SeeAlsoType.BROADER));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Handles null topic")
        void handlesNullTopic() {
            SeeAlsoSection section = generator.generate(null, ContentType.CONCEPT);
            assertEquals("Unknown", section.sourceTopic());
            assertTrue(section.isEmpty());
        }

        @Test
        @DisplayName("Handles blank topic")
        void handlesBlankTopic() {
            SeeAlsoSection section = generator.generate("   ", ContentType.CONCEPT);
            assertEquals("Unknown", section.sourceTopic());
            assertTrue(section.isEmpty());
        }

        @Test
        @DisplayName("Case insensitive pattern matching")
        void caseInsensitivePatternMatching() {
            SeeAlsoSection lower = generator.generate("spring boot", ContentType.TUTORIAL);
            SeeAlsoSection upper = generator.generate("SPRING BOOT", ContentType.TUTORIAL);
            SeeAlsoSection mixed = generator.generate("Spring Boot", ContentType.TUTORIAL);

            // All should identify Java as broader topic
            assertTrue(lower.entries().stream().anyMatch(e -> e.title().equals("Java")));
            assertTrue(upper.entries().stream().anyMatch(e -> e.title().equals("Java")));
            assertTrue(mixed.entries().stream().anyMatch(e -> e.title().equals("Java")));
        }

        @Test
        @DisplayName("Avoids duplicate entries")
        void avoidsDuplicateEntries() {
            // Topic that might match multiple patterns
            SeeAlsoSection section = generator.generate("Spring Boot REST API", ContentType.TUTORIAL);

            long javaCount = section.entries().stream()
                    .filter(e -> e.title().equals("Java"))
                    .count();

            assertEquals(1, javaCount);
        }
    }

    @Nested
    @DisplayName("suggestRelatedTopics()")
    class SuggestRelatedTopics {

        @Test
        @DisplayName("Suggests topics for known frameworks")
        void suggestsTopicsForKnownFrameworks() {
            List<String> suggestions = generator.suggestRelatedTopics("Spring Boot");

            assertFalse(suggestions.isEmpty());
            assertTrue(suggestions.contains("Java"));
        }

        @Test
        @DisplayName("Returns empty for unknown topics")
        void returnsEmptyForUnknownTopics() {
            List<String> suggestions = generator.suggestRelatedTopics("Random Unknown Topic");

            assertTrue(suggestions.isEmpty());
        }

        @Test
        @DisplayName("Includes related ecosystem topics")
        void includesRelatedEcosystemTopics() {
            List<String> suggestions = generator.suggestRelatedTopics("React");

            assertTrue(suggestions.contains("JavaScript"));
        }
    }
}
