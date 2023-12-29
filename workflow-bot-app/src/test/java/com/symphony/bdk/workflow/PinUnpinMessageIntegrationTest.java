package com.symphony.bdk.workflow;

import com.symphony.bdk.gen.api.model.V1IMAttributes;
import com.symphony.bdk.gen.api.model.V2StreamAttributes;
import com.symphony.bdk.gen.api.model.V2StreamType;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V4Message;
import com.symphony.bdk.gen.api.model.V4Stream;
import com.symphony.bdk.workflow.custom.assertion.Assertions;
import com.symphony.bdk.workflow.swadl.SwadlParser;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static com.symphony.bdk.workflow.custom.assertion.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PinUnpinMessageIntegrationTest extends IntegrationTest {
  private static final String MSG_ID = "MSG_ID";
  private static final String STREAM_ID = "STREAM_ID";
  private static final V4Message message = message(MSG_ID);

  @Test
  void pinMessageInIm() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/pin-message.swadl.yaml"));

    V4Stream stream = new V4Stream();
    stream.setStreamType("IM");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, times(1)).updateInstantMessage(eq(STREAM_ID), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }

  @Test
  void pinMessageInRoom() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/pin-message.swadl.yaml"));

    V4Stream stream = new V4Stream();
    stream.setStreamType("ROOM");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, times(1)).updateRoom(eq(STREAM_ID), any(V3RoomAttributes.class));
  }

  @Test
  void pinMessageInImOboNotSupported() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/message/obo/pin-message-im-obo-not-supported.swadl.yaml"));

    V4Stream stream = new V4Stream();
    stream.setStreamType("IM");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message-im-obo-not-supported"));

    assertThat(workflow).executed("pinMessageImOboNotSupported").notExecuted("scriptActivityNotToBeExecuted");
  }

  @ParameterizedTest
  @CsvSource({"/message/obo/pin-message-obo-valid-username.swadl.yaml, /pin-message-obo-valid-username",
      "/message/obo/pin-message-obo-valid-userid.swadl.yaml, /pin-message-obo-valid-userid"})
  void pinMessageInRoomObo(String workflowFile, String command) throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    V4Stream stream = new V4Stream();
    stream.setStreamType("ROOM");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    Assertions.assertThat(workflow).isExecuted();
    verify(oboStreamService, times(1)).updateRoom(eq(STREAM_ID), any(V3RoomAttributes.class));
  }

  @Test
  void pinMessageInRoomOboUnauthorized() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/obo/pin-message-obo-unauthorized.swadl.yaml"));

    V4Stream stream = new V4Stream();
    stream.setStreamType("ROOM");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);

    when(bdkGateway.obo(any(String.class))).thenThrow(new RuntimeException("Unauthorized user"));
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message-obo-unauthorized"));

    assertThat(workflow).executed("pinMessageOboUnauthorized")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void unpinMessageInIm() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/unpin-message.swadl.yaml"));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("IM");
    streamAttributes.setStreamType(streamType);
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, times(1)).updateInstantMessage(eq(STREAM_ID), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }

  @Test
  void unpinMessageInRoom() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/unpin-message.swadl.yaml"));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("ROOM");
    streamAttributes.setStreamType(streamType);
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, times(1)).updateRoom(eq(STREAM_ID), any(V3RoomAttributes.class));
  }

  @ParameterizedTest
  @CsvSource({"/message/obo/unpin-message-obo-valid-username.swadl.yaml, /unpin-message-obo-valid-username",
      "/message/obo/unpin-message-obo-valid-userid.swadl.yaml, /unpin-message-obo-valid-userid"})
  void unpinMessageInRoomObo(String workflowFile, String command) throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream(workflowFile));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("ROOM");
    streamAttributes.setStreamType(streamType);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived(command));

    Assertions.assertThat(workflow).isExecuted();
    verify(oboStreamService, times(1)).updateRoom(eq(STREAM_ID), any(V3RoomAttributes.class));
  }

  @Test
  void unpinMessageInRoomOboUnauthorized() throws Exception {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/obo/unpin-message-obo-unauthorized.swadl.yaml"));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("ROOM");
    streamAttributes.setStreamType(streamType);

    when(bdkGateway.obo(any(String.class))).thenThrow(new RuntimeException("Unauthorized user"));
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message-obo-unauthorized"));

    assertThat(workflow).executed("unpinMessageOboUnauthorized")
        .notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void unpinMessageInImOboNotSupported() throws IOException, ProcessingException {
    final Workflow workflow = SwadlParser.fromYaml(
        getClass().getResourceAsStream("/message/obo/unpin-message-im-obo-not-supported.swadl.yaml"));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("IM");
    streamAttributes.setStreamType(streamType);

    when(bdkGateway.obo(any(String.class))).thenReturn(botSession);
    when(bdkGateway.obo(any(Long.class))).thenReturn(botSession);
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message-im-obo-not-supported"));

    assertThat(workflow).executed("unpinMessageImOboNotSupported").notExecuted("scriptActivityNotToBeExecuted");
  }

  @Test
  void pinMessageFailOnMessageId() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/pin-message.swadl.yaml"));
    when(messageService.getMessage(eq(MSG_ID))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }

  @Test
  void unpinMessageFailOnStreamId() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/unpin-message.swadl.yaml"));
    when(streamService.getStream(eq(STREAM_ID))).thenThrow(new RuntimeException("Failure"));

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }

  @Test
  void pinMessageOnInvalidStreamType() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/pin-message.swadl.yaml"));

    V4Stream stream = new V4Stream();
    stream.setStreamType("POST");
    stream.setStreamId(STREAM_ID);
    message.setStream(stream);
    when(messageService.getMessage(eq(MSG_ID))).thenReturn(message);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/pin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }

  @Test
  void unpinMessageOnInvalidStreamType() throws IOException, ProcessingException {
    final Workflow workflow =
        SwadlParser.fromYaml(getClass().getResourceAsStream("/message/unpin-message.swadl.yaml"));

    V2StreamAttributes streamAttributes = new V2StreamAttributes();
    V2StreamType streamType = new V2StreamType();
    streamType.setType("POST");
    streamAttributes.setStreamType(streamType);
    when(streamService.getStream(eq(STREAM_ID))).thenReturn(streamAttributes);

    engine.deploy(workflow);
    engine.onEvent(messageReceived("/unpin-message"));

    Assertions.assertThat(workflow).isExecuted();
    verify(streamService, never()).updateInstantMessage(any(String.class), any(V1IMAttributes.class));
    verify(streamService, never()).updateRoom(any(String.class), any(V3RoomAttributes.class));
  }
}
