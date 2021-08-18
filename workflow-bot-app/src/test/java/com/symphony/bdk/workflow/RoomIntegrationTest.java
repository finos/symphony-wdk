package com.symphony.bdk.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.symphony.bdk.core.service.pagination.model.PaginationAttribute;
import com.symphony.bdk.gen.api.model.RoomSystemInfo;
import com.symphony.bdk.gen.api.model.Stream;
import com.symphony.bdk.gen.api.model.V2RoomSearchCriteria;
import com.symphony.bdk.gen.api.model.V3RoomAttributes;
import com.symphony.bdk.gen.api.model.V3RoomDetail;
import com.symphony.bdk.gen.api.model.V3RoomSearchResults;
import com.symphony.bdk.workflow.swadl.WorkflowBuilder;
import com.symphony.bdk.workflow.swadl.exception.SwadlNotValidException;
import com.symphony.bdk.workflow.swadl.v1.Workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class RoomIntegrationTest extends IntegrationTest {

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

  @Test
  void updateRoom() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/update-room.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-room"));

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
  void updateRoom_activate() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/update-room-activate.swadl.yaml"));

    V3RoomDetail roomDetail = new V3RoomDetail();
    RoomSystemInfo info = new RoomSystemInfo();
    info.setId("abc");
    roomDetail.setRoomSystemInfo(info);
    when(streamService.getRoomInfo("abc")).thenReturn(roomDetail);

    engine.execute(workflow);
    engine.onEvent(messageReceived("/update-room"));

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

  @Test
  void getRoom() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/get-room.swadl.yaml"));
    when(streamService.getRoomInfo("abc")).thenReturn(new V3RoomDetail());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-room"));

    verify(streamService, timeout(5000)).getRoomInfo("abc");
    assertExecuted(workflow);
  }

  @Test
  void getRoomMembers() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/get-room-members.swadl.yaml"));
    when(streamService.listRoomMembers("abc")).thenReturn(Collections.emptyList());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-room-members"));

    verify(streamService, timeout(5000)).listRoomMembers("abc");
    assertExecuted(workflow);
  }

  @SuppressWarnings("ConstantConditions") // for null pagination attribute with refEq
  @Test
  void getRooms() throws Exception {
    final Workflow workflow = WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/get-rooms.swadl.yaml"));

    V2RoomSearchCriteria query = new V2RoomSearchCriteria()
        .query("test")
        .labels(List.of("test", "test1"))
        .active(true)
        .sortOrder(V2RoomSearchCriteria.SortOrderEnum.BASIC);
    when(streamService.searchRooms(refEq(query))).thenReturn(new V3RoomSearchResults());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-rooms"));

    verify(streamService, timeout(5000)).searchRooms(refEq(query));
    assertExecuted(workflow);
  }

  @SuppressWarnings("ConstantConditions") // for null pagination attribute with refEq
  @Test
  void getRoomsPagination() throws Exception {
    final Workflow workflow =
        WorkflowBuilder.fromYaml(getClass().getResourceAsStream("/room/get-rooms-pagination.swadl.yaml"));

    V2RoomSearchCriteria query = new V2RoomSearchCriteria()
        .query("test")
        .labels(List.of("test", "test1"))
        .active(true);
    when(streamService.searchRooms(refEq(query), refEq(new PaginationAttribute(10, 10))))
        .thenReturn(new V3RoomSearchResults());

    engine.execute(workflow);
    engine.onEvent(messageReceived("/get-rooms-pagination"));

    verify(streamService, timeout(5000)).searchRooms(refEq(query), refEq(new PaginationAttribute(10, 10)));
    assertExecuted(workflow);
  }
}
