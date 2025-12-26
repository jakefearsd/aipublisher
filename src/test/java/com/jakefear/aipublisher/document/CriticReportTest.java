package com.jakefear.aipublisher.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CriticReport")
class CriticReportTest {

    @Nested
    @DisplayName("Approval Status")
    class ApprovalStatus {

        @Test
        @DisplayName("isApproved returns true for APPROVE action")
        void isApprovedForApproveAction() {
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertTrue(report.isApproved());
            assertFalse(report.needsRevision());
            assertFalse(report.needsRework());
        }

        @Test
        @DisplayName("needsRevision returns true for REVISE action")
        void needsRevisionForReviseAction() {
            CriticReport report = new CriticReport(
                    0.75, 0.8, 0.7, 0.75,
                    List.of("Issue"), List.of(), List.of(), List.of(),
                    RecommendedAction.REVISE
            );

            assertFalse(report.isApproved());
            assertTrue(report.needsRevision());
            assertFalse(report.needsRework());
        }

        @Test
        @DisplayName("needsRework returns true for REJECT action")
        void needsReworkForRejectAction() {
            CriticReport report = new CriticReport(
                    0.5, 0.5, 0.3, 0.5,
                    List.of(), List.of("Major syntax issue"), List.of(), List.of(),
                    RecommendedAction.REJECT
            );

            assertFalse(report.isApproved());
            assertFalse(report.needsRevision());
            assertTrue(report.needsRework());
        }
    }

    @Nested
    @DisplayName("Quality Threshold")
    class QualityThreshold {

        @Test
        @DisplayName("meetsQualityThreshold returns true when score equals threshold")
        void meetsThresholdWhenEqual() {
            CriticReport report = new CriticReport(
                    0.8, 0.8, 0.8, 0.8,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertTrue(report.meetsQualityThreshold(0.8));
        }

        @Test
        @DisplayName("meetsQualityThreshold returns true when score exceeds threshold")
        void meetsThresholdWhenExceeds() {
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertTrue(report.meetsQualityThreshold(0.8));
        }

        @Test
        @DisplayName("meetsQualityThreshold returns false when score below threshold")
        void failsThresholdWhenBelow() {
            CriticReport report = new CriticReport(
                    0.7, 0.7, 0.7, 0.7,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.REVISE
            );

            assertFalse(report.meetsQualityThreshold(0.8));
        }
    }

    @Nested
    @DisplayName("Syntax Issues Detection")
    class SyntaxIssuesDetection {

        @Test
        @DisplayName("hasSyntaxIssues returns true when syntax score below 0.8")
        void hasSyntaxIssuesWhenLowScore() {
            CriticReport report = new CriticReport(
                    0.75, 0.8, 0.75, 0.8,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.REVISE
            );

            assertTrue(report.hasSyntaxIssues());
        }

        @Test
        @DisplayName("hasSyntaxIssues returns true when syntax issues list not empty")
        void hasSyntaxIssuesWhenIssuesExist() {
            CriticReport report = new CriticReport(
                    0.85, 0.9, 0.85, 0.85,
                    List.of(), List.of("Uses **bold** instead of __bold__"), List.of(), List.of(),
                    RecommendedAction.REVISE
            );

            assertTrue(report.hasSyntaxIssues());
        }

        @Test
        @DisplayName("hasSyntaxIssues returns false when score >= 0.8 and no issues")
        void noSyntaxIssuesWhenClean() {
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertFalse(report.hasSyntaxIssues());
        }
    }

    @Nested
    @DisplayName("Primarily Syntax Issues Detection")
    class PrimarilySyntaxIssuesDetection {

        @Test
        @DisplayName("hasPrimarilySyntaxIssues returns true when only syntax issues exist")
        void returnsTrueForOnlySyntaxIssues() {
            CriticReport report = new CriticReport(
                    0.75, 0.9, 0.7, 0.85,
                    List.of(),  // No structure issues
                    List.of("Uses **bold** instead of __bold__"),
                    List.of(),  // No style issues
                    List.of(),
                    RecommendedAction.REVISE
            );

            assertTrue(report.hasPrimarilySyntaxIssues());
        }

        @Test
        @DisplayName("hasPrimarilySyntaxIssues returns true with 1 structure issue")
        void returnsTrueWithOneStructureIssue() {
            CriticReport report = new CriticReport(
                    0.75, 0.85, 0.7, 0.85,
                    List.of("Minor structure issue"),  // 1 structure issue is OK
                    List.of("Uses **bold**"),
                    List.of(),
                    List.of(),
                    RecommendedAction.REVISE
            );

            assertTrue(report.hasPrimarilySyntaxIssues());
        }

