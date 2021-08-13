package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.RoomSystemInfo;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.List;

class CreateRoomIntegrationTest extends IntegrationTest {

  @Test
  @DisplayName(
      "Given create-room activity with only user ids fields, when the message is received, "
          + "then an MIM is created with the given users")
  void createRoomWithUids() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/create-mim-with-uids.swadl.yaml"));
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);
    final Stream stream = new Stream();
    stream.setId("0000");
    when(streamService.create(uids)).thenReturn(stream);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/create-mim"));
    verify(streamService, timeout(5000)).create(uids);
  }

  @Test
  @DisplayName(
      "Given create-room activity with only required room details such as name and description, "
          + "when the message is received, then a room with the given details is created")
  void createRoomWithDetails() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/create-room-with-details.swadl.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = true;

    final V3RoomDetail v3RoomDetail = this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.execute(workflow);
    engine.onEvent(IntegrationTest.messageReceived("/create-room"));

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes = this.buildRoomAttributes(roomName, roomDescription, isRoomPublic);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test
  @DisplayName("Given create-room activity with uids and required room details, when the message is received,"
      + "then a room with given details and members is created")
  void createRoomWithDetailsAndMembers() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/create-room-with-details-members.swadl.yaml"));
    final String roomName = "The best room ever";
    final String roomDescription = "this is room description";
    final boolean isRoomPublic = false;
    final List<Long> uids = Arrays.asList(666L, 777L, 999L);

    final V3RoomDetail v3RoomDetail = this.buildRoomDetail("1234", roomName, roomDescription, isRoomPublic);
    when(streamService.create(any(V3RoomAttributes.class))).thenReturn(v3RoomDetail);

    engine.execute(workflow);
    engine.onEvent(IntegrationTest.messageReceived("/create-room-members"));

    ArgumentCaptor<V3RoomAttributes> argumentCaptor = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).create(argumentCaptor.capture());

    uids.forEach(uid -> verify(streamService, times(1)).addMemberToRoom(eq(uid), anyString()));

    final V3RoomAttributes captorValue = argumentCaptor.getValue();

    final V3RoomAttributes expectedRoomAttributes = this.buildRoomAttributes(roomName, roomDescription, isRoomPublic);
    assertRoomAttributes(expectedRoomAttributes, captorValue);
  }

  @Test()
  @DisplayName("Given an invalid create-room activity, when the message is received, then an error is thrown")
  void createRoomInvalidWorkflow() {
    assertThatThrownBy(() -> WorkflowBuilder.fromYaml(
        getClass().getResourceAsStream("/room/create-room-invalid-workflow.swadl.yaml"))).isInstanceOf(
        SwadlNotValidException.class);
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
