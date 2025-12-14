package com.jakefear.aipublisher.cli.curation.relationship;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.RelationshipSuggestion;
import com.jakefear.aipublisher.domain.RelationshipType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for relationship curation commands.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Relationship Curation Commands")
class RelationshipCurationCommandsTest {

    @Mock
    private DiscoverySession session;

    @Mock
    private ConsoleInputHelper input;

    private RelationshipSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new RelationshipSuggestion(
                "Topic A",
                "Topic B",
                RelationshipType.PREREQUISITE_OF,
                0.85,
                "A should be learned before B"
        );
    }

    @Nested
    @DisplayName("ConfirmRelationshipCommand")
    class ConfirmRelationshipCommandTest {

        @Test
        @DisplayName("Confirms suggestion and returns success")
        void confirmsAndReturnsSuccess() throws Exception {
            ConfirmRelationshipCommand command = new ConfirmRelationshipCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).confirmRelationship(suggestion);
            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Confirmed", result.message());
            assertTrue(result.shouldContinue());
        }

        @Test
        @DisplayName("Returns CONFIRM action")
        void returnsConfirmAction() {
            ConfirmRelationshipCommand command = new ConfirmRelationshipCommand();
            assertEquals(CurationAction.CONFIRM, command.getAction());
        }
    }

    @Nested
    @DisplayName("RejectRelationshipCommand")
    class RejectRelationshipCommandTest {

        @Test
        @DisplayName("Rejects suggestion and returns success")
        void rejectsAndReturnsSuccess() throws Exception {
            RejectRelationshipCommand command = new RejectRelationshipCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).rejectRelationship(suggestion);
            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Rejected", result.message());
        }

        @Test
        @DisplayName("Returns REJECT action")
        void returnsRejectAction() {
            RejectRelationshipCommand command = new RejectRelationshipCommand();
            assertEquals(CurationAction.REJECT, command.getAction());
        }
    }

    @Nested
    @DisplayName("ChangeTypeRelationshipCommand")
    class ChangeTypeRelationshipCommandTest {

        @Test
        @DisplayName("Changes type and confirms relationship")
        void changesTypeAndConfirms() throws Exception {
            List<String> typeNames = java.util.Arrays.stream(RelationshipType.values())
                    .map(RelationshipType::getDisplayName)
                    .toList();

            // User selects index 1 (RELATED)
            when(input.promptSelection(eq("Selection"), eq(typeNames), anyInt()))
                    .thenReturn(InputResponse.value("1"));

            ChangeTypeRelationshipCommand command = new ChangeTypeRelationshipCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).confirmRelationshipAs(eq(suggestion), eq(RelationshipType.values()[1]));
            assertEquals(CurationResult.Outcome.MODIFIED, result.outcome());
            assertTrue(result.message().contains("Confirmed as"));
        }

        @Test
        @DisplayName("Uses current type when quit during selection")
        void usesCurrentTypeOnQuit() throws Exception {
            when(input.promptSelection(anyString(), anyList(), anyInt()))
                    .thenReturn(InputResponse.quit());

            ChangeTypeRelationshipCommand command = new ChangeTypeRelationshipCommand();

            command.execute(suggestion, session, input);

            verify(session).confirmRelationshipAs(eq(suggestion), eq(RelationshipType.PREREQUISITE_OF));
        }

        @Test
        @DisplayName("Uses current type for invalid selection")
        void usesCurrentTypeForInvalidSelection() throws Exception {
            when(input.promptSelection(anyString(), anyList(), anyInt()))
                    .thenReturn(InputResponse.value("invalid"));

            ChangeTypeRelationshipCommand command = new ChangeTypeRelationshipCommand();

            command.execute(suggestion, session, input);

            verify(session).confirmRelationshipAs(eq(suggestion), eq(RelationshipType.PREREQUISITE_OF));
        }

        @Test
        @DisplayName("Returns TYPE_CHANGE action")
        void returnsTypeChangeAction() {
            ChangeTypeRelationshipCommand command = new ChangeTypeRelationshipCommand();
            assertEquals(CurationAction.TYPE_CHANGE, command.getAction());
        }
    }

    @Nested
    @DisplayName("RelationshipCurationCommandFactory")
    class RelationshipCurationCommandFactoryTest {

        private RelationshipCurationCommandFactory factory;

        @BeforeEach
        void setUp() {
            factory = new RelationshipCurationCommandFactory();
        }

        @Test
        @DisplayName("Returns ConfirmRelationshipCommand for CONFIRM")
        void returnsConfirmCommand() {
            var command = factory.getCommand(CurationAction.CONFIRM);
            assertInstanceOf(ConfirmRelationshipCommand.class, command);
        }

        @Test
        @DisplayName("Returns ConfirmRelationshipCommand for DEFAULT")
        void returnsConfirmCommandForDefault() {
            var command = factory.getCommand(CurationAction.DEFAULT);
            assertInstanceOf(ConfirmRelationshipCommand.class, command);
        }

        @Test
        @DisplayName("Returns RejectRelationshipCommand for REJECT")
        void returnsRejectCommand() {
            var command = factory.getCommand(CurationAction.REJECT);
            assertInstanceOf(RejectRelationshipCommand.class, command);
        }

        @Test
        @DisplayName("Returns ChangeTypeRelationshipCommand for TYPE_CHANGE")
        void returnsChangeTypeCommand() {
            var command = factory.getCommand(CurationAction.TYPE_CHANGE);
            assertInstanceOf(ChangeTypeRelationshipCommand.class, command);
        }

        @Test
        @DisplayName("Returns null for QUIT")
        void returnsNullForQuit() {
            var command = factory.getCommand(CurationAction.QUIT);
            assertNull(command);
        }

        @Test
        @DisplayName("Returns null for topic-specific actions")
        void returnsNullForTopicActions() {
            assertNull(factory.getCommand(CurationAction.ACCEPT));
            assertNull(factory.getCommand(CurationAction.DEFER));
            assertNull(factory.getCommand(CurationAction.MODIFY));
            assertNull(factory.getCommand(CurationAction.SKIP_REST));
        }

        @Test
        @DisplayName("Returns menu prompt")
        void returnsMenuPrompt() {
            String prompt = factory.getMenuPrompt();
            assertTrue(prompt.contains("[C]onfirm"));
            assertTrue(prompt.contains("[R]eject"));
            assertTrue(prompt.contains("[T]ype change"));
        }
    }
}