        @Test
        @DisplayName("hasPrimarilySyntaxIssues returns false when too many structure issues")
        void returnsFalseWithManyStructureIssues() {
            CriticReport report = new CriticReport(
                    0.65, 0.6, 0.7, 0.7,
                    List.of("Missing intro", "Poor organization"),  // >1 structure issue
                    List.of("Uses **bold**"),
                    List.of(),
                    List.of(),
                    RecommendedAction.REVISE
            );

            assertFalse(report.hasPrimarilySyntaxIssues());
        }

        @Test
        @DisplayName("hasPrimarilySyntaxIssues returns false when too many style issues")
        void returnsFalseWithManyStyleIssues() {
            CriticReport report = new CriticReport(
                    0.7, 0.85, 0.7, 0.65,
                    List.of(),
                    List.of("Uses # heading"),
                    List.of("Passive voice", "Long sentences", "Jargon"),  // >2 style issues
                    List.of(),
                    RecommendedAction.REVISE
            );

            assertFalse(report.hasPrimarilySyntaxIssues());
        }

        @Test
        @DisplayName("hasPrimarilySyntaxIssues returns false when no syntax issues")
        void returnsFalseWhenNoSyntaxIssues() {
            CriticReport report = new CriticReport(
                    0.75, 0.7, 0.9, 0.75,
                    List.of("Structure issue"),
                    List.of(),  // No syntax issues
                    List.of(),
                    List.of(),
                    RecommendedAction.REVISE
            );

            assertFalse(report.hasPrimarilySyntaxIssues());
        }
    }

    @Nested
    @DisplayName("Issue Summary")
    class IssueSummary {

        @Test
        @DisplayName("getIssueSummary counts all issue types")
        void countsAllIssueTypes() {
            CriticReport report = new CriticReport(
                    0.7, 0.7, 0.6, 0.7,
                    List.of("Structure issue 1", "Structure issue 2"),
                    List.of("Syntax issue 1", "Syntax issue 2", "Syntax issue 3"),
                    List.of("Style issue 1"),
                    List.of("Suggestion"),
                    RecommendedAction.REVISE
            );

            String summary = report.getIssueSummary();

            assertTrue(summary.contains("6 issues found"));
            assertTrue(summary.contains("structure: 2"));
            assertTrue(summary.contains("syntax: 3"));
            assertTrue(summary.contains("style: 1"));
        }

        @Test
        @DisplayName("getIssueSummary handles zero issues")
        void handlesZeroIssues() {
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            String summary = report.getIssueSummary();

            assertTrue(summary.contains("0 issues found"));
            assertTrue(summary.contains("structure: 0"));
            assertTrue(summary.contains("syntax: 0"));
            assertTrue(summary.contains("style: 0"));
        }
    }

    @Nested
    @DisplayName("Record Accessors")
    class RecordAccessors {

        @Test
        @DisplayName("All scores are accessible")
        void allScoresAccessible() {
            CriticReport report = new CriticReport(
                    0.85, 0.9, 0.8, 0.88,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertEquals(0.85, report.overallScore());
            assertEquals(0.9, report.structureScore());
            assertEquals(0.8, report.syntaxScore());
            assertEquals(0.88, report.readabilityScore());
        }

        @Test
        @DisplayName("All issue lists are accessible")
        void allIssueListsAccessible() {
            List<String> structureIssues = List.of("Structure issue");
            List<String> syntaxIssues = List.of("Syntax issue");
            List<String> styleIssues = List.of("Style issue");
            List<String> suggestions = List.of("Suggestion");

            CriticReport report = new CriticReport(
                    0.7, 0.7, 0.7, 0.7,
                    structureIssues, syntaxIssues, styleIssues, suggestions,
                    RecommendedAction.REVISE
            );

            assertEquals(structureIssues, report.structureIssues());
            assertEquals(syntaxIssues, report.syntaxIssues());
            assertEquals(styleIssues, report.styleIssues());
            assertEquals(suggestions, report.suggestions());
        }

        @Test
        @DisplayName("RecommendedAction is accessible")
        void recommendedActionAccessible() {
            CriticReport report = new CriticReport(
                    0.9, 0.9, 0.9, 0.9,
                    List.of(), List.of(), List.of(), List.of(),
                    RecommendedAction.APPROVE
            );

            assertEquals(RecommendedAction.APPROVE, report.recommendedAction());
        }
    }
}
