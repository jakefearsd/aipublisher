package com.jakefear.aipublisher.seealso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SeeAlsoType enum.
 */
@DisplayName("SeeAlsoType")
class SeeAlsoTypeTest {

    @Test
    @DisplayName("Main section types are identified correctly")
    void mainSectionTypesIdentifiedCorrectly() {
        assertTrue(SeeAlsoType.BROADER.isMainSection());
        assertTrue(SeeAlsoType.NARROWER.isMainSection());
        assertTrue(SeeAlsoType.RELATED.isMainSection());
        assertFalse(SeeAlsoType.TUTORIAL.isMainSection());
        assertFalse(SeeAlsoType.REFERENCE.isMainSection());
        assertFalse(SeeAlsoType.COMPARISON.isMainSection());
        assertFalse(SeeAlsoType.EXTERNAL.isMainSection());
    }

    @Test
    @DisplayName("Internal types are identified correctly")
    void internalTypesIdentifiedCorrectly() {
        assertTrue(SeeAlsoType.BROADER.isInternal());
        assertTrue(SeeAlsoType.NARROWER.isInternal());
        assertTrue(SeeAlsoType.RELATED.isInternal());
        assertTrue(SeeAlsoType.TUTORIAL.isInternal());
        assertTrue(SeeAlsoType.REFERENCE.isInternal());
        assertTrue(SeeAlsoType.COMPARISON.isInternal());
        assertFalse(SeeAlsoType.EXTERNAL.isInternal());
    }

    @Test
    @DisplayName("All types have display names and descriptions")
    void allTypesHaveDisplayNamesAndDescriptions() {
        for (SeeAlsoType type : SeeAlsoType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isBlank());
            assertNotNull(type.getDescription());
            assertFalse(type.getDescription().isBlank());
        }
    }

    @Test
    @DisplayName("All types have section headings")
    void allTypesHaveSectionHeadings() {
        for (SeeAlsoType type : SeeAlsoType.values()) {
            assertNotNull(type.getSectionHeading());
            assertFalse(type.getSectionHeading().isBlank());
        }
    }

    @Test
    @DisplayName("Section headings are appropriately named")
    void sectionHeadingsAppropriatelyNamed() {
        assertEquals("Broader Topics", SeeAlsoType.BROADER.getSectionHeading());
        assertEquals("Subtopics", SeeAlsoType.NARROWER.getSectionHeading());
        assertEquals("Related Topics", SeeAlsoType.RELATED.getSectionHeading());
        assertEquals("External Links", SeeAlsoType.EXTERNAL.getSectionHeading());
    }
}
