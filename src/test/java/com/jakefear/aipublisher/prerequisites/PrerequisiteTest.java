package com.jakefear.aipublisher.prerequisites;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Prerequisite record.
 */
@DisplayName("Prerequisite")
class PrerequisiteTest {

    @Nested
    @DisplayName("Constructor validation")
    class ConstructorValidation {

        @Test
        @DisplayName("Creates valid prerequisite")
        void createsValidPrerequisite() {
            Prerequisite prereq = new Prerequisite(
                    "Java",
                    PrerequisiteType.HARD,
                    "JavaBasics",
                    "Required for Spring"
            );

            assertEquals("Java", prereq.topic());
            assertEquals(PrerequisiteType.HARD, prereq.type());
            assertEquals("JavaBasics", prereq.wikiPage());
            assertEquals("Required for Spring", prereq.reason());
        }

        @Test
        @DisplayName("Rejects null topic")
        void rejectsNullTopic() {
            assertThrows(NullPointerException.class, () ->
                    new Prerequisite(null, PrerequisiteType.HARD, null, ""));
        }

        @Test
        @DisplayName("Rejects blank topic")
        void rejectsBlankTopic() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Prerequisite("  ", PrerequisiteType.HARD, null, ""));
        }

        @Test
        @DisplayName("Rejects null type")
        void rejectsNullType() {
            assertThrows(NullPointerException.class, () ->
                    new Prerequisite("Java", null, null, ""));
        }

        @Test
        @DisplayName("Handles null reason")
        void handlesNullReason() {
            Prerequisite prereq = new Prerequisite("Java", PrerequisiteType.HARD, null, null);
            assertEquals("", prereq.reason());
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("hard() with wiki page creates hard prerequisite")
        void hardWithWikiPageCreatesHardPrerequisite() {
            Prerequisite prereq = Prerequisite.hard("Java", "JavaBasics", "Foundation");

            assertEquals("Java", prereq.topic());
            assertEquals(PrerequisiteType.HARD, prereq.type());
            assertEquals("JavaBasics", prereq.wikiPage());
            assertEquals("Foundation", prereq.reason());
        }

        @Test
        @DisplayName("hard() without wiki page creates hard prerequisite")
        void hardWithoutWikiPageCreatesHardPrerequisite() {
            Prerequisite prereq = Prerequisite.hard("Java", "Foundation");

            assertEquals(PrerequisiteType.HARD, prereq.type());
            assertNull(prereq.wikiPage());
        }

        @Test
        @DisplayName("soft() with wiki page creates soft prerequisite")
        void softWithWikiPageCreatesSoftPrerequisite() {
            Prerequisite prereq = Prerequisite.soft("Design Patterns", "DesignPatterns", "Helpful");

            assertEquals(PrerequisiteType.SOFT, prereq.type());
            assertEquals("DesignPatterns", prereq.wikiPage());
        }

        @Test
        @DisplayName("soft() without wiki page creates soft prerequisite")
        void softWithoutWikiPageCreatesSoftPrerequisite() {
            Prerequisite prereq = Prerequisite.soft("Design Patterns", "Helpful");

            assertEquals(PrerequisiteType.SOFT, prereq.type());
            assertNull(prereq.wikiPage());
        }

        @Test
        @DisplayName("assumed() creates assumed prerequisite")
        void assumedCreatesAssumedPrerequisite() {
            Prerequisite prereq = Prerequisite.assumed("Basic programming");

            assertEquals("Basic programming", prereq.topic());
            assertEquals(PrerequisiteType.ASSUMED, prereq.type());
            assertNull(prereq.wikiPage());
            assertEquals("", prereq.reason());
        }
    }

    @Nested
    @DisplayName("hasWikiPage()")
    class HasWikiPage {

        @Test
        @DisplayName("Returns true when wiki page exists")
        void returnsTrueWhenWikiPageExists() {
            Prerequisite prereq = Prerequisite.hard("Java", "JavaBasics", "Foundation");
            assertTrue(prereq.hasWikiPage());
        }

        @Test
        @DisplayName("Returns false when wiki page is null")
        void returnsFalseWhenWikiPageIsNull() {
            Prerequisite prereq = Prerequisite.hard("Java", "Foundation");
            assertFalse(prereq.hasWikiPage());
        }

        @Test
        @DisplayName("Returns false when wiki page is blank")
        void returnsFalseWhenWikiPageIsBlank() {
            Prerequisite prereq = new Prerequisite("Java", PrerequisiteType.HARD, "  ", "");
            assertFalse(prereq.hasWikiPage());
        }
    }

    @Nested
    @DisplayName("toWikiText()")
    class ToWikiText {

        @Test
        @DisplayName("Generates simple link when topic matches wiki page")
        void generatesSimpleLinkWhenTopicMatchesWikiPage() {
            Prerequisite prereq = new Prerequisite("Java Basics", PrerequisiteType.HARD, "JavaBasics", "");
            assertEquals("[JavaBasics]", prereq.toWikiText());
        }

        @Test
        @DisplayName("Generates piped link when topic differs from wiki page")
        void generatesPipedLinkWhenTopicDiffersFromWikiPage() {
            Prerequisite prereq = Prerequisite.hard("Java Programming", "JavaBasics", "Foundation");
            assertEquals("[Java Programming|JavaBasics]", prereq.toWikiText());
        }

        @Test
        @DisplayName("Returns plain topic when no wiki page")
        void returnsPlainTopicWhenNoWikiPage() {
            Prerequisite prereq = Prerequisite.hard("Advanced Calculus", "Math foundation");
            assertEquals("Advanced Calculus", prereq.toWikiText());
        }
    }
}
