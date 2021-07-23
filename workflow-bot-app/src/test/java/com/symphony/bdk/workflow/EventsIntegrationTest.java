package com.symphony.bdk.workflow;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.V4Initiator;
import com.symphony.bdk.gen.api.model.V4RoomCreated;
import com.symphony.bdk.gen.api.model.V4RoomDeactivated;
import com.symphony.bdk.gen.api.model.V4RoomReactivated;
import com.symphony.bdk.gen.api.model.V4RoomUpdated;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.gen.api.model.V4User;
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

    assertThat(process).hasValueSatisfying(processId -> {
      await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
    });
  }

  @Test
  void onRoomUpdated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-updated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomUpdatedEvent("123"));

    assertThat(process).hasValueSatisfying(processId -> {
      await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
    });
  }

  @Test
  void onRoomDeactivated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-deactivated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomDeactivatedEvent("123"));

    assertThat(process).hasValueSatisfying(processId -> {
      await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
    });
  }

  @Test
  void onRoomReactivated() throws IOException, ProcessingException {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream(
        "/events/on-room-reactivated.swadl.yaml"));
    engine.execute(workflow);

    Optional<String> process = engine.onEvent(roomReactivatedEvent("123"));

    assertThat(process).hasValueSatisfying(processId -> {
      await().atMost(5, SECONDS).until(() -> processIsCompleted(processId));
    });
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
    V4Initiator initiator = initiator();
    V4RoomDeactivated event = new V4RoomDeactivated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator, event);
  }

  private RealTimeEvent<V4RoomReactivated> roomReactivatedEvent(String roomId) {
    V4Initiator initiator = initiator();
    V4RoomReactivated event = new V4RoomReactivated();
    V4Stream stream = new V4Stream();
    stream.setStreamId(roomId);
    event.setStream(stream);
    return new RealTimeEvent<>(initiator, event);
  }

  private V4Initiator initiator() {
    V4Initiator initiator = new V4Initiator();
    initiator.setUser(new V4User());
    return initiator;
  }
}
