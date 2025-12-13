package com.jakefear.aipublisher.content;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ContentTypeTemplate")
class ContentTypeTemplateTest {

    @Nested
    @DisplayName("forType")
    class ForType {

        @Test
        @DisplayName("Returns template for each content type")
        void returnsTemplateForEachType() {
            for (ContentType type : ContentType.values()) {
                ContentTypeTemplate template = ContentTypeTemplate.forType(type);
                assertNotNull(template, "Template should exist for " + type);
                assertEquals(type, template.contentType());
            }
        }

        @Test
        @DisplayName("Templates have required sections")
        void templatesHaveRequiredSections() {
            for (ContentType type : ContentType.values()) {
                ContentTypeTemplate template = ContentTypeTemplate.forType(type);
                assertFalse(template.requiredSections().isEmpty(),
                    type + " should have at least one required section");
            }
        }

        @Test
        @DisplayName("Tutorial template has Goal, Prerequisites, Steps, Verification")
        void tutorialTemplateHasExpectedSections() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TUTORIAL);

            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Goal")));
            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Prerequisites")));
            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Steps")));
            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Verification")));
        }

        @Test
        @DisplayName("Comparison template has Summary Table")
        void comparisonTemplateHasSummaryTable() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.COMPARISON);

            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Summary Table")));
        }

        @Test
        @DisplayName("Troubleshooting template has Symptoms and Solutions")
        void troubleshootingTemplateHasSymptomsAndSolutions() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TROUBLESHOOTING);

            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Symptoms")));
            assertTrue(template.requiredSections().stream()
                .anyMatch(s -> s.name().equals("Solutions")));
        }
    }

    @Nested
    @DisplayName("toWriterGuidance")
    class ToWriterGuidance {

        @Test
        @DisplayName("Generates guidance with content type name")
        void generatesGuidanceWithTypeName() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.CONCEPT);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("CONTENT TYPE: Concept"));
        }

        @Test
        @DisplayName("Generates guidance with purpose")
        void generatesGuidanceWithPurpose() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TUTORIAL);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("PURPOSE:"));
        }

        @Test
        @DisplayName("Generates guidance with required structure")
        void generatesGuidanceWithRequiredStructure() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.CONCEPT);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("REQUIRED STRUCTURE:"));
            assertTrue(guidance.contains("Definition"));
            assertTrue(guidance.contains("Context"));
        }

        @Test
        @DisplayName("Generates guidance with optional sections when present")
        void generatesGuidanceWithOptionalSections() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.CONCEPT);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("OPTIONAL SECTIONS:"));
            assertTrue(guidance.contains("History"));
        }

        @Test
        @DisplayName("Generates guidance with structure guidance")
        void generatesGuidanceWithStructureGuidance() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.COMPARISON);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("STRUCTURE GUIDANCE:"));
        }

        @Test
        @DisplayName("Generates guidance with tone guidance")
        void generatesGuidanceWithToneGuidance() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TROUBLESHOOTING);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("TONE:"));
        }

        @Test
        @DisplayName("Includes example requirement for types that need examples")
        void includesExampleRequirementForTypesNeedingExamples() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TUTORIAL);
            String guidance = template.toWriterGuidance();

            assertTrue(guidance.contains("EXAMPLES:"));
        }
    }

    @Nested
    @DisplayName("SectionDefinition")
    class SectionDefinitionTests {

        @Test
        @DisplayName("Section definitions have required properties")
        void sectionDefinitionsHaveRequiredProperties() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.CONCEPT);

            for (ContentTypeTemplate.SectionDefinition section : template.requiredSections()) {
                assertNotNull(section.name());
                assertFalse(section.name().isBlank());
                assertNotNull(section.description());
                assertFalse(section.description().isBlank());
                assertTrue(section.required());
            }
        }

        @Test
        @DisplayName("Optional sections are marked as not required")
        void optionalSectionsAreNotRequired() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.CONCEPT);

            for (ContentTypeTemplate.SectionDefinition section : template.optionalSections()) {
                assertFalse(section.required());
            }
        }

        @Test
        @DisplayName("Required sections have order numbers")
        void requiredSectionsHaveOrderNumbers() {
            ContentTypeTemplate template = ContentTypeTemplate.forType(ContentType.TUTORIAL);

            for (ContentTypeTemplate.SectionDefinition section : template.requiredSections()) {
                assertTrue(section.order() >= 1);
            }
        }
    }
}
