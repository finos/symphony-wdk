package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.gen.api.model.RoomSystemInfo;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EditRoomIntegrationTest extends IntegrationTest {

  @Test
  void editRoom() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/edit-room.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/edit-room"));

    ArgumentCaptor<V3RoomAttributes> attributes = ArgumentCaptor.forClass(V3RoomAttributes.class);
    verify(streamService, timeout(5000)).updateRoom(eq("abc"), attributes.capture());

    assertThat(attributes.getValue()).satisfies(a -> {
      assertThat(a.getName()).isNull();
      assertThat(a.getDescription()).isNotEmpty();
      assertThat(a.getDiscoverable()).isTrue();
      assertThat(a.getCopyProtected()).isNull();
    });
  }

  @Test
  void editRoom_activate() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/edit-room-activate.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/edit-room"));

    verify(streamService, timeout(5000)).setRoomActive("abc", true);
  }

  @Test
  void addRoomMember() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/add-room-member.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/add-room-member"));

    verify(streamService, timeout(5000)).addMemberToRoom(123L, "abc");
    verify(streamService, timeout(5000)).addMemberToRoom(456L, "abc");
  }

  @Test
  void removeRoomMember() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/remove-room-member.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/remove-room-member"));

    verify(streamService, timeout(5000)).removeMemberFromRoom(123L, "abc");
    verify(streamService, timeout(5000)).removeMemberFromRoom(456L, "abc");
  }

  @Test
  void promoteRoomMember() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/promote-room-owner.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/promote-room-owner"));

    verify(streamService, timeout(5000)).promoteUserToRoomOwner(123L, "abc");
    verify(streamService, timeout(5000)).promoteUserToRoomOwner(456L, "abc");
  }

  @Test
  void demoteRoomMember() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/demote-room-owner.swadl.yaml"));

    engine.execute(workflow);
    engine.onEvent(messageReceived("/demote-room-owner"));

    verify(streamService, timeout(5000)).demoteUserToRoomParticipant(123L, "abc");
    verify(streamService, timeout(5000)).demoteUserToRoomParticipant(456L, "abc");
  }

}
