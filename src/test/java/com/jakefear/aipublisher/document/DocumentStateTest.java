package com.jakefear.aipublisher.document;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DocumentState")
class DocumentStateTest {

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("CREATED can transition to RESEARCHING")
        void createdCanTransitionToResearching() {
            assertTrue(DocumentState.CREATED.canTransitionTo(DocumentState.RESEARCHING));
        }

        @Test
        @DisplayName("CREATED can transition to REJECTED")
        void createdCanTransitionToRejected() {
            assertTrue(DocumentState.CREATED.canTransitionTo(DocumentState.REJECTED));
        }

        @Test
        @DisplayName("CREATED cannot transition to DRAFTING directly")
        void createdCannotTransitionToDrafting() {
            assertFalse(DocumentState.CREATED.canTransitionTo(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("RESEARCHING can transition to DRAFTING")
        void researchingCanTransitionToDrafting() {
            assertTrue(DocumentState.RESEARCHING.canTransitionTo(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("DRAFTING can transition to FACT_CHECKING")
        void draftingCanTransitionToFactChecking() {
            assertTrue(DocumentState.DRAFTING.canTransitionTo(DocumentState.FACT_CHECKING));
        }

        @Test
        @DisplayName("FACT_CHECKING can transition to EDITING")
        void factCheckingCanTransitionToEditing() {
            assertTrue(DocumentState.FACT_CHECKING.canTransitionTo(DocumentState.EDITING));
        }

        @Test
        @DisplayName("FACT_CHECKING can revert to DRAFTING for revision")
        void factCheckingCanRevertToDrafting() {
            assertTrue(DocumentState.FACT_CHECKING.canTransitionTo(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("EDITING can transition to CRITIQUING")
        void editingCanTransitionToCritiquing() {
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.CRITIQUING));
        }

        @Test
        @DisplayName("CRITIQUING can transition to PUBLISHED")
        void critiquingCanTransitionToPublished() {
            assertTrue(DocumentState.CRITIQUING.canTransitionTo(DocumentState.PUBLISHED));
        }

        @Test
        @DisplayName("EDITING can revert to FACT_CHECKING for revision")
        void editingCanRevertToFactChecking() {
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.FACT_CHECKING));
        }

        @Test
        @DisplayName("EDITING can revert to DRAFTING for major revision")
        void editingCanRevertToDrafting() {
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.DRAFTING));
        }

        @Test
        @DisplayName("Cannot transition to same state")
        void cannotTransitionToSameState() {
            for (DocumentState state : DocumentState.values()) {
                assertFalse(state.canTransitionTo(state),
                        "State " + state + " should not transition to itself");
            }
        }

        @Test
        @DisplayName("Cannot transition to null")
        void cannotTransitionToNull() {
            for (DocumentState state : DocumentState.values()) {
                assertFalse(state.canTransitionTo(null));
            }
        }
    }

    @Nested
    @DisplayName("Terminal States")
    class TerminalStates {

        @Test
        @DisplayName("PUBLISHED is terminal")
        void publishedIsTerminal() {
            assertTrue(DocumentState.PUBLISHED.isTerminal());
        }

        @Test
        @DisplayName("REJECTED is terminal")
        void rejectedIsTerminal() {
            assertTrue(DocumentState.REJECTED.isTerminal());
        }

        @Test
        @DisplayName("PUBLISHED cannot transition to any state")
        void publishedCannotTransition() {
            for (DocumentState target : DocumentState.values()) {
                assertFalse(DocumentState.PUBLISHED.canTransitionTo(target));
            }
        }

        @Test
        @DisplayName("REJECTED cannot transition to any state")
        void rejectedCannotTransition() {
            for (DocumentState target : DocumentState.values()) {
                assertFalse(DocumentState.REJECTED.canTransitionTo(target));
            }
        }

        @ParameterizedTest
        @EnumSource(value = DocumentState.class, names = {"CREATED", "RESEARCHING", "DRAFTING", "FACT_CHECKING", "EDITING", "AWAITING_APPROVAL"})
        @DisplayName("Non-terminal states are not terminal")
        void nonTerminalStatesAreNotTerminal(DocumentState state) {
            assertFalse(state.isTerminal());
        }
    }

    @Nested
    @DisplayName("Processing States")
    class ProcessingStates {

        @Test
        @DisplayName("RESEARCHING is a processing state")
        void researchingIsProcessing() {
            assertTrue(DocumentState.RESEARCHING.isProcessing());
        }

        @Test
        @DisplayName("DRAFTING is a processing state")
        void draftingIsProcessing() {
            assertTrue(DocumentState.DRAFTING.isProcessing());
        }

        @Test
        @DisplayName("FACT_CHECKING is a processing state")
        void factCheckingIsProcessing() {
            assertTrue(DocumentState.FACT_CHECKING.isProcessing());
        }

        @Test
        @DisplayName("EDITING is a processing state")
        void editingIsProcessing() {
            assertTrue(DocumentState.EDITING.isProcessing());
        }

        @Test
        @DisplayName("CREATED is not a processing state")
        void createdIsNotProcessing() {
            assertFalse(DocumentState.CREATED.isProcessing());
        }

        @Test
        @DisplayName("PUBLISHED is not a processing state")
        void publishedIsNotProcessing() {
            assertFalse(DocumentState.PUBLISHED.isProcessing());
        }
    }

    @Nested
    @DisplayName("Happy Path Flow")
    class HappyPathFlow {

        @Test
        @DisplayName("getNextInFlow returns correct sequence")
        void getNextInFlowReturnsCorrectSequence() {
            assertEquals(DocumentState.RESEARCHING, DocumentState.CREATED.getNextInFlow());
            assertEquals(DocumentState.DRAFTING, DocumentState.RESEARCHING.getNextInFlow());
            assertEquals(DocumentState.FACT_CHECKING, DocumentState.DRAFTING.getNextInFlow());
            assertEquals(DocumentState.EDITING, DocumentState.FACT_CHECKING.getNextInFlow());
            assertEquals(DocumentState.CRITIQUING, DocumentState.EDITING.getNextInFlow());
            assertEquals(DocumentState.PUBLISHED, DocumentState.CRITIQUING.getNextInFlow());
        }

        @Test
        @DisplayName("Terminal states have no next state")
        void terminalStatesHaveNoNextState() {
            assertNull(DocumentState.PUBLISHED.getNextInFlow());
            assertNull(DocumentState.REJECTED.getNextInFlow());
        }

        @Test
        @DisplayName("AWAITING_APPROVAL has no automatic next state")
        void awaitingApprovalHasNoNextState() {
            assertNull(DocumentState.AWAITING_APPROVAL.getNextInFlow());
        }
    }

    @Nested
    @DisplayName("Revision Flow")
    class RevisionFlow {

        @Test
        @DisplayName("FACT_CHECKING can revert to DRAFTING")
        void factCheckingRevertsToWriting() {
            assertEquals(DocumentState.DRAFTING, DocumentState.FACT_CHECKING.getPreviousForRevision());
        }

        @Test
        @DisplayName("EDITING can revert to FACT_CHECKING")
        void editingRevertsToFactChecking() {
            assertEquals(DocumentState.FACT_CHECKING, DocumentState.EDITING.getPreviousForRevision());
        }

        @Test
        @DisplayName("Early states have no previous for revision")
        void earlyStatesHaveNoPrevious() {
            assertNull(DocumentState.CREATED.getPreviousForRevision());
            assertNull(DocumentState.RESEARCHING.getPreviousForRevision());
            assertNull(DocumentState.DRAFTING.getPreviousForRevision());
        }
    }

    @Nested
    @DisplayName("Valid Transitions Set")
    class ValidTransitionsSet {

        @Test
        @DisplayName("CREATED has correct valid transitions")
        void createdValidTransitions() {
            Set<DocumentState> valid = DocumentState.CREATED.getValidTransitions();
            assertEquals(2, valid.size());
            assertTrue(valid.contains(DocumentState.RESEARCHING));
            assertTrue(valid.contains(DocumentState.REJECTED));
        }

        @Test
        @DisplayName("Terminal states have empty valid transitions")
        void terminalStatesHaveEmptyTransitions() {
            assertTrue(DocumentState.PUBLISHED.getValidTransitions().isEmpty());
            assertTrue(DocumentState.REJECTED.getValidTransitions().isEmpty());
        }

        @Test
        @DisplayName("AWAITING_APPROVAL can go to multiple states")
        void awaitingApprovalHasMultipleTransitions() {
            Set<DocumentState> valid = DocumentState.AWAITING_APPROVAL.getValidTransitions();
            assertTrue(valid.size() >= 4);
            assertTrue(valid.contains(DocumentState.PUBLISHED));
            assertTrue(valid.contains(DocumentState.REJECTED));
        }
    }

    @Nested
    @DisplayName("Skip Phase Transitions (--pipeline.skip-* flags)")
    class SkipPhaseTransitions {

        @Test
        @DisplayName("DRAFTING can transition directly to EDITING (skip fact-check)")
        void draftingCanTransitionToEditingSkippingFactCheck() {
            // This transition is needed when --pipeline.skip-fact-check=true
            assertTrue(DocumentState.DRAFTING.canTransitionTo(DocumentState.EDITING),
                    "DRAFTING should be able to transition to EDITING when fact-check is skipped");
        }

        @Test
        @DisplayName("EDITING can transition directly to PUBLISHED (skip critique)")
        void editingCanTransitionToPublishedSkippingCritique() {
            // This transition is needed when --pipeline.skip-critique=true
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.PUBLISHED),
                    "EDITING should be able to transition to PUBLISHED when critique is skipped");
        }

        @Test
        @DisplayName("DRAFTING valid transitions include EDITING for skip scenario")
        void draftingValidTransitionsIncludeEditing() {
            Set<DocumentState> valid = DocumentState.DRAFTING.getValidTransitions();
            assertTrue(valid.contains(DocumentState.EDITING),
                    "DRAFTING valid transitions should include EDITING for skip-fact-check");
            assertTrue(valid.contains(DocumentState.FACT_CHECKING),
                    "DRAFTING should still support normal flow to FACT_CHECKING");
        }

        @Test
        @DisplayName("EDITING valid transitions include PUBLISHED for skip scenario")
        void editingValidTransitionsIncludePublished() {
            Set<DocumentState> valid = DocumentState.EDITING.getValidTransitions();
            assertTrue(valid.contains(DocumentState.PUBLISHED),
                    "EDITING valid transitions should include PUBLISHED for skip-critique");
            assertTrue(valid.contains(DocumentState.CRITIQUING),
                    "EDITING should still support normal flow to CRITIQUING");
        }

        @Test
        @DisplayName("Full skip path: DRAFTING -> EDITING -> PUBLISHED works")
        void fullSkipPathWorks() {
            // Simulates --pipeline.skip-fact-check=true --pipeline.skip-critique=true
            assertTrue(DocumentState.DRAFTING.canTransitionTo(DocumentState.EDITING));
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.PUBLISHED));
        }

        @Test
        @DisplayName("Skip fact-check but run critique: DRAFTING -> EDITING -> CRITIQUING -> PUBLISHED")
        void skipFactCheckButRunCritique() {
            assertTrue(DocumentState.DRAFTING.canTransitionTo(DocumentState.EDITING));
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.CRITIQUING));
            assertTrue(DocumentState.CRITIQUING.canTransitionTo(DocumentState.PUBLISHED));
        }

        @Test
        @DisplayName("Run fact-check but skip critique: DRAFTING -> FACT_CHECKING -> EDITING -> PUBLISHED")
        void runFactCheckButSkipCritique() {
            assertTrue(DocumentState.DRAFTING.canTransitionTo(DocumentState.FACT_CHECKING));
            assertTrue(DocumentState.FACT_CHECKING.canTransitionTo(DocumentState.EDITING));
            assertTrue(DocumentState.EDITING.canTransitionTo(DocumentState.PUBLISHED));
        }
    }
}
