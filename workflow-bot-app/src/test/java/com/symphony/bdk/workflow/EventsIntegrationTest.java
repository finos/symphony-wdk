package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Optional;

class EventsIntegrationTest extends IntegrationTest {

  @Test
  void onMessageReceived_streamIdFromEvent() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-message-received.swadl.yaml"));
    when(messageService.send(anyString(), anyString())).thenReturn(message("msgId"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("123", "/execute"));

    verify(messageService, timeout(5000)).send("123", "/execute");
  }

  @Test
  void onRoomCreated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-created.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomCreatedEvent("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomUpdated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-updated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomUpdatedEvent("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomDeactivated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-deactivated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomDeactivatedEvent("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomReactivated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-reactivated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomReactivatedEvent("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onPostShared() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-post-shared.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(postShared("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onImCreated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-im-created.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(imCreated("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onMessageSuppressed() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-message-suppressed.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(messageSuppressed("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomMemberPromotedToOwner() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-member-promoted-to-owner.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomMemberPromotedToOwner("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onRoomMemberDemotedFromOwner() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-member-demoted-from-owner.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomMemberDemotedFromOwner("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserRequestedJoinRoom() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-user-requested-join-room.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(userRequestedJoinRoom("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserJoinedRoom() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-user-joined-room.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(userJoinedRoom("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onUserLeftRoom() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-user-left-room.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(userLeftRoom("123"));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onConnectionRequested() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-connection-requested.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(connectionRequested(123));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
  }

  @Test
  void onConnectionAccepted() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-connection-accepted.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(connectionAccepted(123));

    assertThat(process).hasValueSatisfying(
        processId -> await().atMost(5, SECONDS).until(() -> processIsCompleted(processId)));
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
    initiator.setUser(new V4User());
    return initiator;
  }
}
