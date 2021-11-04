package com.symphony.bdk.workflow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import java.io.IOException;

public class PinUnpinMessageIntegrationTest extends IntegrationTest {
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
