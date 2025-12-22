package com.jakefear.aipublisher.cli.curation.topic;

import com.jakefear.aipublisher.cli.curation.CurationAction;
import com.jakefear.aipublisher.cli.curation.CurationResult;
import com.jakefear.aipublisher.cli.input.ConsoleInputHelper;
import com.jakefear.aipublisher.cli.input.InputResponse;
import com.jakefear.aipublisher.content.ContentType;
import com.jakefear.aipublisher.discovery.DiscoverySession;
import com.jakefear.aipublisher.discovery.TopicSuggestion;
import com.jakefear.aipublisher.domain.ComplexityLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for topic curation commands.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Topic Curation Commands")
class TopicCurationCommandsTest {

    @Mock
    private DiscoverySession session;

    @Mock
    private ConsoleInputHelper input;

    private TopicSuggestion suggestion;

    @BeforeEach
    void setUp() {
        suggestion = new TopicSuggestion(
                "Test Topic",
                "Test description",
                "Category",
                ContentType.TUTORIAL,
                ComplexityLevel.INTERMEDIATE,
                1000,
                0.8,
                "Test rationale",
                "",
                -1.0
        );
    }

    @Nested
    @DisplayName("AcceptTopicCommand")
    class AcceptTopicCommandTest {

        @Test
        @DisplayName("Accepts suggestion and returns success")
        void acceptsAndReturnsSuccess() throws Exception {
            AcceptTopicCommand command = new AcceptTopicCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).acceptTopicSuggestion(suggestion);
            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Accepted", result.message());
            assertTrue(result.shouldContinue());
        }

