package com.jakefear.aipublisher.prerequisites;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PrerequisiteSet record.
 */
@DisplayName("PrerequisiteSet")
class PrerequisiteSetTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid prerequisite set")
        void createsValidPrerequisiteSet() {
            List<Prerequisite> prereqs = List.of(
                    Prerequisite.hard("Java", "JavaBasics", "Foundation"),
                    Prerequisite.soft("Design Patterns", "Helpful")
            );

            PrerequisiteSet set = new PrerequisiteSet("Spring Boot", prereqs);

            assertEquals("Spring Boot", set.topic());
            assertEquals(2, set.size());
        }

        @Test
        @DisplayName("Rejects null topic")
        void rejectsNullTopic() {
            assertThrows(NullPointerException.class, () ->
                    new PrerequisiteSet(null, List.of()));
        }

        @Test
        @DisplayName("Rejects blank topic")
        void rejectsBlankTopic() {
            assertThrows(IllegalArgumentException.class, () ->
                    new PrerequisiteSet("  ", List.of()));
        }

        @Test
        @DisplayName("Handles null prerequisites list")
        void handlesNullPrerequisitesList() {
            PrerequisiteSet set = new PrerequisiteSet("Topic", null);
            assertNotNull(set.prerequisites());
            assertTrue(set.isEmpty());
        }
    }

    @Nested
    @DisplayName("Query methods")
    class QueryMethods {

        private PrerequisiteSet createTestSet() {
            return PrerequisiteSet.builder("Test Topic")
                    .addHard("Java", "JavaBasics", "Required")
                    .addHard("SQL", "SqlBasics", "Database access")
                    .addSoft("Design Patterns", "DesignPatterns", "Helpful")
                    .addAssumed("Basic programming")
                    .build();
        }

        @Test
        @DisplayName("getHardPrerequisites() returns only hard")
        void getHardPrerequisitesReturnsOnlyHard() {
            PrerequisiteSet set = createTestSet();

            List<Prerequisite> hard = set.getHardPrerequisites();

            assertEquals(2, hard.size());
            assertTrue(hard.stream().allMatch(p -> p.type() == PrerequisiteType.HARD));
        }

        @Test
        @DisplayName("getSoftPrerequisites() returns only soft")
        void getSoftPrerequisitesReturnsOnlySoft() {
            PrerequisiteSet set = createTestSet();

            List<Prerequisite> soft = set.getSoftPrerequisites();

            assertEquals(1, soft.size());
            assertTrue(soft.stream().allMatch(p -> p.type() == PrerequisiteType.SOFT));
        }

        @Test
        @DisplayName("getAssumedPrerequisites() returns only assumed")
        void getAssumedPrerequisitesReturnsOnlyAssumed() {
            PrerequisiteSet set = createTestSet();

            List<Prerequisite> assumed = set.getAssumedPrerequisites();

            assertEquals(1, assumed.size());
            assertTrue(assumed.stream().allMatch(p -> p.type() == PrerequisiteType.ASSUMED));
        }

        @Test
        @DisplayName("hasHardPrerequisites() returns correct value")
        void hasHardPrerequisitesReturnsCorrectValue() {
            PrerequisiteSet withHard = createTestSet();
            assertTrue(withHard.hasHardPrerequisites());

            PrerequisiteSet withoutHard = PrerequisiteSet.builder("Test")
                    .addSoft("Something", "Nice to know")
                    .build();
            assertFalse(withoutHard.hasHardPrerequisites());
        }

        @Test
        @DisplayName("isEmpty() returns correct value")
        void isEmptyReturnsCorrectValue() {
            PrerequisiteSet empty = PrerequisiteSet.empty("Topic");
            assertTrue(empty.isEmpty());

            PrerequisiteSet notEmpty = createTestSet();
            assertFalse(notEmpty.isEmpty());
        }
    }

    @Nested
    @DisplayName("toWikiCallout()")
    class ToWikiCallout {

        @Test
        @DisplayName("Returns empty string for empty set")
        void returnsEmptyStringForEmptySet() {
            PrerequisiteSet set = PrerequisiteSet.empty("Topic");
            assertEquals("", set.toWikiCallout());
        }

        @Test
        @DisplayName("Generates warning callout for hard prerequisites")
        void generatesWarningCalloutForHardPrerequisites() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addHard("Java", "JavaBasics", "Foundation")
                    .build();

            String callout = set.toWikiCallout();

            assertTrue(callout.contains("%%warning"));
            assertTrue(callout.contains("Before you begin"));
            assertTrue(callout.contains("Java")); // Topic name appears
            assertTrue(callout.contains("Foundation"));
        }

        @Test
        @DisplayName("Generates info callout for soft prerequisites")
        void generatesInfoCalloutForSoftPrerequisites() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addSoft("Design Patterns", "Helpful background")
                    .build();

            String callout = set.toWikiCallout();

            assertTrue(callout.contains("%%information"));
            assertTrue(callout.contains("Helpful background"));
        }

        @Test
        @DisplayName("Generates both callouts when mixed")
        void generatesBothCalloutsWhenMixed() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addHard("Java", "Required")
                    .addSoft("Patterns", "Helpful")
                    .build();

            String callout = set.toWikiCallout();

            assertTrue(callout.contains("%%warning"));
            assertTrue(callout.contains("%%information"));
        }

        @Test
        @DisplayName("Does not include assumed in callout")
        void doesNotIncludeAssumedInCallout() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addAssumed("Basic programming")
                    .build();

            String callout = set.toWikiCallout();

            // Assumed prerequisites don't generate callouts
            assertEquals("", callout);
        }
    }

    @Nested
    @DisplayName("toPromptFormat()")
    class ToPromptFormat {

        @Test
        @DisplayName("Returns default message for empty set")
        void returnsDefaultMessageForEmptySet() {
            PrerequisiteSet set = PrerequisiteSet.empty("Topic");
            assertTrue(set.toPromptFormat().contains("No specific prerequisites"));
        }

        @Test
        @DisplayName("Formats hard prerequisites with section header")
        void formatsHardPrerequisitesWithSectionHeader() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addHard("Java", "Foundation")
                    .build();

            String prompt = set.toPromptFormat();

            assertTrue(prompt.contains("Required Prerequisites"));
            assertTrue(prompt.contains("Java"));
            assertTrue(prompt.contains("Foundation"));
        }

        @Test
        @DisplayName("Formats soft prerequisites with section header")
        void formatsSoftPrerequisitesWithSectionHeader() {
            PrerequisiteSet set = PrerequisiteSet.builder("Topic")
                    .addSoft("Patterns", "Helpful")
                    .build();

            String prompt = set.toPromptFormat();

            assertTrue(prompt.contains("Recommended Background"));
            assertTrue(prompt.contains("Patterns"));
        }
    }

    @Nested
    @DisplayName("Factory methods and builder")
    class FactoryMethodsAndBuilder {

        @Test
        @DisplayName("empty() creates valid empty set")
        void emptyCreatesValidEmptySet() {
            PrerequisiteSet set = PrerequisiteSet.empty("My Topic");

            assertEquals("My Topic", set.topic());
            assertTrue(set.isEmpty());
        }

        @Test
        @DisplayName("Builder creates proper set")
        void builderCreatesProperSet() {
            PrerequisiteSet set = PrerequisiteSet.builder("Spring Boot")
                    .addHard("Java", "JavaBasics", "Foundation")
                    .addSoft("Maven", "Helpful")
                    .addAssumed("IDE usage")
                    .add(Prerequisite.hard("SQL", "SqlBasics", "Database"))
                    .build();

            assertEquals("Spring Boot", set.topic());
            assertEquals(4, set.size());
            assertEquals(2, set.getHardPrerequisites().size());
            assertEquals(1, set.getSoftPrerequisites().size());
            assertEquals(1, set.getAssumedPrerequisites().size());
        }
    }
}
