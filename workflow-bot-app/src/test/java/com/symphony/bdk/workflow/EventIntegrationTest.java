package com.symphony.bdk.workflow;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static com.symphony.bdk.workflow.custom.assertion.WorkflowAssert.content;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.message.model.Message;
import com.symphony.bdk.gen.api.model.UserV2;
import com.symphony.bdk.gen.api.model.V4ConnectionAccepted;
import com.symphony.bdk.gen.api.model.V4ConnectionRequested;
import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4InstantMessageCreated;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4MessageSuppressed;
import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomMemberDemotedFromOwner;
import com.symphony.bdk.gen.api.model.V4RoomMemberPromotedToOwner;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4SharedPost;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4User;
import com.symphony.bdk.gen.api.model.V4UserJoinedRoom;
import com.symphony.bdk.gen.api.model.V4UserLeftRoom;
import com.symphony.bdk.gen.api.model.V4UserRequestedToJoinRoom;
import com.symphony.bdk.spring.events.RealTimeEvent;
import com.symphony.bdk.workflow.engine.ExecutionParameters;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.exception.InvalidActivityException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

class EventIntegrationTest extends IntegrationTest {

  @Test
  void onMessageReceived_streamIdFromEvent() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-message-received.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("123", "/execute"));

    verify(messageService, timeout(5000)).send(eq("123"), content("/execute"));
  }

  @Test
  void onMessageReceived_botMention() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-message-received-bot-mention.swadl.yaml"));
    UserV2 bot = new UserV2();
    bot.setDisplayName("myBot");
    when(sessionService.getSession()).thenReturn(bot);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("123", "@myBot /execute"));

    verify(messageService, timeout(5000)).send(eq("123"), content("ok"));
  }

  @Test
  void onMessageReceived_botMention_notMentioned() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-message-received-bot-mention.swadl.yaml"));
    UserV2 bot = new UserV2();
    bot.setDisplayName("myBot");
    when(sessionService.getSession()).thenReturn(bot);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("123", "/execute"));

    // no process started if the bot is not mentioned
    assertThat(lastProcess(workflow)).isEmpty();
  }

  @Test
  void onMessageReceived_anyContent() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-message-received-any-content.swadl.yaml"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("123", "/anything"));

    verify(messageService, timeout(5000)).send(eq("123"), content("ok"));
  }

  static Stream<Arguments> swadls() {
    return Stream.of(
        Arguments.arguments("/event/timeout/send-message-timeout.swadl.yaml", "/continue"),
        Arguments.arguments("/event/timeout/send-message-timeout-one-of.swadl.yaml", "/continue1"),
        Arguments.arguments("/event/timeout/send-message-timeout-one-of.swadl.yaml", "/continue2")
    );
  }

  @ParameterizedTest
  @MethodSource("swadls")
  void onMessageReceived_timeout(String swadlFile, String messageToReceive)
      throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(swadlFile));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(new V4Message());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));

    sleepToTimeout(500);

    engine.onEvent(messageReceived(messageToReceive));

    verify(messageService, never()).send(anyString(), any(Message.class));
    assertThat(workflow).as("sendMessageIfNotTimeout activity should not be executed as it times out")
        .executedContains("startWorkflow")
        .notExecuted("sendMessageIfNotTimeout");
  }

  @Test
  void onActivityExpired_timeout() throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/timeout/activity-expired-with-timeout.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(new V4Message());

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));

    ArgumentCaptor<Message> argumentCaptor = ArgumentCaptor.forClass(Message.class);
    await().atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(messageService, times(1)).send(anyString(), argumentCaptor.capture()));

    assertThat(argumentCaptor.getValue().getContent()).as("expirationActivity has been executed")
        .isEqualTo("<messageML>Expired</messageML>");
    assertThat(workflow).as(
            "sendMessageIfNotTimeout activity times out, expirationActivity is executed on its expiration")
        .executed("startWorkflow", "message-received_/continue_timeout", "expirationActivity")
        .notExecuted("script", "sendMessageIfNotTimeout");
  }

  @Test
  void onMultipleActivitiesExpired_timeout() throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow =
        SwadlParser.fromYaml(
            getClass().getResourceAsStream("/event/timeout/on-multiple-activities-expiration-with-timeout.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(new V4Message().messageId("MSG_ID"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));

    sleepToTimeout(500);

    engine.onEvent(messageReceived("123", "/continue1", "MSG_ID"));

    sleepToTimeout(500);

    verify(messageService, times(2)).send(anyString(), any(Message.class));
    assertThat(workflow).as(
            "sendMessageIfNotTimeout activity times out, expirationActivity is executed on its expiration")
        .executed("startWorkflow", "sendMessageIfNotTimeoutFirst", "message-received_/continue2_timeout",
            "expirationActivity")
        .notExecuted("script", "sendMessageIfNotTimeoutSecond");
  }

  @Test
  void onActivityExpiredToNewBranch_timeout() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/event/timeout/activity-expired-leading-to-new-branch.swadl.yaml"));

    when(messageService.send("123", "Expired")).thenReturn(new V4Message());
    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/start"));

    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    await().atMost(2, TimeUnit.SECONDS)
        .untilAsserted(() -> verify(messageService, times(1)).send(anyString(), messageArgumentCaptor.capture()));

    assertThat(messageArgumentCaptor.getValue().getContent()).isEqualTo("<messageML>Expired</messageML>");
    assertThat(workflow).executed("firstActivity", "message-received_/continue_timeout", "expirationActivity",
            "scriptActivityToBeExecuted")
        .notExecuted("sendMessageWithTimeout", "scriptActivityNotToBeExecuted1", "scriptActivityNotToBeExecuted2");
  }

  @Test
  void firstActivity_timeout() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/event/first-activity-with-timeout.swadl.yaml"));

    assertThatExceptionOfType(InvalidActivityException.class)
        .isThrownBy(() -> engine.deploy(workflow))
        .satisfies(e -> assertThat(e.getMessage()).isEqualTo(
            "Invalid activity in the workflow invalid-workflow: Workflow's starting activity startingActivity must "
                + "not have timeout"));
  }

  @Test
  void onRoomCreated() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-created.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomCreatedEvent("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomUpdated() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-updated.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomUpdatedEvent("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomDeactivated() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-deactivated.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomDeactivatedEvent("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomReactivated() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-reactivated.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomReactivatedEvent("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onPostShared() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-post-shared.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(postShared("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onImCreated() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-im-created.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(imCreated("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onMessageSuppressed() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-message-suppressed.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(messageSuppressed("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomMemberPromotedToOwner() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-member-promoted-to-owner.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomMemberPromotedToOwner("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomMemberDemotedFromOwner() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-room-member-demoted-from-owner.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(roomMemberDemotedFromOwner("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserRequestedJoinRoom() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-user-requested-join-room.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(userRequestedJoinRoom("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserJoinedRoom() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-user-joined-room.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(userJoinedRoom("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserLeftRoom() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-user-left-room.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(userLeftRoom("123"));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onConnectionRequested() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-connection-requested.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(connectionRequested(123));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onConnectionAccepted() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(
        "/event/types/on-connection-accepted.swadl.yaml"));
    engine.deploy(workflow);

    engine.onEvent(connectionAccepted(123));

    assertThat(lastProcess()).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  static Stream<Arguments> swadlUnderTest_on() {
    return Stream.of(
        Arguments.arguments("/event/id/on/message-received-event-with-id-on.swadl.yaml",
            messageReceived("123", "/execute")),
        Arguments.arguments("/event/id/on/message-received-with-special-characters-event-with-id-on.swadl.yaml",
            messageReceived("123", "/execute stringArg @user #hashtag $cashtag")),
        Arguments.arguments("/event/id/on/message-suppressed-event-with-id-on.swadl.yaml", messageSuppressed()),
        Arguments.arguments("/event/id/on/im-created-event-with-id-on.swadl.yaml", imCreated()),
        Arguments.arguments("/event/id/on/room-created-event-with-id-on.swadl.yaml", roomCreated()),
        Arguments.arguments("/event/id/on/room-updated-event-with-id-on.swadl.yaml", roomUpdated()),
        Arguments.arguments("/event/id/on/room-deactivated-event-with-id-on.swadl.yaml", roomDeactivated()),
        Arguments.arguments("/event/id/on/room-reactivated-event-with-id-on.swadl.yaml", roomReactivated()),
        Arguments.arguments("/event/id/on/room-member-promoted-event-with-id-on.swadl.yaml", roomMemberPromoted()),
        Arguments.arguments("/event/id/on/room-owner-demoted-event-with-id-on.swadl.yaml", roomOwnerDemoted()),
        Arguments.arguments("/event/id/on/post-shared-event-with-id-on.swadl.yaml", postShared()),
        Arguments.arguments("/event/id/on/connection-accepted-event-with-id-on.swadl.yaml", connectionAccepted()),
        Arguments.arguments("/event/id/on/user-joined-event-with-id-on.swadl.yaml", userJoined()),
        Arguments.arguments("/event/id/on/user-left-event-with-id-on.swadl.yaml", userLeft()),
        Arguments.arguments("/event/id/on/user-requested-connection-event-with-id-on.swadl.yaml", connectionRequested())
    );
  }

  static Stream<Arguments> swadlUnderTest_oneOf() {
    return Stream.of(
        Arguments.arguments("/event/id/one-of/message-received-event-with-id-one-of.swadl.yaml",
            messageReceived("123", "/execute")),
        Arguments.arguments("/event/id/one-of/message-received-special-characters-event-with-id-one-of.swadl.yaml",
            messageReceived("123", "/execute stringArg @user #hashtag $cashtag")),
        Arguments.arguments("/event/id/one-of/message-suppressed-event-with-id-one-of.swadl.yaml", messageSuppressed()),
        Arguments.arguments("/event/id/one-of/im-created-event-with-id-one-of.swadl.yaml", imCreated()),
        Arguments.arguments("/event/id/one-of/room-created-event-with-id-one-of.swadl.yaml", roomCreated()),
        Arguments.arguments("/event/id/one-of/room-updated-event-with-id-one-of.swadl.yaml", roomUpdated()),
        Arguments.arguments("/event/id/one-of/room-deactivated-event-with-id-one-of.swadl.yaml", roomDeactivated()),
        Arguments.arguments("/event/id/one-of/room-reactivated-event-with-id-one-of.swadl.yaml", roomReactivated()),
        Arguments.arguments("/event/id/one-of/room-member-promoted-event-with-id-one-of.swadl.yaml",
            roomMemberPromoted()),
        Arguments.arguments("/event/id/one-of/room-owner-demoted-event-with-id-one-of.swadl.yaml", roomOwnerDemoted()),
        Arguments.arguments("/event/id/one-of/post-shared-event-with-id-one-of.swadl.yaml", postShared()),
        Arguments.arguments("/event/id/one-of/connection-accepted-event-with-id-one-of.swadl.yaml",
            connectionAccepted()),
        Arguments.arguments("/event/id/one-of/user-joined-event-with-id-one-of.swadl.yaml", userJoined()),
        Arguments.arguments("/event/id/one-of/user-left-event-with-id-one-of.swadl.yaml", userLeft()),
        Arguments.arguments("/event/id/one-of/user-requested-connection-event-with-id-one-of.swadl.yaml",
            connectionRequested())
    );
  }

  static Stream<Arguments> swadlUnderTest_allOf() {
    return Stream.of(
        Arguments.arguments("/event/id/all-of/message-suppressed-event-with-id-all-of.swadl.yaml", messageSuppressed()),
        Arguments.arguments("/event/id/all-of/message-received-event-with-id-all-of.swadl.yaml",
            messageReceived("123", "/execute")),
        Arguments.arguments("/event/id/all-of/message-received-with-special-characters-event-with-id-all-of.swadl.yaml",
            messageReceived("123", "/execute stringArg @user #hashtag $cashtag")),
        Arguments.arguments("/event/id/all-of/im-created-event-with-id-all-of.swadl.yaml", imCreated()),
        Arguments.arguments("/event/id/all-of/room-created-event-with-id-all-of.swadl.yaml", roomCreated()),
        Arguments.arguments("/event/id/all-of/room-updated-event-with-id-all-of.swadl.yaml", roomUpdated()),
        Arguments.arguments("/event/id/all-of/room-deactivated-event-with-id-all-of.swadl.yaml", roomDeactivated()),
        Arguments.arguments("/event/id/all-of/room-reactivated-event-with-id-all-of.swadl.yaml", roomReactivated()),
        Arguments.arguments("/event/id/all-of/room-member-promoted-event-with-id-all-of.swadl.yaml",
            roomMemberPromoted()),
        Arguments.arguments("/event/id/all-of/room-owner-demoted-event-with-id-all-of.swadl.yaml", roomOwnerDemoted()),
        Arguments.arguments("/event/id/all-of/post-shared-event-with-id-all-of.swadl.yaml", postShared()),
        Arguments.arguments("/event/id/all-of/connection-accepted-event-with-id-all-of.swadl.yaml",
            connectionAccepted()),
        Arguments.arguments("/event/id/all-of/user-joined-event-with-id-all-of.swadl.yaml", userJoined()),
        Arguments.arguments("/event/id/all-of/user-left-event-with-id-all-of.swadl.yaml", userLeft()),
        Arguments.arguments("/event/id/all-of/user-requested-connection-event-with-id-all-of.swadl.yaml",
            connectionRequested())
    );
  }

  @ParameterizedTest()
  @MethodSource("swadlUnderTest_on")
  void eventWithId_on(String workflowFile, RealTimeEvent event) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    engine.deploy(workflow);
    engine.onEvent(event);

    assertThat(workflow).executed("scriptActivity", "scriptAssertion");
  }

  @ParameterizedTest()
  @MethodSource("swadlUnderTest_oneOf")
  void eventWithId_oneOf(String workflowFile, RealTimeEvent event) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    engine.deploy(workflow);
    engine.onEvent(event);

    assertThat(workflow).executed("scriptActivity", "scriptAssertion");
  }

  @ParameterizedTest()
  @MethodSource("swadlUnderTest_allOf")
  void eventWithId_allOf(String workflowFile, RealTimeEvent event)
      throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    engine.deploy(workflow);

    // all-of cannot be in the starting activity, so we need to start the workflow with a dummy activity
    // start workflow
    engine.onEvent(messageReceived("/start"));

    // Wait for the activity to finish
    Thread.sleep(2000);

    // firsts event of allOf
    engine.onEvent(messageReceived("/alwaysCalled"));

    // Wait for the event to finish
    Thread.sleep(2000);

    // second event of allOf
    engine.onEvent(event);

    assertThat(workflow).executedContains("scriptActivity", "scriptAssertion");
  }

  @ParameterizedTest
  @CsvSource({"/event/id/on/form-replied-event-with-id-on.swadl.yaml",
      "/event/id/one-of/form-replied-event-with-id-one-of.swadl.yaml"})
  void formRepliedEventWithId(String workflowFile) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    engine.deploy(workflow);
    engine.onEvent(form("", "formId", Collections.emptyMap()));

    assertThat(workflow).executed("formRepliedWithIdIn", "scriptActivity", "scriptAssertion");
  }

  @Test
  void formRepliedEventWithId_allOf() throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/event/id/all-of/form-replied-event-with-id-all-of.swadl.yaml"));

    when(messageService.send(anyString(), any(Message.class))).thenReturn(message("msgId"));

    engine.deploy(workflow);

    // all-of cannot be in the starting activity, so we need to start the workflow with a dummy activity
    // start workflow
    engine.onEvent(messageReceived("/start"));

    // Wait for the activity to finish
    Thread.sleep(2000);

    // firsts event of allOf
    engine.onEvent(messageReceived("/alwaysCalled"));

    // Wait for the event to finish
    Thread.sleep(2000);

    // second event of allOf
    await().atMost(5, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
      engine.onEvent(form("msgId", "formId", Collections.emptyMap()));
      return true;
    });

    assertThat(workflow).executedContains("formRepliedWithIdIn", "scriptActivity", "scriptAssertion");
  }

  @ParameterizedTest
  @CsvSource({"/event/id/on/request-received-event-with-id-on.swadl.yaml",
      "/event/id/one-of/request-received-event-with-id-one-of.swadl.yaml"})
  void requestReceivedEventWithId(String workflowFile) throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    engine.deploy(workflow);
    engine.execute(workflow.getId(), new ExecutionParameters(Collections.emptyMap(), "token"));

    assertThat(workflow).executed("scriptActivity", "scriptAssertion");
  }

  @Test
  void requestReceivedEventWithId_allOf() throws IOException, ProcessingException, InterruptedException {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/event/id/one-of/request-received-event-with-id-one-of.swadl.yaml"));

    engine.deploy(workflow);

    // all-of cannot be in the starting activity, so we need to start the workflow with a dummy activity
    // start workflow
    engine.onEvent(messageReceived("/start"));

    // Wait for the activity to finish
    Thread.sleep(2000);

    // firsts event of allOf
    engine.onEvent(messageReceived("/alwaysCalled"));

    // Wait for the event to finish
    Thread.sleep(2000);

    // second event of allOf
    engine.execute(workflow.getId(), new ExecutionParameters(Collections.emptyMap(), "token"));

    assertThat(workflow).executedContains("scriptActivity", "scriptAssertion");
  }

  private RealTimeEvent<V4RoomCreated> roomCreatedEvent(String roomId) {
    V4RoomCreated event = new V4RoomCreated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4RoomUpdated> roomUpdatedEvent(String roomId) {
    V4RoomUpdated event = new V4RoomUpdated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4RoomDeactivated> roomDeactivatedEvent(String roomId) {
    V4RoomDeactivated event = new V4RoomDeactivated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4RoomReactivated> roomReactivatedEvent(String roomId) {
    V4RoomReactivated event = new V4RoomReactivated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4SharedPost> postShared(String messageId) {
    V4SharedPost event = new V4SharedPost();
    V4Message message = new V4Message();
    event.setSharedMessage(message);
    message.setMessageId(messageId);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4InstantMessageCreated> imCreated(String roomId) {
    V4InstantMessageCreated event = new V4InstantMessageCreated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4MessageSuppressed> messageSuppressed(String messageId) {
    V4MessageSuppressed event = new V4MessageSuppressed();
    event.setMessageId(messageId);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4RoomMemberPromotedToOwner> roomMemberPromotedToOwner(String roomId) {
    V4RoomMemberPromotedToOwner event = new V4RoomMemberPromotedToOwner();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4RoomMemberDemotedFromOwner> roomMemberDemotedFromOwner(String roomId) {
    V4RoomMemberDemotedFromOwner event = new V4RoomMemberDemotedFromOwner();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4UserRequestedToJoinRoom> userRequestedJoinRoom(String roomId) {
    V4UserRequestedToJoinRoom event = new V4UserRequestedToJoinRoom();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4UserLeftRoom> userLeftRoom(String roomId) {
    V4UserLeftRoom event = new V4UserLeftRoom();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4UserJoinedRoom> userJoinedRoom(String roomId) {
    V4UserJoinedRoom event = new V4UserJoinedRoom();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4ConnectionRequested> connectionRequested(long userId) {
    V4ConnectionRequested event = new V4ConnectionRequested();
    V4User user = new V4User();
    user.setUserId(userId);
    event.setToUser(user);
    return new RealTimeEvent<>(initiator(), event);
  }

  private RealTimeEvent<V4ConnectionAccepted> connectionAccepted(long userId) {
    V4ConnectionAccepted event = new V4ConnectionAccepted();
    V4User user = new V4User();
    user.setUserId(userId);
    event.setFromUser(user);
    return new RealTimeEvent<>(initiator(), event);
  }

  private V4Initiator initiator() {
    V4Initiator initiator = new V4Initiator();
    V4User user = new V4User();
    user.setUserId(123L);
    initiator.setUser(user);
    return initiator;
  }
}