        @Test
        @DisplayName("Returns ACCEPT action")
        void returnsAcceptAction() {
            AcceptTopicCommand command = new AcceptTopicCommand();
            assertEquals(CurationAction.ACCEPT, command.getAction());
        }
    }

    @Nested
    @DisplayName("RejectTopicCommand")
    class RejectTopicCommandTest {

        @Test
        @DisplayName("Rejects suggestion and returns success")
        void rejectsAndReturnsSuccess() throws Exception {
            RejectTopicCommand command = new RejectTopicCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).rejectTopicSuggestion(suggestion);
            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Rejected", result.message());
        }

        @Test
        @DisplayName("Returns REJECT action")
        void returnsRejectAction() {
            RejectTopicCommand command = new RejectTopicCommand();
            assertEquals(CurationAction.REJECT, command.getAction());
        }
    }

    @Nested
    @DisplayName("DeferTopicCommand")
    class DeferTopicCommandTest {

        @Test
        @DisplayName("Defers suggestion and returns success")
        void defersAndReturnsSuccess() throws Exception {
            DeferTopicCommand command = new DeferTopicCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).deferTopicSuggestion(suggestion);
            assertEquals(CurationResult.Outcome.SUCCESS, result.outcome());
            assertEquals("Deferred to backlog", result.message());
        }

        @Test
        @DisplayName("Returns DEFER action")
        void returnsDeferAction() {
            DeferTopicCommand command = new DeferTopicCommand();
            assertEquals(CurationAction.DEFER, command.getAction());
        }
    }

    @Nested
    @DisplayName("ModifyTopicCommand")
    class ModifyTopicCommandTest {

        @Test
        @DisplayName("Modifies and accepts with new values")
        void modifiesAndAccepts() throws Exception {
            when(input.promptOptional(eq("  New name"), any()))
                    .thenReturn(InputResponse.value("New Name"));
            when(input.promptOptional(eq("  New description"), any()))
                    .thenReturn(InputResponse.value("New description"));

            ModifyTopicCommand command = new ModifyTopicCommand();

            CurationResult result = command.execute(suggestion, session, input);

            verify(session).modifyAndAcceptTopic(eq(suggestion), any());
            assertEquals(CurationResult.Outcome.MODIFIED, result.outcome());
            assertEquals("Modified and accepted", result.message());
        }

        @Test
        @DisplayName("Uses original values when user enters blanks")
        void usesOriginalValuesForBlanks() throws Exception {
            when(input.promptOptional(eq("  New name"), any()))
                    .thenReturn(InputResponse.value(""));
            when(input.promptOptional(eq("  New description"), any()))
                    .thenReturn(InputResponse.value(""));

            ModifyTopicCommand command = new ModifyTopicCommand();

            command.execute(suggestion, session, input);

            verify(session).modifyAndAcceptTopic(eq(suggestion), any());
        }

        @Test
        @DisplayName("Returns quit when user quits during name prompt")
        void returnsQuitOnNameQuit() throws Exception {
            when(input.promptOptional(eq("  New name"), any()))
                    .thenReturn(InputResponse.quit());

            ModifyTopicCommand command = new ModifyTopicCommand();

            CurationResult result = command.execute(suggestion, session, input);

            assertTrue(result.shouldQuit());
            verify(session, never()).modifyAndAcceptTopic(any(), any());
        }

        @Test
        @DisplayName("Returns MODIFY action")
        void returnsModifyAction() {
            ModifyTopicCommand command = new ModifyTopicCommand();
            assertEquals(CurationAction.MODIFY, command.getAction());
        }
    }

    @Nested
    @DisplayName("SkipRestTopicCommand")
    class SkipRestTopicCommandTest {

        @Test
        @DisplayName("Accepts high-relevance topics")
        void acceptsHighRelevance() throws Exception {
            TopicSuggestion highRelevance = new TopicSuggestion(
                    "High", "desc", "cat",
                    ContentType.TUTORIAL, ComplexityLevel.INTERMEDIATE,
                    1000, 0.9, "rationale", "", -1.0);

            SkipRestTopicCommand command = new SkipRestTopicCommand(List.of(highRelevance));

            command.execute(suggestion, session, input);

            // Current suggestion (0.8 relevance) and high relevance both accepted
            verify(session).acceptTopicSuggestion(suggestion);
            verify(session).acceptTopicSuggestion(highRelevance);
        }

        @Test
        @DisplayName("Defers low-relevance topics")
        void defersLowRelevance() throws Exception {
            TopicSuggestion lowRelevance = new TopicSuggestion(
                    "Low", "desc", "cat",
                    ContentType.TUTORIAL, ComplexityLevel.INTERMEDIATE,
                    1000, 0.5, "rationale", "", -1.0);

            SkipRestTopicCommand command = new SkipRestTopicCommand(List.of(lowRelevance));

            command.execute(suggestion, session, input);

            verify(session).acceptTopicSuggestion(suggestion); // 0.8
            verify(session).deferTopicSuggestion(lowRelevance); // 0.5
        }

        @Test
        @DisplayName("Returns skipRest result")
        void returnsSkipRestResult() throws Exception {
            SkipRestTopicCommand command = new SkipRestTopicCommand(List.of());

            CurationResult result = command.execute(suggestion, session, input);

            assertEquals(CurationResult.Outcome.SKIPPED, result.outcome());
            assertFalse(result.shouldContinue());
            assertFalse(result.shouldQuit());
        }

        @Test
        @DisplayName("Returns SKIP_REST action")
        void returnsSkipRestAction() {
            SkipRestTopicCommand command = new SkipRestTopicCommand(List.of());
            assertEquals(CurationAction.SKIP_REST, command.getAction());
        }
    }

    @Nested
    @DisplayName("TopicCurationCommandFactory")
    class TopicCurationCommandFactoryTest {

        private TopicCurationCommandFactory factory;

        @BeforeEach
        void setUp() {
            factory = new TopicCurationCommandFactory();
        }

        @Test
        @DisplayName("Returns AcceptTopicCommand for ACCEPT")
        void returnsAcceptCommand() {
            var command = factory.getCommand(CurationAction.ACCEPT, List.of());
            assertInstanceOf(AcceptTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns AcceptTopicCommand for DEFAULT")
        void returnsAcceptCommandForDefault() {
            var command = factory.getCommand(CurationAction.DEFAULT, List.of());
            assertInstanceOf(AcceptTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns RejectTopicCommand for REJECT")
        void returnsRejectCommand() {
            var command = factory.getCommand(CurationAction.REJECT, List.of());
            assertInstanceOf(RejectTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns DeferTopicCommand for DEFER")
        void returnsDeferCommand() {
            var command = factory.getCommand(CurationAction.DEFER, List.of());
            assertInstanceOf(DeferTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns ModifyTopicCommand for MODIFY")
        void returnsModifyCommand() {
            var command = factory.getCommand(CurationAction.MODIFY, List.of());
            assertInstanceOf(ModifyTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns SkipRestTopicCommand for SKIP_REST")
        void returnsSkipRestCommand() {
            var command = factory.getCommand(CurationAction.SKIP_REST, List.of());
            assertInstanceOf(SkipRestTopicCommand.class, command);
        }

        @Test
        @DisplayName("Returns null for QUIT")
        void returnsNullForQuit() {
            var command = factory.getCommand(CurationAction.QUIT, List.of());
            assertNull(command);
        }

        @Test
        @DisplayName("Returns menu prompt")
        void returnsMenuPrompt() {
            String prompt = factory.getMenuPrompt();
            assertTrue(prompt.contains("[A]ccept"));
            assertTrue(prompt.contains("[R]eject"));
            assertTrue(prompt.contains("[D]efer"));
            assertTrue(prompt.contains("[M]odify"));
            assertTrue(prompt.contains("[S]kip rest"));
            assertTrue(prompt.contains("[Q]uit"));
        }
    }
}
