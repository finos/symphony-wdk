package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.bdk.core.auth.AuthSession;
import com.symphony.bdk.core.service.message.MessageService;
import com.symphony.bdk.core.service.stream.StreamService;
import com.symphony.bdk.gen.api.model.RoomSystemInfo;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.engine.WorkflowEngine;
import com.symphony.bdk.workflow.lang.WorkflowBuilder;
import com.symphony.bdk.workflow.lang.exception.YamlNotValidException;
import com.symphony.bdk.workflow.lang.swadl.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;

@SpringBootTest
class IntegrationTest {

  @Autowired
  WorkflowEngine engine;

  // Mock the BDK
  @MockBean
  AuthSession botSession;
  @MockBean
  StreamService streamService;
  @MockBean
  MessageService messageService;

  @Test
  @DisplayName("send-message can send a message to the stream id defined")
  void sendMessageOnMessage() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/send-message-on-message.yaml"));
    engine.execute(workflow);

    engine.messageReceived("123", "/message");

    verify(messageService, timeout(5000)).send(anyString(), anyString());
  }

  @Test
  @DisplayName("create-room can create a new MIM with only user ids when only uids field is defined")
  void createRoomWithUids() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/create-mim-with-uids.yaml"));
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);
    final Stream stream = new Stream();
    stream.setId("0000");
    when(streamService.create(uids)).thenReturn(stream);

    engine.execute(workflow);
    engine.messageReceived("123", "/create-mim");
    verify(streamService, timeout(5000)).create(uids);
  }

  @Test
  @DisplayName("create-room can create a new room with details such as name and description when no uids field defined")
  void createRoomWithDetails() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/create-room-with-details.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = true;

    final V3RoomDetail v3RoomDetail = this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.execute(workflow);
    engine.messageReceived("123", "/create-room");

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes = this.buildRoomAttributes(roomName, roomDescription, isRoomPublic);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test
  @DisplayName("create-room can create a new room with details and members when all fields are defined")
  void createRoomWithDetailsAndMembers() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/create-room-with-details-members.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = false;
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);

    final V3RoomDetail v3RoomDetail = this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.execute(workflow);
    engine.messageReceived("123", "/create-room-members");

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    uids.forEach(uid -> verify(streamService, times(1)).addMemberToRoom(eq(uid), anyString()));

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes = this.buildRoomAttributes(roomName, roomDescription, isRoomPublic);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test()
  @DisplayName("create-room should have uids or name fields")
  void createRoomInvalidWorkflow() throws Exception {
    assertThatThrownBy(() -> WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/create-room-invalid-workflow.yaml"))).isInstanceOf(
        YamlNotValidException.class);
  }

  private void assertRoomAttributes(V3RoomAttributes expected, V3RoomAttributes actual) {
    assertThat(expected.getName()).isEqualTo(actual.getName());
    assertThat(expected.getDescription()).isEqualTo(actual.getDescription());
    assertThat(expected.getPublic()).isEqualTo(actual.getPublic());
  }

  private V3RoomDetail buildRoomDetail(String id, String name, String description, boolean isPublic) {
    V3RoomDetail v3RoomDetail = new V3RoomDetail();
    RoomSystemInfo roomSystemInfo = new RoomSystemInfo();
    roomSystemInfo.setId(id);
    V3RoomAttributes v3RoomAttributes = new V3RoomAttributes();
    v3RoomAttributes.name(name).description(description)._public(isPublic);
    v3RoomDetail.setRoomSystemInfo(roomSystemInfo);
    v3RoomDetail.setRoomAttributes(v3RoomAttributes);

    return v3RoomDetail;
  }

  private V3RoomAttributes buildRoomAttributes(String name, String description, boolean isPublic) {
    V3RoomAttributes expectedRoomAttributes = new V3RoomAttributes();
    return expectedRoomAttributes.name(name).description(description)._public(isPublic);
  }
}
